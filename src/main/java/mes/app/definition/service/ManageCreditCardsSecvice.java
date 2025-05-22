package mes.app.definition.service;

import lombok.extern.slf4j.Slf4j;
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

  public List<Map<String, Object>> getCreditCardsList(String spjangcd, String txtcardnm, String txtcardnum) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("txtcardnm", txtcardnm);
    dicParam.addValue("txtcardnum", txtcardnum);

    String sql = """
        select * from tb_iz010 ti
        where ti.spjangcd = :spjangcd 
        """;

    if (txtcardnum != null && !txtcardnum.isEmpty()) {  //카드번호
      sql += " and ti.cardnum like :txtcardnm ";
      dicParam.addValue("txtcardnm", "%" + txtcardnum + "%");
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
}
