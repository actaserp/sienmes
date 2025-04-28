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
                 b.banknm as bankname,
                 b.bankpopcd as managementnum,
                 accnum as accountNumber,
                 accname as accountName,
                 onlineid as onlineBankId,
                 onlinepw as onlineBankPw,
                 accpw as paymentPw,
                 popsort as accountType,
                 accbirth as birth,
                 popyn as popyn,
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
}
