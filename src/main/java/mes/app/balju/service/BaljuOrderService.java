package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
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
          , sh."Name" as "ShipmentStateName"
          , b."State"
          , b."UnitPrice" as "BaljuUnitPrice"
          , b."Price" as "BaljuPrice"
          , to_char(b."_created", 'yyyy-mm-dd') as create_date
          , case b."PlanTableName" when 'prod_week_term' then '주간계획' when 'bundle_head' then '임의계획' else b."PlanTableName" end as plan_state
          from balju b
          inner join material m on m.id = b."Material_id"
          inner join mat_grp mg on mg.id = m."MaterialGroup_id"
          left join unit u on m."Unit_id" = u.id
          left join company c on c.id= b."Company_id"
          left join store_house sh ON sh.id::varchar = b."ShipmentState"
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
            , fn_code_name('Balju_type', b."SujuType") as "SujuTypeName"
            , to_char(b."ProductionPlanDate", 'yyyy-mm-dd') as production_plan_date
            , to_char(b."ShipmentPlanDate", 'yyyy-mm-dd') as shiment_plan_date
            , b."Description"
            , b."AvailableStock" as "AvailableStock"
            , b."ReservationStock" as "ReservationStock"
            , b."SujuQty2" as "SujuQty2"
            , b."State"
            ,b."UnitPrice" as "BaljuUnitPrice"
            ,b."Price" as "BaljuPrice"
            ,b."Vat"as "BaljuVat"
            , fn_code_name('balju_state', b."State") as "StateName"
            , to_char(b."_created", 'yyyy-mm-dd') as create_date
            from balju b
            inner join material m on m.id = b."Material_id"
            inner join mat_grp mg on mg.id = m."MaterialGroup_id"
            left join unit u on m."Unit_id" = u.id
            left join company c on c.id= b."Company_id"
            where b.id = :id
			""";
//    log.info("발주상세 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    Map<String,Object> item = this.sqlRunner.getRow(sql, paramMap);

    return item;
  }

  //주문 번호 생성
  @Transactional
  public String makeJumunNumber(Date dataDate) {
    String baseDate = new SimpleDateFormat("yyyyMMdd").format(dataDate);

    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("data_date", baseDate);
    paramMap.addValue("code", "BaljuNumber");

    int currVal = 1;

    // 1. 현재 값 조회
    String checkSql = """
        SELECT "CurrVal" 
        FROM seq_maker 
        WHERE "Code" = :code AND "BaseDate" = :data_date
        FOR UPDATE
    """;
    Map<String, Object> mapRow = sqlRunner.getRow(checkSql, paramMap);

    if (mapRow != null && mapRow.containsKey("CurrVal")) {
      currVal = (int) mapRow.get("CurrVal") + 1;

      // 2. 시퀀스 업데이트
      String updateSql = """
            UPDATE seq_maker 
            SET "CurrVal" = :currVal, "_modified" = now()
            WHERE "Code" = :code AND "BaseDate" = :data_date
        """;
      paramMap.addValue("currVal", currVal);
      sqlRunner.execute(updateSql, paramMap);

    } else {
      // 3. 신규 row 생성
      currVal = 1;

      String insertSql = """
            INSERT INTO seq_maker("Code", "BaseDate", "Code2", "CurrVal", "_modified") 
            VALUES (:code, :data_date, NULL, :currVal, now())
        """;
      paramMap.addValue("currVal", currVal);
      sqlRunner.execute(insertSql, paramMap);
    }

    // 4. 주문번호 조립
    String jumunNumber = baseDate + "-" + String.format("%04d", currVal);
    //log.info("✅ 최종 생성된 주문번호: {}", jumunNumber);
    return jumunNumber;
  }

  public List<Map<String, Object>> getBaljuPrice(int materialId, String jumunDate, int companyId) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("mat_pk", materialId);
    dicParam.addValue("company_id", companyId);
    dicParam.addValue("ApplyStartDate", jumunDate);

    String sql = """
			select mcu.id 
            , mcu."Company_id"
            , c."Name" as "CompanyName"
            , mcu."UnitPrice" 
            , mcu."FormerUnitPrice" 
            , mcu."ApplyStartDate"::date 
            , mcu."ApplyEndDate"::date 
            , mcu."ChangeDate"::date 
            , mcu."ChangerName" 
            from mat_comp_uprice mcu 
            inner join company c on c.id = mcu."Company_id"
            where 1=1
            and mcu."Material_id" = :mat_pk
            and mcu."Company_id" = :company_id
            and to_date(:ApplyStartDate, 'YYYY-MM-DD') between mcu."ApplyStartDate"::date and mcu."ApplyEndDate"::date
            and mcu."Type" = '01'
            order by c."Name", mcu."ApplyStartDate" desc
        """;

//    log.info("발주 단가 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
    return items;
  }

  public void updateMatCompUnitPrice(int materialId, int companyId, String jumunDate, double newUnitPrice, String changerName) {
    String sql = """
        UPDATE mat_comp_uprice
        SET "FormerUnitPrice" = "UnitPrice",
            "UnitPrice" = :unitPrice,
            "ChangeDate" = now(),
            "ChangerName" = :changerName
        WHERE "Material_id" = :materialId
          AND "Company_id" = :companyId
          AND TO_DATE(:jumunDate, 'YYYY-MM-DD') BETWEEN "ApplyStartDate" AND "ApplyEndDate"
          AND "Type" = '01'
    """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("unitPrice", newUnitPrice)
        .addValue("changerName", changerName)
        .addValue("materialId", materialId)
        .addValue("companyId", companyId)
        .addValue("jumunDate", jumunDate);

    int affected = sqlRunner.execute(sql, params);
    //log.info("🔁 단가 업데이트 완료 (이전 단가 백업 포함): {}건", affected);
  }

  public List<Map<String, Object>> balju_stop(Integer id) {
    // 1. 현재 상태 조회
    String selectSql = "SELECT \"State\" FROM balju WHERE id = :id;";
    MapSqlParameterSource selectParams = new MapSqlParameterSource()
        .addValue("id", id);

    String currentState = sqlRunner.queryForObject(
        selectSql,
        selectParams,
        (rs, rowNum) -> rs.getString("State")
    );
    // 2.새 상태값 결정
    String newState = "canceled";
    if ("canceled".equalsIgnoreCase(currentState)) {
      newState = "draft"; // 다시 입고 가능한 상태로
    }

    // 3. 상태 업데이트
    String updateSql = """
        UPDATE balju
        SET "State" = :state
        WHERE id = :id
    """;
    MapSqlParameterSource updateParams = new MapSqlParameterSource()
        .addValue("state", newState)
        .addValue("id", id);

    int affected = sqlRunner.execute(updateSql, updateParams);
//    log.info("상태 변경 완료: {} → {}, affected = {}", currentState, newState, affected);

    return List.of(Map.of(
        "updatedRows", affected,
        "newState", newState
    ));
  }

}
