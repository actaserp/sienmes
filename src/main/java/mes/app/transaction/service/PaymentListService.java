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
                                                    String remark) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);
        dicParam.addValue("accountNum", accountNum);
        dicParam.addValue("depositType", depositType);
        dicParam.addValue("remark", remark);

        String sql = """
                SELECT b.TRDATE,
                    -- c.id,
                    -- c."Name",
                    b.ACCOUT,
                    b.IOTYPE,
                    b.BANKNM,
                    b.ACCNUM,
                    b.TRID,
                    b.REMARK1
                 FROM tb_banktransit b 
                 -- JOIN company c ON c.id = b.cltcd 
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
            sql += " AND b.REMARK1 = :remark";
        }

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
