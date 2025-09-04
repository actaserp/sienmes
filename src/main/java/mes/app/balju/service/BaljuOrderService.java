package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BaljuOrderService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getBaljuList(String date_kind, Timestamp start, Timestamp end, String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("date_kind", date_kind);
    dicParam.addValue("start", start);
    dicParam.addValue("end", end);
    dicParam.addValue("spjangcd", spjangcd);

    String sql = """
        WITH s2 AS (            -- Standard2 숫자만 저장 → 그대로 사용
           SELECT
             m.id AS material_id,
             CASE
               WHEN m."Standard2" IS NULL OR btrim(m."Standard2") = '' THEN NULL
               ELSE NULLIF(
                      regexp_replace(m."Standard2", '[^0-9\\.]', '', 'g'),
                      ''
                    )::numeric
             END AS s2_num
         FROM material m
         ),
         unitw AS (              -- 단위 미표시 → kg 가정
           SELECT
             s2.material_id,
             s2.s2_num AS unit_weight_kg
           FROM s2
         ),
         base_data AS (
           SELECT
             bh.id AS bh_id,
             bh."Company_id",
             b."CompanyName",
             b."BaljuHead_id",
             bh."JumunDate",
             bh."JumunNumber",
             mg."Name" AS "MaterialGroupName",
             fn_code_name('Balju_type', bh."SujuType") AS "BaljuTypeName",
             b.id AS balju_id,
             m."Code" AS product_code,
             m."Name" AS product_name,
             u."Name" AS unit,
             b."SujuQty",
             -- ▼ 단위중량(kg) 컬럼도 같이 노출 (질문 예시 표의 '단위중량')
             uw.unit_weight_kg AS "단위중량",
             b."UnitPrice",
             b."Price",
             b."Vat",
             b."TotalAmount",
             fn_code_name('balju_state', bh."State") AS "StateName",
             mi."SujuQty2" AS "SujuQty2", -- 입고 '중량'
            COALESCE(mi."SujuQty2",0) / NULLIF(uw.unit_weight_kg, 0) AS "SujuQty2_ea",  -- ★ 추가: 입고 '개수'
            GREATEST(                                              -- ★ 변경: 미입고(잔여) '개수'
              COALESCE(b."SujuQty",0)
              - (COALESCE(mi."SujuQty2",0) / NULLIF(uw.unit_weight_kg, 0)),
              0
            ) AS "SujuQty3",
             sh."Name" AS "ShipmentStateName",
             bh."DeliveryDate",
             b."Description",
          -- ■ BalJuHeadType (영문 코드) — 라인 상태 집계만 사용
          (
            SELECT CASE
                     WHEN s.cnt_force     = s.cnt_total AND s.cnt_total > 0 THEN 'force_completion'
                     WHEN s.cnt_canceled  = s.cnt_total THEN 'canceled'
                     WHEN s.cnt_received  = s.cnt_total THEN 'received'
                     WHEN s.cnt_draft     = s.cnt_total THEN 'draft'
                     WHEN s.cnt_partial   = s.cnt_total THEN 'partial'
                     ELSE 'partial'  -- 섞여 있으면 partial
                   END
            FROM (
              SELECT
                COUNT(*) FILTER (WHERE x.line_state = 'force_completion') AS cnt_force,
                COUNT(*) FILTER (WHERE x.line_state = 'canceled')         AS cnt_canceled,
                COUNT(*) FILTER (WHERE x.line_state = 'received')         AS cnt_received,
                COUNT(*) FILTER (WHERE x.line_state = 'draft')            AS cnt_draft,
                COUNT(*) FILTER (WHERE x.line_state = 'partial')          AS cnt_partial,
                COUNT(*)                                                 AS cnt_total
              FROM (
                SELECT
                  CASE
                    WHEN b2."State" = 'canceled'         THEN 'canceled'         -- 명시 상태 우선
                    WHEN b2."State" = 'force_completion' THEN 'force_completion' -- 명시 상태 우선
                ELSE CASE
                       -- 잔량 = 발주개수 - 입고개수
                       WHEN ABS(
                              COALESCE(b2."SujuQty",0)
                              - (COALESCE(mi2."SujuQty2",0) / NULLIF(u2.unit_weight_kg,0))
                            ) < 1e-9 THEN 'received'
                       WHEN (
                              COALESCE(b2."SujuQty",0)
                              - (COALESCE(mi2."SujuQty2",0) / NULLIF(u2.unit_weight_kg,0))
                            ) > 0 THEN 'partial'
                       ELSE 'draft'
                     END
                  END AS line_state
                FROM balju b2
                LEFT JOIN (
                  SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
                  FROM mat_inout
                  WHERE "SourceTableName"='balju' AND COALESCE("_status",'a')='a'
                  GROUP BY "SourceDataPk"
                ) mi2 ON mi2."SourceDataPk" = b2.id
                INNER JOIN material m2 ON m2.id = b2."Material_id"
                LEFT JOIN unitw u2 ON u2.material_id = m2.id  -- unitw CTE 재사용
                WHERE b2."BaljuHead_id" = bh.id
              ) x
            ) s
          ) AS "BalJuHeadType",
          -- ■ bh_StateName (한글명) — 같은 로직을 코드명 매핑
          fn_code_name(
            'balju_state',
            (
              SELECT CASE
                       WHEN s.cnt_force     = s.cnt_total AND s.cnt_total > 0 THEN 'force_completion'
                       WHEN s.cnt_canceled  = s.cnt_total THEN 'canceled'
                       WHEN s.cnt_received  = s.cnt_total THEN 'received'
                       WHEN s.cnt_draft     = s.cnt_total THEN 'draft'
                       WHEN s.cnt_partial   = s.cnt_total THEN 'partial'
                       ELSE 'partial'
                     END
              FROM (
                SELECT
                  COUNT(*) FILTER (WHERE x.line_state = 'force_completion') AS cnt_force,
                  COUNT(*) FILTER (WHERE x.line_state = 'canceled')         AS cnt_canceled,
                  COUNT(*) FILTER (WHERE x.line_state = 'received')         AS cnt_received,
                  COUNT(*) FILTER (WHERE x.line_state = 'draft')            AS cnt_draft,
                  COUNT(*) FILTER (WHERE x.line_state = 'partial')          AS cnt_partial,
                  COUNT(*)                                                 AS cnt_total
                FROM (
                  SELECT
           CASE
             WHEN b2."State" = 'canceled'         THEN 'canceled'
             WHEN b2."State" = 'force_completion' THEN 'force_completion'
             ELSE
               CASE
                 -- 단위중량 없음/0 → 안전 분기(중량 기준)
                 WHEN NULLIF(u2.unit_weight_kg, 0) IS NULL THEN
                   CASE
                     WHEN COALESCE(mi2."SujuQty2", 0) = 0 THEN 'draft'   -- 전혀 입고 없음
                     ELSE 'partial'                                      -- 일부(중량) 입고
                   END
                 -- 단위중량 있음 → EA 기준 판정
                 ELSE
                   CASE
                     WHEN ABS(
                            COALESCE(b2."SujuQty",0)
                            - (COALESCE(mi2."SujuQty2",0) / u2.unit_weight_kg)
                          ) < 1e-9
                       THEN 'received'                                   -- 완납
                     WHEN COALESCE(mi2."SujuQty2",0) = 0
                       THEN 'draft'                                      -- 전혀 입고 없음
                     WHEN (COALESCE(mi2."SujuQty2",0) / u2.unit_weight_kg) > 0
                        AND (COALESCE(b2."SujuQty",0)
                             - (COALESCE(mi2."SujuQty2",0) / u2.unit_weight_kg)) > 0
                       THEN 'partial'                                    -- 일부 입고
                     ELSE 'draft'
                   END
               END
           END AS line_state
         FROM balju b2
         LEFT JOIN (
           SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
           FROM mat_inout
           WHERE "SourceTableName"='balju' AND COALESCE("_status",'a')='a'
           GROUP BY "SourceDataPk"
         ) mi2 ON mi2."SourceDataPk" = b2.id
         INNER JOIN material m2 ON m2.id = b2."Material_id"
         LEFT JOIN unitw u2 ON u2.material_id = m2.id
         WHERE b2."BaljuHead_id" = bh.id
                ) x
              ) s
            )
          ) AS "bh_StateName",
             ROW_NUMBER() OVER (PARTITION BY bh."JumunNumber" ORDER BY b.id ASC) AS rn
           FROM balju_head bh
           LEFT JOIN balju b
             ON b."BaljuHead_id" = bh.id AND b.spjangcd = bh.spjangcd AND b."JumunNumber" = bh."JumunNumber"
           INNER JOIN material m
             ON m.id = b."Material_id" AND m.spjangcd = b.spjangcd
           INNER JOIN mat_grp mg
             ON mg.id = m."MaterialGroup_id" AND mg.spjangcd = b.spjangcd
           LEFT JOIN unit u
             ON m."Unit_id" = u.id AND u.spjangcd = b.spjangcd
           LEFT JOIN store_house sh
             ON sh.id::varchar = b."ShipmentState" AND sh.spjangcd = b.spjangcd
           LEFT JOIN (
             SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
             FROM mat_inout
             WHERE "SourceTableName" = 'balju' AND COALESCE("_status",'a') = 'a'
             GROUP BY "SourceDataPk"
           ) mi ON mi."SourceDataPk" = b.id
           LEFT JOIN unitw uw ON uw.material_id = m.id
           WHERE bh.spjangcd = :spjangcd
        """;

    if (date_kind.equals("sales")) {
      sql += " AND bh.\"JumunDate\" BETWEEN :start AND :end ";
    } else {
      sql += " AND bh.\"DeliveryDate\" BETWEEN :start AND :end ";
    }

    sql += """
        )
         SELECT
           bh_id,
           "JumunNumber",
           MAX("Company_id") AS "Company_id",
           MAX("CompanyName") AS "CompanyName",
           MAX("BaljuHead_id") AS "BaljuHead_id",
           MAX("JumunDate") AS "JumunDate",
           MAX("MaterialGroupName") AS "MaterialGroupName",
           MAX("BaljuTypeName") AS "BaljuTypeName",
           MAX(CASE WHEN rn = 1 THEN product_code END) AS product_code,
           MAX(CASE WHEN rn = 1 THEN product_name END) AS product_name,
           MAX(CASE WHEN rn = 1 THEN unit END) AS unit,
           SUM("SujuQty") AS "SujuQty",
           MAX("단위중량") AS "단위중량",               -- (참고용 표시)
           SUM("UnitPrice") AS "BaljuUnitPrice",
           SUM("Price") AS "BaljuPrice",
           SUM("Vat") AS "BaljuVat",
           SUM("TotalAmount") AS "BaljuTotalPrice",
           MAX("StateName") AS "StateName",
           MAX("BalJuHeadType") AS "BalJuHeadType",
           MAX("bh_StateName") AS "bh_StateName",
           SUM("SujuQty2") AS "SujuQty2",
           -- ▼ row에서 만든 (SujuQty2/단위중량) 값의 합
           COALESCE(SUM("SujuQty2_ea"), 0) AS "SujuQty2_ea",
           COALESCE(SUM("SujuQty3"), 0) AS "SujuQty3",
           MAX("ShipmentStateName") AS "ShipmentStateName",
           MAX("DeliveryDate") AS "DueDate",
           MAX("Description") AS "Description"
         FROM base_data
         GROUP BY "JumunNumber", bh_id
         ORDER BY MAX("DeliveryDate") DESC, bh_id
        """;

//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    return this.sqlRunner.getRows(sql, dicParam);
  }

  public Map<String, Object> getBaljuDetail(int id) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("id", id);

    String sql = """
        WITH balju_total AS (
           SELECT "BaljuHead_id" AS bh_id,
                  SUM(COALESCE("TotalAmount", 0)) AS total_amount_sum
           FROM balju
           GROUP BY "BaljuHead_id"
         ),
         -- 1개당 중량(kg)
         s2 AS (
           SELECT
             m.id AS material_id,
             CASE
               WHEN m."Standard2" IS NULL OR btrim(m."Standard2")='' THEN NULL
               ELSE NULLIF(
                      regexp_replace(replace(m."Standard2", ',', '.'), '[^0-9\\.]', '', 'g'),
                      ''
                    )::numeric
             END AS unit_weight_kg
           FROM material m
         ),
         -- 누적 입고중량(kg)
         mi AS (
           SELECT "SourceDataPk" AS balju_id,
                  SUM("InputQty") AS recv_kg
           FROM mat_inout
           WHERE "SourceTableName" = 'balju'
             AND COALESCE("_status", 'a') = 'a'
           GROUP BY "SourceDataPk"
         ),
         -- 라인 상태(중량 기준 파생)
         line_state AS (
           SELECT
             b.id AS balju_id,
             b."BaljuHead_id" AS bh_id,
             CASE
               WHEN b."State" = 'canceled' THEN 'canceled'
               WHEN COALESCE(mi.recv_kg, 0) = 0 THEN 'draft'
               WHEN COALESCE(mi.recv_kg, 0) >= (COALESCE(b."SujuQty",0) * COALESCE(s2.unit_weight_kg,1))
                    THEN 'received'
               ELSE 'partial'
             END AS derived_state
           FROM balju b
           LEFT JOIN material m ON m.id = b."Material_id" AND m.spjangcd = b.spjangcd
           LEFT JOIN s2      ON s2.material_id = m.id
           LEFT JOIN mi      ON mi.balju_id   = b.id
         ),
         -- Head 상태(라인 파생 상태 집계)
         head_state AS (
           SELECT
             ls.bh_id,
             CASE
               WHEN COUNT(*) FILTER (WHERE ls.derived_state = 'received') = COUNT(*) THEN 'received'
               WHEN COUNT(*) FILTER (WHERE ls.derived_state = 'draft')    = COUNT(*) THEN 'draft'
               WHEN COUNT(*) FILTER (WHERE ls.derived_state = 'canceled') = COUNT(*) THEN 'canceled'
               ELSE 'partial'
             END AS head_state
           FROM line_state ls
           GROUP BY ls.bh_id
         )

         SELECT
           bh.id AS bh_id,
           bh."Company_id",
           c."Name" AS "CompanyName",
           bh."JumunDate",
           bh."DeliveryDate",
           bh.special_note,
           bh."JumunNumber",
           b.id AS balju_id,
           b."Material_id",
           COALESCE(m."Code", '') AS product_code,
           COALESCE(m."Name", '') AS product_name,
           COALESCE(mg."Name", '') AS "MaterialGroupName",
           COALESCE(mg.id, 0) AS "MaterialGroup_id",
           fn_code_name('mat_type', mg."MaterialType") AS "MaterialTypeName",
           s."Value" as "BaljuTypeName",
           b."SujuQty",
           u."Name" AS unit,
           b."UnitPrice" AS "BaljuUnitPrice",
           b."Price" AS "BaljuPrice",
           b."Vat" AS "BaljuVat",
           b."InVatYN",
           b."TotalAmount" AS "LineTotalAmount",
           COALESCE(bt.total_amount_sum, 0) AS "BaljuTotalPrice",
           TO_CHAR(b."ProductionPlanDate", 'yyyy-mm-dd') AS production_plan_date,
           TO_CHAR(b."ShipmentPlanDate", 'yyyy-mm-dd') AS shiment_plan_date,
           b."Description",
           b."AvailableStock",
           b."ReservationStock",

           -- ★ 라인 상태: 중량 기준 파생 값 사용
           ls.derived_state AS "BalJuType",
           fn_code_name('balju_state', ls.derived_state) AS "balju_StateName",

           COALESCE(mi.recv_kg, 0) AS recv_kg,
           (COALESCE(b."SujuQty",0) * COALESCE(s2.unit_weight_kg,1)) AS order_kg,

           -- ★ Head 상태: 파생 라인 상태 집계 결과 사용
           hs.head_state AS "BalJuHeadType",
           fn_code_name('balju_state', hs.head_state) AS "bh_StateName",

           TO_CHAR(b."_created", 'yyyy-mm-dd') AS create_date

         FROM balju_head bh
         LEFT JOIN balju b         ON b."BaljuHead_id" = bh.id
         LEFT JOIN material m      ON m.id = b."Material_id" AND m.spjangcd = b.spjangcd
         LEFT JOIN s2              ON s2.material_id = m.id
         LEFT JOIN mat_grp mg      ON mg.id = m."MaterialGroup_id" AND mg.spjangcd = b.spjangcd
         LEFT JOIN unit u          ON m."Unit_id" = u.id AND u.spjangcd = b.spjangcd
         LEFT JOIN company c       ON c.id = b."Company_id"
         LEFT JOIN sys_code s      ON bh."SujuType" = s."Code" AND s."CodeType" = 'Balju_type'
         LEFT JOIN mi              ON mi.balju_id = b.id
         LEFT JOIN line_state ls   ON ls.balju_id = b.id
         LEFT JOIN head_state hs   ON hs.bh_id    = bh.id
         LEFT JOIN balju_total bt  ON bt.bh_id    = bh.id
         WHERE bh.id = :id
        """;
