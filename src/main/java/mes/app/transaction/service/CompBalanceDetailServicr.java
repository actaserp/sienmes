package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.Encryption.EncryptionUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CompBalanceDetailServicr {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(String start, String end, String company, String spjangcd) {

    if (company == null || company.trim().isEmpty()) {
      //log.warn("⚠️ 거래처 코드가 비어 있습니다. 빈 리스트를 반환합니다.");
      return Collections.emptyList(); // 빈 결과 반환
    }

    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("company", Integer.parseInt(company));
    paramMap.addValue("spjangcd", spjangcd);
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = LocalDate.parse(start, inputFormatter);
    LocalDate endDate = LocalDate.parse(end, inputFormatter);

    // SQL용 날짜 포맷
    String formattedStart = startDate.format(dbFormatter);
    String formattedEnd = endDate.format(dbFormatter);

    // 전월 기준 계산
    YearMonth prevMonth = YearMonth.from(startDate).minusMonths(1);
    String prevYm = prevMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    String baseDate = prevMonth.atDay(1).format(dbFormatter);

    // 파라미터 등록
    paramMap.addValue("start", formattedStart);
    paramMap.addValue("end", formattedEnd);
    paramMap.addValue("prevYm", prevYm);
    paramMap.addValue("baseDate", baseDate);


    String sql = """
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
        saletbl AS (
            SELECT s.cltcd, SUM(s.totalamt) AS totsale, s.spjangcd
            FROM tb_salesment s
            WHERE s.misdate BETWEEN :start AND :end
              AND s.cltcd = :company
              AND s.spjangcd = :spjangcd
            GROUP BY s.cltcd, s.spjangcd
        ),
        incomtbl AS (
            SELECT s.cltcd, SUM(s.accin) AS totaccout, s.spjangcd
            FROM tb_banktransit s
            WHERE s.trdate BETWEEN :start AND :end
              AND s.cltcd = :company
              AND s.spjangcd = :spjangcd
              and s.ioflag = '0'
            GROUP BY s.cltcd, s.spjangcd
        ),
        union_data_raw AS (
            -- 전잔액
            SELECT
                c.id,
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
            LEFT JOIN saletbl p ON c.id = p.cltcd AND c.spjangcd = p.spjangcd
            LEFT JOIN incomtbl q ON c.id = q.cltcd AND c.spjangcd = q.spjangcd
            WHERE c.id = :company AND c.spjangcd = :spjangcd
            UNION ALL
            -- 매출
            SELECT
                s.cltcd,
                c."Name" AS comp_name,
                TO_DATE(s.misdate, 'YYYYMMDD'),
                '매출',
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
                s.totalamt AS totalamt,
                NULL::text AS memo,
                s.remark1,
                1 AS remaksseq
            FROM tb_salesment s
            LEFT JOIN tb_salesdetail d ON s.misdate = d.misdate AND s.misnum = d.misnum AND s.spjangcd = d.spjangcd
            LEFT JOIN sys_code sc ON sc."Code" = s.misgubun
            JOIN company c ON c.id = s.cltcd AND c.spjangcd = s.spjangcd
            WHERE s.misdate BETWEEN :start AND :end
              AND s.spjangcd = :spjangcd
              AND s.cltcd = :company
            GROUP BY s.cltcd, c."Name", s.misdate, s.totalamt, s.misgubun, sc."Value", s.remark1
            UNION ALL
            -- 입금
            SELECT
                b.cltcd,
                c."Name" AS comp_name,
                TO_DATE(b.trdate, 'YYYYMMDD'),
                '입금액',
                NULL::numeric AS amount,
                NULL::text AS itemnm,
                NULL::text AS misgubun,
                sc."Value" AS iotype,
                b.banknm,
                b.accnum,
                b.eumnum,
                TO_CHAR(TO_DATE(NULLIF(b.eumtodt, ''), 'YYYYMMDD'), 'YYYY-MM-DD'),
                tt.tradenm,
                b.accin,
                NULL::numeric AS totalamt,
                b.memo,
                b.remark1,
                2 AS remaksseq
            FROM tb_banktransit b
            JOIN company c ON c.id = b.cltcd AND c.spjangcd = b.spjangcd
            LEFT JOIN sys_code sc ON sc."Code" = b.iotype
            LEFT JOIN tb_trade tt ON tt.trid = b.trid AND tt.spjangcd = b.spjangcd
             WHERE TO_DATE(b.trdate, 'YYYYMMDD') BETWEEN TO_DATE(:start, 'YYYYMMDD') AND TO_DATE(:end, 'YYYYMMDD') 
              AND b.cltcd = :company
              AND b.spjangcd = :spjangcd
              and b.ioflag = '0'
        ),
        union_data AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY id, date ORDER BY remaksseq) AS rn
            FROM union_data_raw
        )
        SELECT
            x.id AS cltid,
            x.comp_name,
            x.date,
            x.summary,
            x.amount,
            x.accin,
            x.totalamt,
           SUM(
            COALESCE(x.amount, 0) + COALESCE(x.totalamt, 0) - COALESCE(x.accin, 0)
        ) OVER (
            PARTITION BY x.id
            ORDER BY x.date, x.remaksseq
            ROWS UNBOUNDED PRECEDING
        ) AS balance,
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
        ORDER BY x.id, x.date, x.remaksseq
        """;
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("거래처별잔액명세서(입금) read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

}
