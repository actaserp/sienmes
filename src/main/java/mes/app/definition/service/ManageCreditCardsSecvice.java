package mes.app.definition.service;

import lombok.extern.slf4j.Slf4j;
import mes.Encryption.EncryptionUtil;
import mes.app.aop.DecryptField;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ManageCreditCardsSecvice {

  @Autowired
  SqlRunner sqlRunner;

  @DecryptField(columns = {"cardnum", "accnum", "cardnum_real"} )
  public List<Map<String, Object>> getCreditCardsList(String spjangcd, String txtcardnm, String txtcardnum) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("txtcardnm", txtcardnm); //카드명
    dicParam.addValue("txtcardnum", txtcardnum);  //카드번호

    String sql = """
        select ti.cardnum as cardnum_real , *
        from tb_iz010 ti
        where ti.spjangcd = :spjangcd 
        """;

    if (txtcardnum != null && !txtcardnum.isEmpty()) {  //카드번호
      sql += " and ti.cardnum like :txtcardnum ";
      dicParam.addValue("txtcardnum", "%" + txtcardnum + "%");
    }
    if (txtcardnm != null && !txtcardnm.isEmpty()) {
      sql += " and ti.cardnm like :txtcardnm ";
      dicParam.addValue("txtcardnm", "%" + txtcardnm + "%");
    }
//    log.info("신용카드 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  public String findDecryptedAccountNumberByAccid(Integer accid) throws Exception {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("accid", accid);

    String sql = """
        SELECT accnum 
        FROM tb_account 
        WHERE accid = :accid
        """;

    List<Map<String, Object>> result = this.sqlRunner.getRows(sql, dicParam);

    if (result.isEmpty()) {
      throw new RuntimeException("계좌 정보가 존재하지 않습니다. accid = " + accid);
    }

    String encryptedAccnum = (String) result.get(0).get("accnum");
    return EncryptionUtil.decrypt(encryptedAccnum); // 복호화된 계좌번호 반환
  }

}