//    log.info("발주상세 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);

    if (rows.isEmpty()) return Collections.emptyMap();

    // 공통 헤더 정보 (첫 번째 row 기준)
    Map<String, Object> header = new LinkedHashMap<>();
    Map<String, Object> first = rows.get(0);

    header.put("mode", "edit");
    header.put("id", first.get("bh_id"));
    header.put("Company_id", first.get("Company_id"));
    header.put("CompanyName", first.get("CompanyName"));
    header.put("JumunDate", first.get("JumunDate"));
    header.put("DeliveryDate", first.get("DeliveryDate"));
    header.put("State", first.get("BalJuHeadType"));
    header.put("StateName", first.get("bh_StateName"));
    header.put("special_note", first.get("special_note"));
    header.put("JumunNumber", first.get("JumunNumber"));

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();

      item.put("id", row.get("balju_id"));
      item.put("Material_id", row.get("Material_id"));
      item.put("product_code", row.get("product_code"));
      item.put("product_name", row.get("product_name"));
      item.put("quantity", row.get("SujuQty"));
      item.put("unit_price", row.get("BaljuUnitPrice"));
      item.put("supply_price", row.get("BaljuPrice"));
      item.put("vat", row.get("BaljuVat"));
      item.put("total_price", row.get("LineTotalAmount"));
      item.put("description", row.get("Description"));
      item.put("vatIncluded", row.get("InVatYN"));
      item.put("State", row.get("BalJuType"));
      item.put("balju_StateName", row.get("balju_StateName"));

      items.add(item);
    }

    header.put("items", items);
    return header;
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
    //log.info(" 최종 생성된 주문번호: {}", jumunNumber);
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
                 , mcu."ApplyStartDate"
                 , mcu."ApplyEndDate"
                 , mcu."ChangeDate"
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
    // 1. 현재 상태 조회 (입고 수량 포함)
    String selectSql = """
            SELECT b."State", b."SujuQty", mi."SujuQty2"
            FROM balju b
            LEFT JOIN (
                SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
                FROM mat_inout
                WHERE "SourceTableName" = 'balju' AND COALESCE("_status", 'a') = 'a'
                GROUP BY "SourceDataPk"
            ) mi ON mi."SourceDataPk" = b.id
            WHERE b.id = :id
        """;

    MapSqlParameterSource selectParams = new MapSqlParameterSource().addValue("id", id);

    Map<String, Object> result = sqlRunner.queryForObject(selectSql, selectParams, (rs, rowNum) -> {
      Map<String, Object> map = new HashMap<>();
      map.put("State", rs.getString("State"));
      map.put("SujuQty", rs.getInt("SujuQty"));

      // SujuQty2는 null 가능 → 안전하게 처리
      int sujuQty2 = rs.getInt("SujuQty2");
      if (rs.wasNull()) sujuQty2 = 0;
      map.put("SujuQty2", sujuQty2);

      return map;
    });

    // 2. 현재 값 추출
    String currentState = (String) result.get("State");
    int sujuQty = (int) result.get("SujuQty");
    int sujuQty2 = (int) result.get("SujuQty2");

    // 3. 새 상태값 결정
    String newState;
    if ("canceled".equalsIgnoreCase(currentState)) {
      // 중지 취소 → 입고량에 따라 상태 판단
      if (sujuQty2 == 0) {
        newState = "draft";
      } else if (sujuQty2 < sujuQty) {
        newState = "partial";
      } else {
        newState = "received";
      }
    } else {
      // 중지가 아니면 → 중지 처리
      newState = "canceled";
    }

    // 4. 상태 업데이트
    String updateSql = """
            UPDATE balju
            SET "State" = :state
            WHERE id = :id
        """;

    MapSqlParameterSource updateParams = new MapSqlParameterSource()
        .addValue("state", newState)
        .addValue("id", id);

    int affected = sqlRunner.execute(updateSql, updateParams);

    // 5. 결과 반환
    return List.of(Map.of(
        "updatedRows", affected,
        "newState", newState
    ));
  }


  public int saveCompanyUnitPrice(Map<String, Object> data) {
    Integer materialId = CommonUtil.tryIntNull(data.get("Material_id"));
    Integer companyId = CommonUtil.tryIntNull(data.get("Company_id"));

    // ApplyStartDate 처리
    String applyStartDateStr = CommonUtil.tryString(data.get("ApplyStartDate"));
    LocalDateTime applyStartDateLocal = LocalDateTime.parse(applyStartDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    Timestamp applyStartDate = Timestamp.valueOf(applyStartDateLocal);

    // 현재 날짜와 비교하여 ApplyEndDate 설정
    LocalDate applyStartDateDate = applyStartDateLocal.toLocalDate();
    LocalDate today = LocalDate.now();

    Timestamp applyEndDate = applyStartDateDate.equals(today)
        ? applyStartDate
        : Timestamp.valueOf(applyStartDateDate.minusDays(1).atStartOfDay());

    Timestamp applyEndDate2 = CommonUtil.tryTimestamp("2100-12-31");

    Float unitPrice = CommonUtil.tryFloatNull(data.get("UnitPrice"));
    String changerName = CommonUtil.tryString(data.get("ChangerName"));
    String type = CommonUtil.tryString(data.get("type"));
    Integer userId = CommonUtil.tryIntNull(data.get("user_id"));

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("materialId", materialId);
    dicParam.addValue("companyId", companyId);
    dicParam.addValue("applyStartDate", applyStartDate, java.sql.Types.TIMESTAMP);
    dicParam.addValue("applyEndDate", applyEndDate, java.sql.Types.TIMESTAMP);
    dicParam.addValue("applyEndDate2", applyEndDate2, java.sql.Types.TIMESTAMP);
    dicParam.addValue("unitPrice", unitPrice);
    dicParam.addValue("changerName", changerName);
    dicParam.addValue("userId", userId);
    dicParam.addValue("type", type);
    dicParam.addValue("formerUnitPrice", null);

    String sql = """
        select id, "UnitPrice"
        from mat_comp_uprice
        where "Material_id" = :materialId
        and "Company_id" = :companyId
        and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
        """;

    Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
    if (!MapUtils.isEmpty(item)) {
      dicParam.addValue("formerUnitPrice", CommonUtil.tryFloatNull(item.get("UnitPrice")));
    }

    sql = """
        update mat_comp_uprice
        set "ApplyEndDate" = :applyEndDate
        where "Material_id" = :materialId
        and "Company_id" = :companyId
        and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
        """;

    this.sqlRunner.execute(sql, dicParam);

    sql = """
        INSERT INTO public.mat_comp_uprice
        ("_created", "_creater_id", "Material_id", "Company_id", "ApplyStartDate", 
         "ApplyEndDate", "UnitPrice", "FormerUnitPrice", "ChangeDate", "ChangerName", "Type")
        VALUES (
         now(), :userId, :materialId, :companyId, :applyStartDate,
         :applyEndDate2, :unitPrice, :formerUnitPrice, now(), :changerName, :type
        )
        """;

    return this.sqlRunner.execute(sql, dicParam);
  }

  //FROM 데이터 조회용
  public Map<String, Object> getSenderInfo(String userid) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("userid", userid);

    String sql = """
        select
        au.spjangcd ,
        x.spjangnm ,
        x.tel1 ,
        x.adresa
        from auth_user au 
        left join tb_xa012 x on x.spjangcd = au.spjangcd 
        where au.username =:userid
        """;
//    log.info("FROM (발신자) SQL: {}", sql);
//    log.info("FROM (발신자)데이터: {}", paramMap.getValues());
    return this.sqlRunner.getRow(sql, paramMap);
  }

  public Map<String, Object> getReceiverInfo(Integer companyId) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("companyId", companyId);

    String sql = """
        SELECT
            c."Name" AS company_name,
            c."TelNumber"  AS tel,
            c."Address" AS address
        FROM company c
        WHERE c.id = :companyId 
        """;
//    log.info("TO (수신처) SQL: {}", sql);
//    log.info("TO (수신처) 데이터: {}", paramMap.getValues());
    return this.sqlRunner.getRow(sql, paramMap);
  }

  public String getReceiverEmail(Integer bhId) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("bhId", bhId);

    String sql = """
        SELECT c."Email"
        FROM balju_head bh
        LEFT JOIN company c ON c.id = bh."Company_id"
        WHERE bh.id = :bhId
        """;

    return this.sqlRunner.queryForObject(sql, param, (rs, rowNum) -> rs.getString("Email"));
  }

}
