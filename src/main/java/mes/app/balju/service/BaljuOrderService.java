package mes.app.balju.service;

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
public class BaljuOrderService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getBaljuList(String date_kind, Timestamp start, Timestamp end) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("date_kind", date_kind);
    dicParam.addValue("start", start);
    dicParam.addValue("end", end);

    String sql = """
        select b.id
          , b."JumunNumber"
          , b."Material_id" as "Material_id"
          , mg."Name" as "MaterialGroupName"
          , mg.id as "MaterialGroup_id"
          , fn_code_name('mat_type', mg."MaterialType") as "MaterialTypeName"
          , m.id as "Material_id"
          , m."Code" as product_code
          , m."Name" as product_name
          , u."Name" as unit
          , b."SujuQty" as "SujuQty"
          , to_char(b."JumunDate", 'yyyy-mm-dd') as "JumunDate"
          , to_char(b."DueDate", 'yyyy-mm-dd') as "DueDate"
          , b."CompanyName"
          , b."Company_id"
          , b."SujuType"
          , fn_code_name('Balju_type', b."SujuType") as "BaljuTypeName"
          , to_char(b."ProductionPlanDate", 'yyyy-mm-dd') as production_plan_date
          , to_char(b."ShipmentPlanDate", 'yyyy-mm-dd') as shiment_plan_date
          , b."Description"
          , b."AvailableStock" as "AvailableStock"
          , b."ReservationStock" as "ReservationStock"
          , b."SujuQty2" as "SujuQty2"
          , fn_code_name('balju_state', b."State") as "StateName"
          , fn_code_name('shipment_state', b."ShipmentState") as "ShipmentStateName"
          , b."State"
          , to_char(b."_created", 'yyyy-mm-dd') as create_date
          , case b."PlanTableName" when 'prod_week_term' then '주간계획' when 'bundle_head' then '임의계획' else b."PlanTableName" end as plan_state
          from balju b
          inner join material m on m.id = b."Material_id"
          inner join mat_grp mg on mg.id = m."MaterialGroup_id"
          left join unit u on m."Unit_id" = u.id
          left join company c on c.id= b."Company_id"
          where 1 = 1
			""";

    if (date_kind.equals("sales")) {
      sql += """
				and b."JumunDate" between :start and :end 
		        order by b."JumunDate" desc,  m."Name"
				""";
    } else {
      sql +="""
				and b."DueDate" between :start and :end
		        order by b."DueDate" desc,  m."Name"
				""";
    }
//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  public Map<String, Object> getBaljuDetail(int id) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("id", id);

    String sql = """
			select b.id
            , b."JumunNumber"
            , b."Material_id" as "Material_id"
            , mg."Name" as "MaterialGroupName"
            , mg.id as "MaterialGroup_id"
            , fn_code_name('mat_type', mg."MaterialType") as "MaterialTypeName"
            , m.id as "Material_id"
            , m."Code" as product_code
            , m."Name" as product_name
            , u."Name" as unit
            , b."SujuQty" as "SujuQty"
            , to_char(b."JumunDate", 'yyyy-mm-dd') as "JumunDate"
            , to_char(b."DueDate", 'yyyy-mm-dd') as "DueDate"
            , b."CompanyName"
            , b."Company_id"
            , b."SujuType"
            , fn_code_name('suju_type', b."SujuType") as "SujuTypeName"
            , to_char(b."ProductionPlanDate", 'yyyy-mm-dd') as production_plan_date
            , to_char(b."ShipmentPlanDate", 'yyyy-mm-dd') as shiment_plan_date
            , b."Description"
            , b."AvailableStock" as "AvailableStock"
            , b."ReservationStock" as "ReservationStock"
            , b."SujuQty2" as "SujuQty2"
            , b."State"
            , fn_code_name('suju_state', b."State") as "StateName"
            , to_char(b."_created", 'yyyy-mm-dd') as create_date
            from balju b
            inner join material m on m.id = b."Material_id"
            inner join mat_grp mg on mg.id = m."MaterialGroup_id"
            left join unit u on m."Unit_id" = u.id
            left join company c on c.id= b."Company_id"
            where b.id = :id
			""";

    Map<String,Object> item = this.sqlRunner.getRow(sql, paramMap);

    return item;
  }
}
