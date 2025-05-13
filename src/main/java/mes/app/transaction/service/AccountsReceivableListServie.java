package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccountsReceivableListServie {
  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getTotalList(String start_date, String end_date, Integer company, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = LocalDate.parse(start_date, inputFormatter);
    LocalDate endDate = LocalDate.parse(end_date, inputFormatter);

    String formattedStart = startDate.format(dbFormatter);
    String formattedEnd = endDate.format(dbFormatter);

    YearMonth prevMonth = YearMonth.from(startDate).minusMonths(1);
    String prevYm = prevMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

    paramMap.addValue("prevYm", prevYm);
    paramMap.addValue("start", formattedStart);
    paramMap.addValue("end", formattedEnd);
    paramMap.addValue("company", company);
    paramMap.addValue("spjangcd", spjangcd);

    String sql= """
        WITH LASTTbl as (
             select
             cltcd,
             max(yyyymm) as yyyymm 
             from tb_yearamt
             where yyyymm < :prevYm and ioflag = '0' 
             and spjangcd = :spjangcd
             group by cltcd
         )
         SELECT
             m.id AS cltcd,
             m."Name" as clt_name,
             COALESCE(y.yearamt, 0) AS receivables,
             COALESCE(s.TOTALAMT, 0) AS sales,
             COALESCE(b.ACCIN, 0) AS "AmountDeposited",
             COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) AS balance
         FROM COMPANY M
         LEFT JOIN (
             SELECT y.cltcd, SUM(y.yearamt) AS yearamt  
             FROM tb_yearamt y
             JOIN LASTTbl h ON y.cltcd = h.cltcd AND y.yyyymm = h.yyyymm AND y.ioflag = '0'
              WHERE y.spjangcd = :spjangcd
             GROUP BY y.cltcd
         ) y ON m.id = y.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(totalamt) AS sales_amt
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
              AND spjangcd = :spjangcd
             GROUP BY cltcd
         ) jan_s ON m.id = jan_s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(TOTALAMT) AS TOTALAMT
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
              AND spjangcd = :spjangcd
             GROUP BY cltcd
         ) s ON m.id = s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(ACCIN) AS ACCIN  
             FROM tb_banktransit
             WHERE TRDATE BETWEEN :start AND :end
              AND spjangcd = :spjangcd
              AND ioflag ='0'
             GROUP BY cltcd
         ) b ON m.id = b.cltcd
         WHERE COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) <> 0
        """;
    if (company != null) {
      sql += " AND m.id = :company ";
      paramMap.addValue("company", company);
    }

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("미수금 집계 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;

  }

  //미수금 현황 상세
  public List<Map<String, Object>> getDetailList(String start_date, String end_date, String company, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = LocalDate.parse(start_date, inputFormatter);
    LocalDate endDate = LocalDate.parse(end_date, inputFormatter);

    // SQL용 날짜 포맷
    String formattedStart = startDate.format(dbFormatter);
    String formattedEnd = endDate.format(dbFormatter);

    // 전월 기준 계산
    YearMonth prevMonth = YearMonth.from(startDate).minusMonths(1);
    String prevYm = prevMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    String baseDate = prevMonth.atDay(1).format(dbFormatter);

    paramMap.addValue("prevYm", prevYm);
    paramMap.addValue("baseDate", baseDate);
    paramMap.addValue("start", formattedStart);
    paramMap.addValue("end", formattedEnd);
    paramMap.addValue("company", Integer.valueOf(company));
    paramMap.addValue("spjangcd", spjangcd);

    String sql= """
        WITH lastym AS (
            SELECT cltcd, MAX(yyyymm) AS yyyymm
            FROM tb_yearamt
            WHERE yyyymm < :prevYm
              AND ioflag = '0'
              AND cltcd = :company
              AND spjangcd = :spjangcd
            GROUP BY cltcd
        ),
        lasttbl AS (
            SELECT y.cltcd, y.yearamt, y.yyyymm, y.spjangcd
            FROM tb_yearamt y
            JOIN lastym m ON y.cltcd = m.cltcd AND y.yyyymm = m.yyyymm
            WHERE y.ioflag = '0'
              AND y.spjangcd = :spjangcd
        ),
        union_data_raw AS (
            -- 전잔액
            SELECT
                c.id AS cltcd,
                c."Name" AS comp_name,
                TO_DATE(:baseDate, 'YYYYMMDD') AS date,
                '전잔액' AS summary,
                COALESCE(h.yearamt, 0) AS amount,
                NULL::text AS itemnm,
                NULL::text AS misgubun,
                NULL::text AS iotype,
                NULL::text AS banknm,
                NULL::text AS accnum,
                NULL::text AS eumnum,
                NULL::text AS eumtodt,
                NULL::text AS tradenm,
                NULL::numeric AS accin,
                NULL::numeric AS totalamt,
                NULL::text AS memo,
                NULL::text AS remark1,
                0 AS remaksseq
            FROM company c
            LEFT JOIN lasttbl h ON c.id = h.cltcd AND c.spjangcd = h.spjangcd
            WHERE c.id = :company AND c.spjangcd = :spjangcd
            UNION ALL
            -- 매출
            SELECT
                s.cltcd,
                c."Name" AS comp_name,
                TO_DATE(s.misdate, 'YYYYMMDD') AS date,
                '매출' AS summary,
                NULL::numeric AS amount,
                CONCAT(
                    MAX(CASE WHEN d.misseq::int = 1 THEN d.itemnm END),
                    CASE WHEN COUNT(DISTINCT d.itemnm) > 1 THEN ' 외 ' || (COUNT(DISTINCT d.itemnm) - 1) || '건' ELSE '' END
                ) AS itemnm,
                sc."Value" AS misgubun,
                NULL::text AS iotype,
                NULL::text AS banknm,
                NULL::text AS accnum,
                NULL::text AS eumnum,
                NULL::text AS eumtodt,
                NULL::text AS tradenm,
                NULL::numeric AS accin,
                s.totalamt,
                NULL::text AS memo,
                s.remark1,
                2 AS remaksseq
            FROM tb_salesment s
            LEFT JOIN tb_salesdetail d ON s.misdate = d.misdate AND s.misnum = d.misnum AND s.spjangcd = d.spjangcd
            LEFT JOIN sys_code sc ON sc."Code" = s.misgubun
            JOIN company c ON c.id = s.cltcd AND c.spjangcd = s.spjangcd
            WHERE s.misdate BETWEEN :start AND :end
              AND s.cltcd = :company
              AND s.spjangcd = :spjangcd
            GROUP BY s.cltcd, c."Name", s.misdate, s.totalamt, s.misgubun, sc."Value", s.remark1
            UNION ALL
            -- 입금
            SELECT
                b.cltcd,
                c."Name" AS comp_name,
                TO_DATE(b.trdate, 'YYYYMMDD') AS date,
                '입금' AS summary,
                NULL::numeric AS amount,
                NULL::text AS itemnm,
                NULL::text AS misgubun,
                sc."Value" AS iotype,
                b.banknm,
                b.accnum,
                b.eumnum,
                TO_CHAR(TO_DATE(NULLIF(b.eumtodt, ''), 'YYYYMMDD'), 'YYYY-MM-DD') AS eumtodt,
                tt.tradenm,
                b.accin,
                NULL::numeric AS totalamt,
                b.memo,
                b.remark1,
                1 AS remaksseq
            FROM tb_banktransit b
            JOIN company c ON c.id = b.cltcd AND c.spjangcd = b.spjangcd
            LEFT JOIN sys_code sc ON sc."Code" = b.iotype
            LEFT JOIN tb_trade tt ON tt.trid = b.trid AND tt.spjangcd = b.spjangcd
            WHERE TO_DATE(b.trdate, 'YYYYMMDD') BETWEEN TO_DATE(:start, 'YYYYMMDD') AND TO_DATE(:end, 'YYYYMMDD') 
              AND b.cltcd = :company
              AND b.spjangcd = :spjangcd
              AND b.ioflag = '0'
        ),
        union_data AS (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY cltcd, date ORDER BY remaksseq) AS rn
            FROM union_data_raw
        )
        SELECT
            x.cltcd,
            x.comp_name,
            x.date,
            x.summary,
            COALESCE(x.amount, x.totalamt, x.accin) AS total_amount,
            SUM(
              COALESCE(x.amount, 0) + COALESCE(x.totalamt, 0) - COALESCE(x.accin, 0)
            ) OVER (
              PARTITION BY x.cltcd
              ORDER BY x.date, x.remaksseq, x.itemnm
              ROWS UNBOUNDED PRECEDING
            ) AS balance,        
            x.accin,
            x.totalamt,
            x.itemnm,
            x.misgubun,
            x.iotype,
            x.banknm,
            x.accnum,
            x.eumnum,
            x.eumtodt,
            x.tradenm,
            x.memo,
            x.remark1
        FROM union_data x
        ORDER BY x.cltcd, x.date, x.remaksseq
        """;

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("미수금 현황 상세 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }
}
