package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DepositListService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getDepositList(String depositType, Timestamp start, Timestamp end, String company, String txtDescription) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", company);
    paramMap.addValue("txtDescription", txtDescription);

    String sql = """
        select
           tb.trdate ,
           tb.accin ,
           c."Name" as "CompanyName" ,
           tb.iotype ,
           sc."Value" as deposit_type,
           sc."Code" as deposit_code,
           tb.banknm ,
           tb.accnum ,
           tt.tradenm ,
           tb.remark1
           from tb_banktransit tb
           left join company c on c.id = tb.cltcd
           left join  sys_code sc on sc."Code" = tb.iotype
           left join tb_trade tt on tb.trid = tt.trid
           where 1=1 
        """;
    if (depositType != null && !depositType.isEmpty()) {
      sql += " AND sc.\"Value\" ILIKE :depositType ";
      paramMap.addValue("depositType", "%" + depositType + "%");
    }

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("입금현황 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

}
