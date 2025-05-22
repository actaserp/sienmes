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
        """;

//    log.info("비용 항목 상세 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }
}
