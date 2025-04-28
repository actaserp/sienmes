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

  public List<Map<String, Object>> getDepositList(String depositType, Timestamp start, Timestamp end, String company, String txtDescription, String AccountName) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", company);
    paramMap.addValue("txtDescription", txtDescription);
    paramMap.addValue("AccountName", AccountName);

    String sql = """
        select
           tb.ioid,
           TO_CHAR(tb.trdate::DATE, 'YYYY-MM-DD') AS trdate,
           tb.accin ,
           tb.remark1 as "CompanyName" ,
            -- c."Name" as "CompanyName" ,
           tb.iotype ,
           sc."Value" as deposit_type,
           sc."Code" as deposit_code,
           tb.banknm ,
           tb.accnum ,
           tt.tradenm ,
           tb.remark3
           from tb_banktransit tb
           left join company c on c.id = tb.cltcd
           left join  sys_code sc on sc."Code" = tb.iotype
           left join tb_trade tt on tb.trid = tt.trid
           WHERE tb.ioflag = '0'
           and TO_DATE(tb.trdate, 'YYYYMMDD') between :start and :end
        """;
    if (depositType != null && !depositType.isEmpty()) {
      sql += " AND sc.\"Value\" ILIKE :depositType ";
      paramMap.addValue("depositType", "%" + depositType + "%");
    }

    if (company != null && !company.isEmpty()) {
      sql += " AND tb.cltcd = :company ";
      paramMap.addValue("company", "%" + company + "%");
    }

    if (txtDescription != null && !txtDescription.isEmpty()) {
      sql += " AND tb.remark3 ILIKE :txtDescription ";
      paramMap.addValue("txtDescription", "%" + txtDescription + "%");
    }
    if (AccountName != null && !AccountName.isEmpty()) {
      sql += " AND tb.accid = :AccountName ";
      paramMap.addValue("AccountName", AccountName );
    }

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("입금현황 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

}
