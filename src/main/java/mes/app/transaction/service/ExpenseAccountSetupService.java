package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExpenseAccountSetupService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getExpenseAccountList(String spjangcd) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("spjangcd", spjangcd);

    String sql = """
        select sc."Code" as code, 
        sc."Value" as group_name, 
        sc."Description" as remark
       from sys_code sc
       where sc."CodeType" ='gartcd'
       ORDER BY CAST(sc."Code" AS INTEGER) ASC
        """;

//    log.info("비용 항목등록 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  public List<Map<String, Object>> getExpenseAccountDetail(String groupCode) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("groupCode", groupCode);

    String sql = """
        select * from tb_ca648 tc 
        where tc.gartcd = :groupCode
        ORDER BY CAST(tc.artcd AS INTEGER) 
        """;

//    log.info("비용 항목 상세 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  public String findgartcd(String spjangcd, String gartcd) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("gartcd", gartcd);

    String sql = """
        SELECT MAX(SUBSTRING(artcd, LENGTH(:gartcd) + 1, 2)) AS maxSuffix
        FROM tb_ca648
        WHERE spjangcd = :spjangcd AND gartcd = :gartcd
    """;

    log.info("gartcd 찾기 SQL: {}", sql);
    log.info("SQL Parameters: {}", dicParam.getValues());

    Map<String, Object> row = this.sqlRunner.getRow(sql, dicParam);
    return (String) row.get("maxSuffix");
  }

  public void saveGroupRemark(String spjangcd, String gartcd, String gartName, String remark) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("spjangcd", spjangcd);
    param.addValue("code", gartcd);

    // 1. 존재 여부 확인
    String checkSql = """
        SELECT COUNT(*) 
        FROM sys_code 
        WHERE "CodeType" = 'gartcd' 
          AND "Code" = :code 
          AND spjangcd = :spjangcd
    """;
    int count = sqlRunner.queryForObject(checkSql, param, (rs, rowNum) -> rs.getInt(1));

    if (count > 0) {
      // 2. UPDATE
      String updateSql = """
            UPDATE sys_code 
            SET "Description" = :remark, _modified = NOW()
            WHERE "CodeType" = 'gartcd' 
              AND "Code" = :code 
              AND spjangcd = :spjangcd
        """;
      param.addValue("remark", remark);
      sqlRunner.execute(updateSql, param);
    } else {
      // 3. INSERT
      String insertSql = """
            INSERT INTO sys_code 
            (_status, _created, _modified, _creater_id, _modifier_id, id, "CodeType", "Code", "Value", "Description", _ordering, spjangcd, vercode)
            VALUES 
            ('', NOW(), NULL, 0, 0, nextval('sys_code_id_seq'::regclass), 'gartcd', :code, :value, :remark, 0, :spjangcd, NULL)
        """;
      param.addValue("value", gartName);
      param.addValue("remark", remark);
      sqlRunner.execute(insertSql, param);
    }
  }

}
