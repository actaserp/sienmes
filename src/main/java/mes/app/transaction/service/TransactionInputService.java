package mes.app.transaction.service;


import mes.domain.repository.TB_ACCOUNTRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Service
public class TransactionInputService {


    private final TB_ACCOUNTRepository accountRepository;
    private final SqlRunner sqlRunner;

    public TransactionInputService(TB_ACCOUNTRepository accountRepository, SqlRunner sqlRunner) {
        this.accountRepository = accountRepository;

        this.sqlRunner = sqlRunner;
    }


    public List<Map<String, Object>> getAccountList(){

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        String sql = """
                SELECT
                 a.accid as accountid,
                 b.banknm as bankname,
                 b.bankpopcd as managementnum,
                 accnum as accountNumber,
                 accname as accountName,
                 onlineid as onlineBankId,
                 onlinepw as onlineBankPw,
                 accpw as paymentPw,
                 accbirth as birth,
                 case when popyn = '1' then true
                 else false
                 end as popyn,
                 case when popsort = '1' then '개인'
                 else '법인'
                 end as accounttype
                 FROM tb_account a
                left join tb_xbank b on b.bankid = a.bankid
                 ORDER BY accid ASC
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }

    public List<Map<String, Object>> getTransactionHistory(String searchfrdate, String searchtodate, String TradeType, Integer parsedAccountId){

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("searchfrdate", searchfrdate);
        parameterSource.addValue("searchtodate", searchtodate);
        parameterSource.addValue("accid", parsedAccountId);
        parameterSource.addValue("ioflag", TradeType);


        String sql = """
                SELECT to_char(to_date(trdate, 'YYYYMMDD'), 'YYYY-MM-DD') as trade_date
                ,accin as input_money
                ,accout as output_money
                ,feeamt as commission
                ,remark1 as remark
                ,trid as trade_type
                ,banknm as bankname
                ,accnum as account
                FROM public.tb_banktransit
                where trdate between :searchfrdate and :searchtodate
                """;

        if(TradeType != null && !TradeType.isEmpty()){
            sql += """
                    AND ioflag = :ioflag
                    """;
        }

        if(parsedAccountId != null){
            sql += """
                    AND accid = :accid
                    """;
        }

        sql += """
                ORDER BY trdate desc
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }


}
