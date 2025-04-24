package mes.app.transaction.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PaymentListService {
    @Autowired
    SqlRunner sqlRunner;

    // 지급현황 리스트 조회
    public List<Map<String, Object>> getPaymentList(String date_from,
                                                    String date_to,
                                                    Integer companyCode,
                                                    String accountNum,
                                                    String depositType,
                                                    String remark) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);
        dicParam.addValue("accountNum", accountNum);
        dicParam.addValue("depositType", depositType);
        dicParam.addValue("remark", remark);

        String sql = """
                WITH LASTTbl as (
                   select cltcd, 
                   max(yyyymm) as yyyymm 
                   from tb_yearamt 
                   where yyyymm < ‘202501’ and ioflag = ‘1’     group by cltcd
                )SELECT
                    m.id AS 거래처코드,
                    ISNULL(y.yearamt, 0) AS 전잔액,
                    ISNULL(s.TOTALAMT, 0) AS 매출액,
                    ISNULL(b.ACCIN, 0) AS 입금액,
                    ISNULL(y.yearamt, 0) + ISNULL(s.TOTALAMT, 0) - ISNULL(b.ACCIN, 0) AS 현잔액 
                FROM COMPANY M
                LEFT JOIN (
                    SELECT cltcd,
                           SUM(yearamt) AS yearamt
                           FROM tb_yearamt y
                           join LASTTbl h      
                           on y.cltcd = h.cltcd and y.yyyymm = h.yyyymm and y.ioflag = ‘0’
                ) y ON m.cltcd = y.cltcd
                LEFT JOIN (
                    SELECT cltcd, SUM(totalamt) AS sales_amt
                    FROM tb_invoicement
                    WHERE misdate BETWEEN '20250101' AND '20250131'
                    GROUP BY cltcd
                ) jan_s ON m.cltcd = jan_s.cltcd
                
                LEFT JOIN (
                    SELECT cltcd, 
                    SUM(TOTALAMT) AS TOTALAMT    
                    FROM tb_invoicement    
                    WHERE misdate between ‘20250201’ and ‘20250423’   
                    GROUP BY cltcd
                ) s ON m.cltcd = s.cltcd
                
                LEFT JOIN (
                    SELECT cltcd, 
                    SUM(ACCOUT) AS ACCOUT    
                    FROM tb_banktransit   
                    WHERE TRDATE between ‘20250101’ and ‘20250423’   
                    GROUP BY cltcd
                ) b ON m.cltcd = b.cltcdWHERE
                    ISNULL(y.yearamt, 0) + ISNULL(s.TOTALAMT, 0) - ISNULL(b.ACCIN, 0) <> 0
        		""";
        if(companyCode != null){
            sql += " WHERE M.Code = :companyCode";
        }
        if(accountNum != null && !accountNum.isEmpty()){
            sql += " WHERE M.AccountNumber = :AccountNumber";
        }
        if(depositType != null && !depositType.isEmpty()){
            sql += " WHERE b.IOTYPE = :depositType";
        }
        if(remark != null && !remark.isEmpty()){
            sql += " WHERE b.REMARK1 = :remark";
        }

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
