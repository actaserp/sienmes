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
public class CompBalanceDetailServicr {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(Timestamp start, Timestamp end, String company) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", company);
    String sql = """
        WITH lasttbl  as (
             select cltcd, 
             yearamt,
             max(yyyymm) as yyyymm    
             from tb_yearamt     
             where yyyymm < '202501' and ioflag = '0' and cltcd = :company    
             group by cltcd, yearamt
          ),  saletbl  as (
             select s.cltcd, 
             sum(s.totalamt) as totsale    
             from tb_salesment s    -- 매출 내역 관리
             where s.misdate BETWEEN :start AND :end and cltcd = :company   
             group by cltcd 
          ), incomtbl  as (
             select s.cltcd, 
             sum(s.accout) as totaccout    
             from tb_banktransit s   -- 입출금 관리 
             where s. trdate BETWEEN :start AND :end and cltcd = :company    
             group by cltcd 
          )
        SELECT 
            x.id AS 거래처코드,
            x.일자,
            x.적요,
            x.금액,
            SUM(x.금액) OVER (PARTITION BY x.id ORDER BY x.일자, x.적요순서 ROWS UNBOUNDED PRECEDING) AS 잔액
        FROM (   
            SELECT 
                y.id,
                TO_DATE('20241201', 'YYYYMMDD') AS 일자,
                '전잔액' AS 적요,
                SUM(h.yearamt) + SUM(P.totsale) - SUM(Q. totaccout) AS 금액,
                0 AS 적요순서
            FROM company y     
            join lasttbl  h      on y.id = h.cltcd 
            join saletbl   P      on y.id = P.cltcd   
            join  incomtbl   Q      on y.id = Q.cltcd 
            GROUP BY y.id  
            UNION ALL
            -- 매출액
            SELECT 
                s.cltcd,
                TO_DATE(s.misdate, 'YYYYMMDD') AS 일자,
                '매출' AS 적요,
                s.totalamt AS 금액,
                1 AS 적요순서
            FROM tb_salesment s -- 매출 내역 관리
            WHERE s.misdate BETWEEN '20250101' AND '20250423'
            UNION ALL
            SELECT 
                b.cltcd,
                TO_DATE(b.trdate, 'YYYYMMDD') AS 일자,
                '입금액' AS 적요,
                b.accin AS 금액,
                2 AS 적요순서
            FROM tb_banktransit b --입출금 과ㄴ리
            WHERE b.trdate BETWEEN '20250101' AND '20250423'
        ) x
        ORDER BY x.id, x.일자, x.적요순서
        
        """;
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
    //log.info("거래처별잔액명세서(입금) read SQL: {}", sql);
    //log.info("SQL Parameters: {}", paramMap.getValues());
    return items;

  }
}
