package mes.app.transaction.Service;

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
                                                    String remark,
                                                    String eumNum) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);
        dicParam.addValue("accountNum", accountNum);
        dicParam.addValue("depositType", depositType);
        dicParam.addValue("remark", "%" + remark + "%");
        dicParam.addValue("eumNum", eumNum);

        String sql = """
                SELECT b.TRDATE,
                    c.id,
                    c."Name",
                    b.ACCOUT,
                    b.IOTYPE,
                    a.accname,
                    b.ACCNUM,
                    b.TRID,
                    b.REMARK1 || ' ' || b.REMARK2 || ' ' || b.REMARK3 || ' ' || b.REMARK4 AS remark
                 FROM tb_banktransit b 
                 LEFT JOIN company c ON c.id = b.cltcd 
                 LEFT JOIN tb_account a ON b.accnum = a.accnum
                 WHERE 1=1
                 AND ioflag = '1'
        		""";
        if(companyCode != null){
            sql += " AND c.id = :companyCode";
        }
        if(accountNum != null && !accountNum.isEmpty()){
            sql += " AND b.ACCNUM = :AccountNumber";
        }
        if(depositType != null && !depositType.isEmpty()){
            sql += " AND b.IOTYPE = :depositType";
        }
        if(remark != null && !remark.isEmpty()){
            sql += " AND COALESCE(b.REMARK1, '') || COALESCE(b.REMARK2, '') || COALESCE(b.REMARK3, '') || COALESCE(b.REMARK4, '') LIKE :remark";
        }
        if(eumNum != null && !eumNum.isEmpty()){
            sql += " AND b.eumnum = :eumNum";
        }
        sql += " ORDER BY b.TRDATE ASC";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
