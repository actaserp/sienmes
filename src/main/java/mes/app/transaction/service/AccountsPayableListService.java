package mes.app.transaction.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AccountsPayableListService {
    @Autowired
    SqlRunner sqlRunner;

    // 미지급현황 리스트 조회
    public List<Map<String, Object>> getPayableList(String date_from,
                                                    String date_to,
                                                    Integer companyCode) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);

        String sql = """
                WITH LASTTbl as (
                   select cltcd, 
                   max(yyyymm) as yyyymm    
                   from tb_yearamt     
                   where yyyymm < ‘202501’ and ioflag = ‘0’ and cltcd = ‘1000’   
                   group by cltcd
                ),  SaleTbl as (
                   select cltcd, 
                   sum(totalamt) as totsale    
                   from tb_salesment    
                   where s.misdate BETWEEN '20250101' AND '20250131‘  and cltcd = ‘1000’    
                   group by cltcd   
                   union
                ),
                  InComTbl as (
                   select cltcd, 
                   sum(accin) as totaccin    
                   from tb_banktransit    
                   where s. trdate BETWEEN '20250101' AND '20250131‘  and cltcd = ‘1000’    
                   group by cltcd   
                   union
                )SELECT
                    x.cltcd AS 거래처코드,
                    x.일자,
                    x.적요,
                    x.금액,
                    SUM(x.금액) OVER (PARTITION BY x.cltcd ORDER BY x.일자, x.적요순서 ROWS UNBOUNDED PRECEDING) AS 잔액
                FROM (
                   
                    SELECT
                        y.cltcd,
                        CAST('202412' AS DATE) AS 일자,
                        '전잔액' AS 적요,
                        SUM(h.yearamt) + SUM(P.totsale) - SUM(Q. totaccin) AS 금액,
                        0 AS 적요순서
                    FROM tb_거래처테이블 y     
                    join LASTTbl h      
                    on y.cltcd = h.cltcd and y.yyyymm = h.yyyymm 
                    join SaleTbl  P      
                    on y.cltcd = P.cltcd  
                    join  InComTbl  Q      
                    on y.cltcd = Q.cltcd 
                    UNION ALL

                    -- 매출액
                    SELECT
                        s.cltcd,
                        s.misdate AS 일자,
                        '매출' AS 적요,
                        s.totalamt AS 금액,
                        1 AS 적요순서
                    FROM tb_salesment s
                    WHERE s.misdate BETWEEN '20250101' AND '20250423'

                    UNION ALL
                
                    SELECT
                        b.cltcd,
                        b.trdate AS 일자,
                        '입금액' AS 적요,
                        -b.accin AS 금액,
                        2 AS 적요순서
                    FROM tb_banktransit b
                    WHERE b.trdate BETWEEN '20250101' AND '20250423'
                ) x
                ORDER BY x.cltcd, x.일자, x.적요순서;
        		""";
        if(companyCode != null){
            sql += " WHERE M.Code = :companyCode";
        }


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
