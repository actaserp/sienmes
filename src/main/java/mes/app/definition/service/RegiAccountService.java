package mes.app.definition.service;

import lombok.extern.slf4j.Slf4j;
import mes.Encryption.EncryptionUtil;
import mes.domain.entity.TB_ACCOUNT;
import mes.domain.repository.TB_ACCOUNTRepository;
import mes.domain.services.SqlRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
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
                ,a.viewid
                ,a.viewpw
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

    @Transactional
    public void saveAccount(Integer pk, Integer bankname, String accnum ,String accname, String onlineid, String viewid, String viewpw){

        TB_ACCOUNT account;

        if (pk != null) {
            account = accountRepository.findById(pk).orElseGet(TB_ACCOUNT::new);
        } else {
            account = new TB_ACCOUNT(); // 새로 생성
        }

        account.setAccid(pk);

        account.setBankid(bankname);
        String EncryptedAccnum = "";
        String Encryptedviewpw = "";


        try{
            EncryptedAccnum = EncryptionUtil.encrypt(accnum);
            Encryptedviewpw = EncryptionUtil.encrypt(viewpw);
        }catch (Exception e){
            log.error("암호화 중 에러 발생 , [발생위치]: {} , [에러내용] : {}" , this.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException();
        }

        account.setAccnum(EncryptedAccnum);
        account.setOnlineid(onlineid);
        account.setViewid(viewid);
        account.setViewpw(Encryptedviewpw);
        account.setAccname(accname);


        accountRepository.save(account);
    }

    public void deleteAccount(Integer id) {

        accountRepository.deleteById(id);
    }
}
