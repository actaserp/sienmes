package mes.app.transaction.Service;

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
                                                    Integer companyCode
                                                    ) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);

        String sql = """
                WITH LASTTbl as (
                   select cltcd, max(yyyymm) as yyyymm    from tb_yearamt     where yyyymm < '202501' and ioflag = '1'     group by cltcd
                )SELECT
                    m.id AS cltcd, -- 거래처코드
                    m."Name",
                    COALESCE(y.yearamt, 0) AS yearamt, -- 전잔액
                    COALESCE(s.TOTALAMT, 0) totalamt, -- 매입액
                    COALESCE(b.ACCOUT, 0) AS accout, -- 출금액
                    COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCOUT, 0) AS nowamt -- 현잔액
                FROM COMPANY M
                LEFT JOIN (
                    SELECT y.cltcd, SUM(y.yearamt) AS yearamt    FROM tb_yearamt y     join LASTTbl h      on y.cltcd = h.cltcd and y.yyyymm = h.yyyymm and y.ioflag = '1'
                    GROUP BY y.cltcd
                ) y ON m.id = y.cltcd
                LEFT JOIN (
                    SELECT cltcd, SUM(totalamt) AS sales_amt
                    FROM tb_salesment
                    WHERE misdate BETWEEN '20250101' AND '20250131'
                    GROUP BY cltcd
                ) jan_s ON m.id = jan_s.cltcd

                LEFT JOIN (
                    SELECT cltcd, 
                    SUM(TOTALAMT) AS TOTALAMT    
                    FROM tb_salesment    
                    WHERE misdate between '20250201' and '20250423'   
                    GROUP BY cltcd
                ) s ON m.id = s.cltcd

                LEFT JOIN (
                    SELECT cltcd, SUM(ACCOUT) AS ACCOUT    FROM tb_banktransit    WHERE TRDATE between '20250101' and '20250423'   GROUP BY cltcd
                ) b ON m.id = b.cltcd
                    WHERE COALESCE(y.yearamt, 0) + COALESCE(s.TOTALAMT, 0) - COALESCE(b.ACCOUT, 0) <> 0
        		""";
        if(companyCode != null){
            sql += " AND M.Code = :companyCode";
        }

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

    // 미지급현황 상세 리스트 조회
    public List<Map<String, Object>> getPayableDetailList(String date_from,
                                                    String date_to,
                                                    Integer companyCode
    ) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);

        String sql = """
                WITH lasttbl  as (
                         select cltcd,
                         yearamt,
                         max(yyyymm) as yyyymm   
                         from tb_yearamt    
                         where yyyymm < '202501' and ioflag = '1' and cltcd = '1000'   
                         group by cltcd, yearamt
                      ),  saletbl  as (
                         select s.cltcd,
                         sum(s.totalamt) as totsale   
                         from tb_salesment s   
                         where s.misdate BETWEEN '20250101' AND '20250131' and cltcd = '1000'   
                         group by cltcd
                      ), incomtbl  as (
                         select s.cltcd,
                         sum(s.accout) as totaccout   
                         from tb_banktransit s   
                         where s. trdate BETWEEN '20250101' AND '20250131'  and cltcd = '1000'   
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
                              TO_DATE('20241201', 'YYYYMMDD') AS todate,
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
                          WHERE s.misdate BETWEEN '20250101' AND '20250423'
                
                          UNION ALL
                      
                          SELECT
                              b.cltcd,
                              TO_DATE(b.trdate, 'YYYYMMDD') AS todate,
                              '입금액' AS remark1,
                              b.accin AS total,
                              2 AS remarkCnt
                          FROM tb_banktransit b
                          WHERE b.trdate BETWEEN '20250101' AND '20250423'
                      ) x
                      LEFT JOIN company a ON x.id = a.id
                      ORDER BY x.id, x.todate, x.remarkCnt
        		""";
        if(companyCode != null){
            sql += " WHERE M.Code = :companyCode";
        }

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
