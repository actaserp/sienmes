package mes.app.definition.service;

import mes.domain.repository.TB_ACCOUNTRepository;
import mes.domain.services.SqlRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Service
public class RegiAccountService {


    private final TB_ACCOUNTRepository accountRepository;
    private final SqlRunner sqlRunner;

    public RegiAccountService(TB_ACCOUNTRepository accountRepository, SqlRunner sqlRunner) {
        this.accountRepository = accountRepository;
        this.sqlRunner = sqlRunner;
    }


    public List<Map<String, Object>> getAccountList(Integer bankid, String accnum, String spjangcd){
        MapSqlParameterSource param = new MapSqlParameterSource();

        param.addValue("spjangcd", spjangcd);
        param.addValue("bankid", bankid);
        param.addValue("accnum", "%" + accnum + "%");

        String sql = """
                select 
                 a.accid
                ,a.bankid as bankid
                ,a.accnum
                ,a.accname
                ,a.mijamt
                ,a.onlineid
                ,a.onlinepw
                ,a.accpw
                ,case 
                    when a.popyn = '1' then '연동'
                    else '미연동'
                end as popyn     
                ,b.banknm as bankname 
                from
                tb_account a
                left join tb_xbank b on a.bankid = b.bankid
                where 1=1
                """;

        if(bankid != null){
            sql += """
                and a.bankid = :bankid
                """;
        }

        if(accnum != null || !accnum.isEmpty()){
            sql += """
                and a.accnum like :accnum
                """;
        }

        sql += """
                and a.spjangcd = :spjangcd
                """;

        return sqlRunner.getRows(sql, param);
    }
}
