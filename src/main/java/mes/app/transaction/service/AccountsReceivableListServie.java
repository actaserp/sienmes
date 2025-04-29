package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccountsReceivableListServie {
  @Autowired
  SqlRunner sqlRunner;


  public List<Map<String, Object>> getTotalList(Timestamp start, Timestamp end, String company) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", company);

    String sql= """
        WITH LASTTbl as (
             select
             cltcd,
             max(yyyymm) as yyyymm 
             from tb_yearamt
             where yyyymm < '202501' and ioflag = '0' 
             group by cltcd
         )
         SELECT
             m.id AS cltcd,
             COALESCE(y.yearamt, 0) AS receivables,
             COALESCE(s.TOTALAMT, 0) AS sales,
             COALESCE(b.ACCIN, 0) AS "AmountDeposited",
             COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) AS balance
         FROM COMPANY M
         LEFT JOIN (
             SELECT y.cltcd, SUM(y.yearamt) AS yearamt  
             FROM tb_yearamt y
             JOIN LASTTbl h ON y.cltcd = h.cltcd AND y.yyyymm = h.yyyymm AND y.ioflag = '0'
             GROUP BY y.cltcd
         ) y ON m.id = y.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(totalamt) AS sales_amt
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
             GROUP BY cltcd
         ) jan_s ON m.id = jan_s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(TOTALAMT) AS TOTALAMT
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
             GROUP BY cltcd
         ) s ON m.id = s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(ACCIN) AS ACCIN  
             FROM tb_banktransit
             WHERE TRDATE BETWEEN :start AND :end
             GROUP BY cltcd
         ) b ON m.id = b.cltcd
         WHERE COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) <> 0
        """;
    if (company != null && !company.isEmpty()) {
      sql += " AND y.cltcd = :company ";
      paramMap.addValue("company", company);
    }

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("미수금 집계 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;

  }

  //미수금 현황 상세
  public List<Map<String, Object>> getDetailList(Timestamp start, Timestamp end, String company) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", company);

    String sql= """
        WITH LASTTbl as (
             select
             cltcd,
             max(yyyymm) as yyyymm 
             from tb_yearamt
             where yyyymm < '202501' and ioflag = '0' 
             group by cltcd
         )
         SELECT
             m.id AS cltcd,
             COALESCE(y.yearamt, 0) AS receivables,
             COALESCE(s.TOTALAMT, 0) AS sales,
             COALESCE(b.ACCIN, 0) AS "AmountDeposited",
             COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) AS balance
         FROM COMPANY M
         LEFT JOIN (
             SELECT y.cltcd, SUM(y.yearamt) AS yearamt  
             FROM tb_yearamt y
             JOIN LASTTbl h ON y.cltcd = h.cltcd AND y.yyyymm = h.yyyymm AND y.ioflag = '0'
             GROUP BY y.cltcd
         ) y ON m.id = y.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(totalamt) AS sales_amt
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
             GROUP BY cltcd
         ) jan_s ON m.id = jan_s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(TOTALAMT) AS TOTALAMT
             FROM tb_salesment
             WHERE misdate BETWEEN :start AND :end
             GROUP BY cltcd
         ) s ON m.id = s.cltcd
         LEFT JOIN (
             SELECT cltcd, SUM(ACCIN) AS ACCIN  
             FROM tb_banktransit
             WHERE TRDATE BETWEEN :start AND :end
             GROUP BY cltcd
         ) b ON m.id = b.cltcd
         WHERE COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCIN, 0) <> 0
        """;

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
    log.info("미수금 현황 상세 read SQL: {}", sql);
    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }


  /*
  * select * from tb_salesment; -- 매출 내역 관리
select * from tb_salesdetail; -- 매출 내역 관리 상세

select * from tb_invoicement; -- 매입 내역관리
select * from tb_invoicdetail;-- 매입 내역 상세
*
* 이거 사용해서 상세 내용을  만들어야하는지 질문 하기
*
  *
  * */
}
