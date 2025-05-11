package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccountsPayableListService {
  @Autowired
  SqlRunner sqlRunner;

  // 미지급현황 리스트 조회
  public List<Map<String, Object>> getPayableList(String start, String end, Integer company, String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("start", start);
    dicParam.addValue("end", end);
    dicParam.addValue("company", company);
    dicParam.addValue("spjangcd", spjangcd);
    // 시작일을 LocalDate로 변환
    LocalDate startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    // 전월 계산
    String prevYm = startDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    dicParam.addValue("prevYm", prevYm);

    String sql = """
          WITH LASTTbl as (
                      select
                      cltcd,
                      max(yyyymm) as yyyymm 
                      from tb_yearamt
                      where yyyymm <  :prevYm and ioflag = '1'  and spjangcd =:spjangcd
                      group by cltcd
                  )
                  SELECT
                      m.id AS cltcd,
                      m."Name" as cltName,
                      COALESCE(y.yearamt, 0) AS receivables,
                      COALESCE(s.TOTALAMT, 0) AS sales,
                      COALESCE(b.accout, 0) AS "AmountDeposited",
                      COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.accout, 0) AS balance
                  FROM COMPANY M
                  LEFT JOIN (
                      SELECT y.cltcd, SUM(y.yearamt) AS yearamt  
                      FROM tb_yearamt y
                      JOIN LASTTbl h ON y.cltcd = h.cltcd AND y.yyyymm = h.yyyymm AND y.ioflag = '1'
                      GROUP BY y.cltcd
                  ) y ON m.id = y.cltcd
                  LEFT JOIN (
                      SELECT cltcd, SUM(TOTALAMT) AS TOTALAMT
                      FROM tb_invoicement
                      WHERE misdate BETWEEN :start AND :end and spjangcd =:spjangcd
                      GROUP BY cltcd
                  ) s ON m.id = s.cltcd
                  LEFT JOIN (
                      SELECT cltcd, SUM(accout) AS accout  
                      FROM tb_banktransit
                      WHERE TRDATE BETWEEN :start AND :end and spjangcd =:spjangcd
                      GROUP BY cltcd
                  ) b ON m.id = b.cltcd
                  WHERE COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.accout, 0) <> 0
        """;
    if (company != null) {
      sql += " AND m.id = :company";
    }
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
//    log.info("미지급 현황 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    return items;
  }

  // 미지급현황 상세 리스트 조회
  public List<Map<String, Object>> getPayableDetailList(String start, String end, Integer company, String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("start", start);
    dicParam.addValue("end", end);
    dicParam.addValue("company", company);
    dicParam.addValue("spjangcd", spjangcd);
    // 시작일을 LocalDate로 변환
    LocalDate startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    // 전월 계산
    String prevYm = startDate.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    dicParam.addValue("prevYm", prevYm);
    // 전월 1일 날짜 계산
    String baseDate = startDate.minusMonths(1).withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    dicParam.addValue("baseDate", baseDate);
    String sql = """
        WITH lasttbl  as (
         select cltcd,
         yearamt,
         max(yyyymm) as yyyymm 
         from tb_yearamt    
         where yyyymm < :prevYm and ioflag = '1' and cltcd = :company 
             group by cltcd, yearamt
          ),  saletbl  as (
             select s.cltcd,
             sum(s.totalamt) as totsale   
             from tb_salesment s   
             where s.misdate BETWEEN  :start AND :end and cltcd = :company   
             group by cltcd
          ), incomtbl  as (
             select s.cltcd,
             sum(s.accout) as totaccout   
             from tb_banktransit s   
             where s. trdate BETWEEN :start AND :end  and cltcd = :company  
             group by cltcd
          )
          SELECT
              x.id AS cltcd,
              x.todate,
              x.remark1,
              x.total,
              SUM(x.total) OVER (PARTITION BY x.id ORDER BY x.todate, x.remarkCnt ROWS UNBOUNDED PRECEDING) AS nowamt,
              a."Name"
          FROM (
              SELECT
                  y.id,
                  TO_DATE(:baseDate, 'YYYYMMDD') AS todate,
          '전잔액' AS remark1,
              SUM(h.yearamt) + SUM(P.totsale) - SUM(Q. totaccout) AS total,
              0 AS remarkCnt
          FROM company y    
          join lasttbl  h      on y.id = h.cltcd
          join saletbl   P      on y.id = P.cltcd  
          join  incomtbl   Q      on y.id = Q.cltcd
          GROUP BY y.id
          UNION ALL
          -- 매출액
          SELECT
              s.cltcd,
              TO_DATE(s.misdate, 'YYYYMMDD') AS todate,
          '매출' AS remark1,
              s.totalamt AS total,
              1 AS remarkCnt
          FROM tb_salesment s
          WHERE s.misdate BETWEEN :start AND :end
          and s.spjangcd =:spjangcd
          UNION ALL
          SELECT
              b.cltcd,
              TO_DATE(b.trdate, 'YYYYMMDD') AS todate,
          '입금액' AS remark1,
              b.accin AS total,
              2 AS remarkCnt
          FROM tb_banktransit b
          WHERE b.trdate BETWEEN :start AND :end
              and b.spjangcd =:spjangcd
          ) x
          LEFT JOIN company a ON x.id = a.id
          WHERE 1=1
        """;
      sql += " AND a.id = :company";
      sql +=" ORDER BY x.id, x.todate, x.remarkCnt";

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
//    log.info("미지금 현황 상세 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    return items;
  }

}
