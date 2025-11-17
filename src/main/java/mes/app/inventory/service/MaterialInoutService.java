package mes.app.inventory.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;

@Slf4j
@Service
public class MaterialInoutService {

	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getMaterialInout(String srchStartDt, String srchEndDt, String housePk,
			String matType, String matGrpPk, String keyword, String spjangcd) {
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("srchStartDt", srchStartDt);
		param.addValue("srchEndDt", srchEndDt);
		param.addValue("housePk", housePk);
		param.addValue("matType", matType);
		param.addValue("matGrpPk", matGrpPk);
		param.addValue("keyword", keyword);
		param.addValue("spjangcd", spjangcd);
		
		String sql = """
					select distinct mi.id as mio_pk
                    , fn_code_name('inout_type', mi."InOut") as inout
                    , mi."Material_id"
                    , mi."InputType" 
                    , mi."OutputType" 
                    , case when mi."InOut" = 'in' then fn_code_name('input_type', mi."InputType") 
	                    when mi."InOut" = 'out' then fn_code_name('output_type', mi."OutputType") 
	                    when mi."InOut" = 'recall' then fn_code_name('recall_type', mi."OutputType")
	                    when mi."InOut" = 'return' then fn_code_name('return_type', mi."InputType")
	                    end as inout_type
                    , to_char(mi."InoutDate",'yyyy-mm-dd ') as "InoutDate"
                    , to_char(mi."InoutTime", 'hh24:mi') as "InoutTime"
                    , sh."Name" as "store_house_name"
                    , m."Code" as "material_code"
                    , m."Name" as "material_name"
                    , m."CurrentStock" 
                    , m."ValidDays"
                    , m."LotSize"
                    , m."PackingUnitQty"
                    , mi."StoreHouse_id"
                    , mih2."CurrentStock" as "HouseStock"
                    , m."SafetyStock" 
                    , coalesce(mi."InputQty", 0) as "InputQty"
                    , coalesce(mi."OutputQty", 0) as "OutputQty"
                    , u2."Name" as "unit_name"
                    , mi."Description" 
                    , fn_code_name('mat_type', mg."MaterialType") as material_type
                    --, coalesce(lot_cnt.lot_count,0) as lot_count
                    , (select count(ml."LotNumber") as lot_count 
                        from mat_lot ml 
                        where ml."SourceTableName" ='mat_inout' 
                        and ml."SourceDataPk" = mi.id
                        )  as lot_count 
                    , coalesce(mi."PotentialInputQty",0) as "potentialInputQty"
                    , fn_code_name('inout_state', mi."State" ) as "inout_state"
                    , var."StateName" as "state_name"
                    , tir."JudgeCode" as judge_code
                    , m."LotUseYN" as lot_use
                    from mat_inout mi 
                    inner join material m on mi."Material_id" = m.id
                    left join mat_grp mg on mg.id = m."MaterialGroup_id"
                    inner join store_house sh on mi."StoreHouse_id" = sh.id
                    left join unit u2 on m."Unit_id" = u2.id 
                    --left join mat_order mo on mi."MaterialOrder_id" = mo.id 
                    --and m.id = mo."Material_id" 
                    left join mat_in_house mih2 on mih2."Material_id"  = m.id
                    and mih2."StoreHouse_id" = mi."StoreHouse_id"
                    left join rela_data rd on mi.id = rd."DataPk2" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName2"  = 'mat_inout'
                    left join bundle_head bh on bh.id = rd."DataPk1" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName1"  = 'bundle_head'
                    left join v_appr_result var on var."SourceDataPk" = bh.id and var."SourceTableName" ='bundle_head'
                    left join test_result tr on tr."SourceDataPk"  = mi.id and tr."SourceTableName" = 'mat_inout'
                    left join test_item_result tir on tr.id = tir."TestResult_id"
                    where 1 = 1
                    and m."Useyn" = '0'
                    --and sh."HouseType" = 'material'
                    and mi."InoutDate" between cast(:srchStartDt as date) and cast(:srchEndDt as date)
                    and mi.spjangcd = :spjangcd
				""";
		
		if (StringUtils.isEmpty(housePk)==false) sql +=" and sh.id = cast(:housePk as Integer) ";
		if (StringUtils.isEmpty(matType)==false) sql +=" and mg.\"MaterialType\" = :matType ";
		if (StringUtils.isEmpty(matGrpPk)==false) sql +=" and m.\"MaterialGroup_id\" = cast(:matGrpPk as Integer) ";
		if (StringUtils.isEmpty(keyword)==false) sql +=" and upper(m.\"Name\") like concat('%%',upper(:keyword),'%%') ";
		
		sql += " order by \"InoutDate\" desc, \"InoutTime\" desc, mi.id desc ";
		
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);
        
        return items;
	}

	public List<Map<String, Object>> getMaterialInoutReceipt(
			String srchStartDt, String srchEndDt, String housePk,
			String matType, String matGrpPk, String keyword, String spjangcd) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("srchStartDt", srchStartDt);
		param.addValue("srchEndDt", srchEndDt);
		param.addValue("housePk", housePk);
		param.addValue("matType", matType);
		param.addValue("matGrpPk", matGrpPk);
		param.addValue("keyword", keyword);
		param.addValue("spjangcd", spjangcd);

		String sql = """
        WITH s2 AS (  -- Standard2 → unit_weight_kg (kg/EA)로 환산
          SELECT
            m.id AS material_id,
            lower(COALESCE(m."Standard2", '')) AS s2_raw,
            NULLIF(
              regexp_replace(
                replace(lower(COALESCE(m."Standard2", '')), ',', '.'),
                '[^0-9.\\-]', '', 'g'
              ),
              ''
            )::numeric AS s2_num
          FROM material m
        ),
        unitw AS (  -- kg/EA 값 계산
          SELECT
            s2.material_id,
            CASE
              WHEN s2_num IS NULL       THEN NULL
              WHEN s2_raw LIKE '%t%'    THEN s2_num * 1000      -- ton → kg
              WHEN s2_raw LIKE '%kg%'   THEN s2_num             -- kg
              WHEN s2_raw ~ '(^|[^k])g' THEN s2_num / 1000      -- g → kg (kg와 구분)
              ELSE s2_num               -- 단위 없음 → kg 가정
            END AS unit_weight_kg
          FROM s2
        )
        SELECT DISTINCT
              mi.id AS mio_pk
            , fn_code_name('inout_type', mi."InOut") AS inout
            , mi."Material_id"
            , mi."InputType"
            , mi."OutputType"
            , CASE WHEN mi."InOut" = 'in'     THEN fn_code_name('input_type',  mi."InputType")
                   WHEN mi."InOut" = 'return' THEN fn_code_name('return_type', mi."InputType")
              END AS inout_type
            , to_char(mi."InoutDate",'yyyy-mm-dd ') AS "InoutDate"
            , to_char(mi."InoutTime", 'hh24:mi')    AS "InoutTime"
            , sh."Name" AS "store_house_name"
            , m."Code" AS "material_code"
            , m."Name" AS "material_name"
            , m."CurrentStock"
            , m."ValidDays"
            , m."Standard2" as lot_size
            , m."PackingUnitQty"
            , mi."StoreHouse_id"
            , mih2."CurrentStock" AS "HouseStock"
            , m."SafetyStock"

            -- 저장된 원본(kg) 유지
            , COALESCE(mi."InputQty",  0) AS "InputQty"
            , COALESCE(mi."OutputQty", 0) AS "OutputQty"
            , u2."Name" AS "unit_name"    -- 기본단위(보통 'kg')

            , mi."Description"
            , fn_code_name('mat_type', mg."MaterialType") AS material_type
            , (
                SELECT count(ml."LotNumber")
                FROM mat_lot ml
                WHERE ml."SourceTableName" ='mat_inout'
                  AND ml."SourceDataPk" = mi.id
              ) AS lot_count
            , COALESCE(mi."PotentialInputQty",0) AS "potentialInputQty"
            , fn_code_name('inout_state', mi."State") AS "inout_state"
            , var."StateName" AS "state_name"
            , tir."JudgeCode" AS judge_code
            , m."LotUseYN" AS lot_use

            -- ▼ 파생값: 단위중량(kg/EA) & 개수(EA) 환산
            , uw.unit_weight_kg
            , CASE
                WHEN uw.unit_weight_kg IS NOT NULL AND COALESCE(mi."InputQty",0)  > 0
                  THEN (COALESCE(mi."InputQty",0)  / NULLIF(uw.unit_weight_kg,0))
              END::numeric(18,6) AS "InputQtyEA"
            , CASE
                WHEN uw.unit_weight_kg IS NOT NULL AND COALESCE(mi."OutputQty",0) > 0
                  THEN (COALESCE(mi."OutputQty",0) / NULLIF(uw.unit_weight_kg,0))
              END::numeric(18,6) AS "OutputQtyEA"
            , CASE
                WHEN uw.unit_weight_kg IS NOT NULL THEN 'EA'  -- 환산 성공 시 표시는 EA
                ELSE u2."Name"                               -- 실패 시 기본단위(kg)로
              END AS "disp_unit"

        FROM mat_inout mi
        INNER JOIN material m      ON mi."Material_id" = m.id
        LEFT JOIN  mat_grp mg      ON mg.id = m."MaterialGroup_id"
        INNER JOIN store_house sh  ON mi."StoreHouse_id" = sh.id
        LEFT JOIN  unit u2         ON m."Unit_id" = u2.id
        LEFT JOIN  mat_in_house mih2
               ON mih2."Material_id"  = m.id
              AND mih2."StoreHouse_id" = mi."StoreHouse_id"
        LEFT JOIN  rela_data rd
               ON mi.id = rd."DataPk2"
              AND rd."RelationName" = 'mat_inout_test_result'
              AND rd."TableName2"   = 'mat_inout'
        LEFT JOIN  bundle_head bh
               ON bh.id = rd."DataPk1"
              AND rd."RelationName" = 'mat_inout_test_result'
              AND rd."TableName1"   = 'bundle_head'
        LEFT JOIN  v_appr_result var
               ON var."SourceDataPk" = bh.id
              AND var."SourceTableName" ='bundle_head'
        LEFT JOIN  test_result tr
               ON tr."SourceDataPk"  = mi.id
              AND tr."SourceTableName" = 'mat_inout'
        LEFT JOIN  test_item_result tir ON tr.id = tir."TestResult_id"

        -- ★ Standard2 파싱 결과 조인
        LEFT JOIN unitw uw ON uw.material_id = m.id

        WHERE m."Useyn" = '0'
          AND mi."InOut" IN ('in', 'return')
          AND mi."InoutDate" BETWEEN CAST(:srchStartDt AS date) AND CAST(:srchEndDt AS date)
          AND mi.spjangcd = :spjangcd
        """;

		if (!StringUtils.isEmpty(housePk))  sql += " AND sh.id = cast(:housePk as integer) ";
		if (!StringUtils.isEmpty(matType))  sql += " AND mg.\"MaterialType\" = :matType ";
		if (!StringUtils.isEmpty(matGrpPk)) sql += " AND m.\"MaterialGroup_id\" = cast(:matGrpPk as integer) ";
		if (!StringUtils.isEmpty(keyword))  sql += " AND upper(m.\"Name\") like concat('%%', upper(:keyword), '%%') ";

		sql += " ORDER BY \"InoutDate\" DESC, \"InoutTime\" DESC, mi.id DESC ";

		return this.sqlRunner.getRows(sql, param);
	}

	public List<Map<String, Object>> getMaterialInoutIssue(String srchStartDt, String srchEndDt, String housePk,
															  String matType, String matGrpPk, String keyword, String spjangcd) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("srchStartDt", srchStartDt);
		param.addValue("srchEndDt", srchEndDt);
		param.addValue("housePk", housePk);
		param.addValue("matType", matType);
		param.addValue("matGrpPk", matGrpPk);
		param.addValue("keyword", keyword);
		param.addValue("spjangcd", spjangcd);

		String sql = """
					select distinct mi.id as mio_pk
                    , fn_code_name('inout_type', mi."InOut") as inout
                    , mi."Material_id"
                    , mi."InputType" 
                    , mi."OutputType" 
                    , case when mi."InOut" = 'out' then fn_code_name('output_type', mi."OutputType") 
	                    when mi."InOut" = 'recall' then fn_code_name('recall_type', mi."OutputType")
	                    end as inout_type
                    , to_char(mi."InoutDate",'yyyy-mm-dd ') as "InoutDate"
                    , to_char(mi."InoutTime", 'hh24:mi') as "InoutTime"
                    , sh."Name" as "store_house_name"
                    , m."Code" as "material_code"
                    , m."Name" as "material_name"
                    , m."CurrentStock" 
                    , m."ValidDays"
                    , m."LotSize"
                    , m."PackingUnitQty"
                    , mi."StoreHouse_id"
                    , mih2."CurrentStock" as "HouseStock"
                    , m."SafetyStock" 
                    , coalesce(mi."InputQty", 0) as "InputQty"
                    , coalesce(mi."OutputQty", 0) as "OutputQty"
                    , u2."Name" as "unit_name"
                    , mi."Description" 
                    , fn_code_name('mat_type', mg."MaterialType") as material_type
                    --, coalesce(lot_cnt.lot_count,0) as lot_count
                    , (select count(ml."LotNumber") as lot_count 
                        from mat_lot ml 
                        where ml."SourceTableName" ='mat_inout' 
                        and ml."SourceDataPk" = mi.id
                        )  as lot_count 
                    , coalesce(mi."PotentialInputQty",0) as "potentialInputQty"
                    , fn_code_name('inout_state', mi."State" ) as "inout_state"
                    , var."StateName" as "state_name"
                    , tir."JudgeCode" as judge_code
                    , m."LotUseYN" as lot_use
                    from mat_inout mi 
                    inner join material m on mi."Material_id" = m.id
                    left join mat_grp mg on mg.id = m."MaterialGroup_id"
                    inner join store_house sh on mi."StoreHouse_id" = sh.id
                    left join unit u2 on m."Unit_id" = u2.id 
                    left join mat_in_house mih2 on mih2."Material_id"  = m.id
                    and mih2."StoreHouse_id" = mi."StoreHouse_id"
                    left join rela_data rd on mi.id = rd."DataPk2" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName2"  = 'mat_inout'
                    left join bundle_head bh on bh.id = rd."DataPk1" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName1"  = 'bundle_head'
                    left join v_appr_result var on var."SourceDataPk" = bh.id and var."SourceTableName" ='bundle_head'
                    left join test_result tr on tr."SourceDataPk"  = mi.id and tr."SourceTableName" = 'mat_inout'
                    left join test_item_result tir on tr.id = tir."TestResult_id"
                    where 1 = 1
                    and m."Useyn" = '0'
                    AND mi."InOut" IN ('out', 'recall')
                    and mi."OutputType" != 'disposal_out'
                    and mi."InoutDate" between cast(:srchStartDt as date) and cast(:srchEndDt as date)
                    and mi.spjangcd = :spjangcd
				""";

		if (StringUtils.isEmpty(housePk)==false) sql +=" and sh.id = cast(:housePk as Integer) ";
		if (StringUtils.isEmpty(matType)==false) sql +=" and mg.\"MaterialType\" = :matType ";
		if (StringUtils.isEmpty(matGrpPk)==false) sql +=" and m.\"MaterialGroup_id\" = cast(:matGrpPk as Integer) ";
		if (StringUtils.isEmpty(keyword)==false) sql +=" and upper(m.\"Name\") like concat('%%',upper(:keyword),'%%') ";

		sql += " order by \"InoutDate\" desc, \"InoutTime\" desc, mi.id desc ";

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);

		return items;
	}

	public List<Map<String, Object>> getMaterialInoutDisposal(String srchStartDt, String srchEndDt, String housePk,
													  String matType, String matGrpPk, String keyword, String spjangcd) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("srchStartDt", srchStartDt);
		param.addValue("srchEndDt", srchEndDt);
		param.addValue("housePk", housePk);
		param.addValue("matType", matType);
		param.addValue("matGrpPk", matGrpPk);
		param.addValue("keyword", keyword);
		param.addValue("spjangcd", spjangcd);

		String sql = """
					select distinct mi.id as mio_pk
                    , fn_code_name('inout_type', mi."InOut") as inout
                    , mi."Material_id"
                    , mi."InputType" 
                    , mi."OutputType" 
                    , case when mi."InOut" = 'in' then fn_code_name('input_type', mi."InputType") 
	                    when mi."InOut" = 'out' then fn_code_name('output_type', mi."OutputType") 
	                    when mi."InOut" = 'recall' then fn_code_name('recall_type', mi."OutputType")
	                    when mi."InOut" = 'return' then fn_code_name('return_type', mi."InputType")
	                    end as inout_type
                    , to_char(mi."InoutDate",'yyyy-mm-dd ') as "InoutDate"
                    , to_char(mi."InoutTime", 'hh24:mi') as "InoutTime"
                    , sh."Name" as "store_house_name"
                    , m."Code" as "material_code"
                    , m."Name" as "material_name"
                    , m."CurrentStock" 
                    , m."ValidDays"
                    , m."LotSize"
                    , m."PackingUnitQty"
                    , mi."StoreHouse_id"
                    , mih2."CurrentStock" as "HouseStock"
                    , m."SafetyStock" 
                    , coalesce(mi."InputQty", 0) as "InputQty"
                    , coalesce(mi."OutputQty", 0) as "OutputQty"
                    , u2."Name" as "unit_name"
                    , mi."Description" 
                    , fn_code_name('mat_type', mg."MaterialType") as material_type
                    --, coalesce(lot_cnt.lot_count,0) as lot_count
                    , (select count(ml."LotNumber") as lot_count 
                        from mat_lot ml 
                        where ml."SourceTableName" ='mat_inout' 
                        and ml."SourceDataPk" = mi.id
                        )  as lot_count 
                    , coalesce(mi."PotentialInputQty",0) as "potentialInputQty"
                    , fn_code_name('inout_state', mi."State" ) as "inout_state"
                    , var."StateName" as "state_name"
                    , tir."JudgeCode" as judge_code
                    , m."LotUseYN" as lot_use
                    from mat_inout mi 
                    inner join material m on mi."Material_id" = m.id
                    left join mat_grp mg on mg.id = m."MaterialGroup_id"
                    inner join store_house sh on mi."StoreHouse_id" = sh.id
                    left join unit u2 on m."Unit_id" = u2.id 
                    left join mat_in_house mih2 on mih2."Material_id"  = m.id
                    and mih2."StoreHouse_id" = mi."StoreHouse_id"
                    left join rela_data rd on mi.id = rd."DataPk2" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName2"  = 'mat_inout'
                    left join bundle_head bh on bh.id = rd."DataPk1" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName1"  = 'bundle_head'
                    left join v_appr_result var on var."SourceDataPk" = bh.id and var."SourceTableName" ='bundle_head'
                    left join test_result tr on tr."SourceDataPk"  = mi.id and tr."SourceTableName" = 'mat_inout'
                    left join test_item_result tir on tr.id = tir."TestResult_id"
                    where 1 = 1
                    and m."Useyn" = '0'
                    and mi."OutputType" = 'disposal_out'
                    and mi."InoutDate" between cast(:srchStartDt as date) and cast(:srchEndDt as date)
                    and mi.spjangcd = :spjangcd
				""";

		if (StringUtils.isEmpty(housePk)==false) sql +=" and sh.id = cast(:housePk as Integer) ";
		if (StringUtils.isEmpty(matType)==false) sql +=" and mg.\"MaterialType\" = :matType ";
		if (StringUtils.isEmpty(matGrpPk)==false) sql +=" and m.\"MaterialGroup_id\" = cast(:matGrpPk as Integer) ";
		if (StringUtils.isEmpty(keyword)==false) sql +=" and upper(m.\"Name\") like concat('%%',upper(:keyword),'%%') ";

		sql += " order by \"InoutDate\" desc, \"InoutTime\" desc, mi.id desc ";

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);

		return items;
	}

	public List<Map<String, Object>> getMaterialInoutDetail(Integer mio_pk) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mio_pk", mio_pk);

		String sql= """
			select distinct mi.id as mio_pk
                    , fn_code_name('inout_type', mi."InOut") as inout
                    , mi."InOut" as "inoutSelect"
					, mg."Name" as "cboMaterialGroupName"
					, mg."id" as "cboMaterialGroup"
					, COALESCE(NULLIF(mi."InputType", ''), NULLIF(mi."OutputType", '')) AS "InoutType"
					, to_char(mi."InoutDate", 'yyyy-mm-dd') || 'T' || to_char(mi."InoutTime", 'hh24:mi') as "inoutDate"
					, ROUND(
						  COALESCE(
							  NULLIF(mi."InputQty", 0),
							  NULLIF(mi."OutputQty", 0),
							  NULLIF(mi."PotentialInputQty", 0),
							  0
						  )::numeric,
						  6
					  ) AS "InoutQty"
					, mg."MaterialType" as "cboMaterialType"
                    , mi."Material_id"
                    , mi."InputType" 
                    , mi."OutputType" 
                    , case when mi."InOut" = 'in' then fn_code_name('input_type', mi."InputType") 
	                    when mi."InOut" = 'out' then fn_code_name('output_type', mi."OutputType") 
	                    when mi."InOut" = 'recall' then fn_code_name('recall_type', mi."OutputType")
	                    when mi."InOut" = 'return' then fn_code_name('return_type', mi."InputType")
	                    end as inout_type
                    , to_char(mi."InoutDate",'yyyy-mm-dd ') as "InoutDate"
                    , to_char(mi."InoutTime", 'hh24:mi') as "InoutTime"
                    , sh."Name" as "store_house_name"
                    , m."Code" as "Material_code"
                    , m."Name" as "Material_name"
                    , m."CurrentStock" 
                    , m."ValidDays"
                    , m."PackingUnitQty"
                    , mi."StoreHouse_id"
                    , mih2."CurrentStock" as "HouseStock"
                    , m."SafetyStock" 
                    , coalesce(mi."InputQty", 0) as "InputQty"
                    , coalesce(mi."OutputQty", 0) as "OutputQty"
                    , u2."Name" as "unit_name"
                    , mi."Description" 
                    , fn_code_name('mat_type', mg."MaterialType") as "cboMaterialTypeName"
                    , coalesce(mi."PotentialInputQty",0) as "potentialInputQty"
                    , fn_code_name('inout_state', mi."State" ) as "inout_state"
                    , var."StateName" as "state_name"
                    , tir."JudgeCode" as judge_code
                    , m."LotUseYN" as lot_use
                    , mi."Company_id" as "cboCompany"
                    , c."Name" as "CompanyName"
                    from mat_inout mi 
                    inner join material m on mi."Material_id" = m.id
                    left join mat_grp mg on mg.id = m."MaterialGroup_id"
                    inner join store_house sh on mi."StoreHouse_id" = sh.id
                    left join unit u2 on m."Unit_id" = u2.id 
                    left join mat_in_house mih2 on mih2."Material_id"  = m.id
                    and mih2."StoreHouse_id" = mi."StoreHouse_id"
                    left join rela_data rd on mi.id = rd."DataPk2" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName2"  = 'mat_inout'
                    left join bundle_head bh on bh.id = rd."DataPk1" and rd."RelationName" = 'mat_inout_test_result' and rd."TableName1"  = 'bundle_head'
                    left join v_appr_result var on var."SourceDataPk" = bh.id and var."SourceTableName" ='bundle_head'
                    left join test_result tr on tr."SourceDataPk"  = mi.id and tr."SourceTableName" = 'mat_inout'
                    left join test_item_result tir on tr.id = tir."TestResult_id"
                    left join company c on c.id= mi."Company_id"
                    where 1 = 1
                    and m."Useyn" = '0'
					and mi.id = :mio_pk
				""";

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);

		return items;
	}

	public List<Map<String, Object>> mioLotList(String mioId) {
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mioId", mioId);
		
		String sql = """
            select 
            mi.id as mio_id
            , ml.id as ml_id
            , ml."LotNumber" 
            , m."Name" as "MaterialName"
            , m."Code" as "MaterialCode" 
            , mg."Name" as "MaterialGroupName" 
            , m."MaterialGroup_id" 
            , m."Unit_id" 
            , m."ValidDays" 
            , u."Name" as "UnitName"
            , ml."InputQty"
            , m."Thickness"
            , m."Width"
            , m."Length"
            , to_char(ml."InputDateTime",'yyyy-MM-dd hh24:mi:ss') as "InputDateTime"
            , to_char(ml."EffectiveDate",'yyyy-MM-dd') as "EffectiveDate"
            , ml."Description"
            , ml."StoreHouse_id" as store_house_id
            from mat_lot ml  
                left join material m on m.id = ml."Material_id"
                left join mat_grp mg on mg.id = m."MaterialGroup_id" 
                left join unit u on u.id = m."Unit_id" 
                left join mat_inout mi on ml."SourceDataPk" = mi.id and ml."SourceTableName" ='mat_inout'
            where mi.id = cast(:mioId as Integer) 
			""";
		
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);
		return items;
	}

	public List<Map<String, Object>> mioTestList(Integer mioId, Integer testResultId) {
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mioId", mioId);
		param.addValue("testResultId", testResultId);
		
		String sql = """
				select ti.id, up."Name" as "CheckName", ti."ResultType" as "resultType", to_char(tir."TestDateTime", 'YYYY-MM-DD') as "testDate"
				, tir."JudgeCode", tir."CharResult" , ti."Name" as name ,tir."Char1" as result1
				, tr.id as "testResultId", tr."TestMaster_id" as "testMasterId"
				from test_item_result tir
				inner join test_result tr on tr.id = tir."TestResult_id"
				inner join test_item ti on tir."TestItem_id"  = ti.id 
				inner join user_profile up on tir."_creater_id"  = up."User_id" 
				where tr."SourceTableName" = 'mat_inout' and tr."SourceDataPk" = :mioId
				and tr.id= :testResultId
				order by ti.id
				""";
		
		
		
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);
		
		return items;
	}

	public Integer getTestMasterByItem(Integer mioId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mioId", mioId);

		String sql = """
                    SELECT tmm."TestMaster_id" AS testMasterId
                            FROM mat_inout mi
                            INNER JOIN test_mast_mat tmm ON mi."Material_id" = tmm."Material_id"
                            WHERE mi.id = :mioId
                            LIMIT 1
                """;

		List<Map<String, Object>> result = this.sqlRunner.getRows(sql, param);
		return result.isEmpty() ? null : (Integer) result.get(0).get("testMasterId");
	}

	public List<Map<String, Object>> prodTestListByTestMaster(Integer testMasterId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("testMasterId", testMasterId);

		String sql = """
                    SELECT tm.id AS testMasterId, ti.id, ti."Name" AS name, ti."ResultType" AS "resultType",
                           tim."SpecText" AS "specText", '' AS result1
                    FROM test_item_mast tim
                    INNER JOIN test_mast tm ON tim."TestMaster_id" = tm.id
                    INNER JOIN test_item ti ON tim."TestItem_id" = ti.id
                    WHERE tm.id = :testMasterId
                """;

		return this.sqlRunner.getRows(sql, param);
	}

	public List<Map<String, Object>> mioTestDefaultList() {
		
		String sql = """
				select ti.id,ti."Name" as name, ti."ResultType" as "resultType", '' as result1
				from test_item ti
				inner join test_method tm on ti."TestMethod_id"  = tm.id 
				where tm."Code"  = 'inout_test'
				order by ti.id
			    """;
		
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, null);
		
		return items;
	}

	public Map<String, Object> getEffectDate(Integer mioId) {
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mioId", mioId);
		
		String sql = """
				select (case when mi."EffectiveDate" = null then null else to_char(mi."EffectiveDate", 'YYYY-MM-DD') end)  as "EffectiveDate"
				from mat_inout mi 
				inner join material m on m.id = mi."Material_id"
				where mi.id = :mioId
				""";
		
		Map<String,Object> items = this.sqlRunner.getRow(sql, param);
		
		return items;
	}

	public List<Map<String, Object>> getBaljuList(Timestamp start, Timestamp end, String spjangcd) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		dicParam.addValue("spjangcd", spjangcd);

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
          , COALESCE(mi."SujuQty2", 0) AS "SujuQty2"
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
          LEFT JOIN (
			   SELECT
				   "SourceDataPk",
				   SUM("InputQty") AS "SujuQty2"
			   FROM mat_inout
			   WHERE "SourceTableName" = 'balju'
				 AND COALESCE("_status", 'a') = 'a'
				 AND "InOut" = 'in'
			   GROUP BY "SourceDataPk"
		   ) mi ON mi."SourceDataPk" = b.id
          where 1 = 1
          and b."JumunDate" between :start and :end 
          AND COALESCE(mi."SujuQty2", 0) < b."SujuQty"
          and b.spjangcd = :spjangcd
          and "State" != 'force_completion'
			order by b."JumunDate" desc,  m."Name"
			""";

//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
		List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

		return itmes;
	}

	public List<Map<String, Object>> getBaljuInList(Timestamp start, Timestamp end, String spjangcd, Integer choComp, String keyword) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		dicParam.addValue("spjangcd", spjangcd);
		dicParam.addValue("choComp", choComp);
		dicParam.addValue("keyword", keyword);

		String sql = """
		WITH s2 AS (  -- Standard2 숫자 파싱(콤마 → 점 치환)
			SELECT
				m.id AS material_id,
				CASE
					WHEN m."Standard2" IS NULL OR btrim(m."Standard2")='' THEN NULL
					ELSE NULLIF(
								 regexp_replace(replace(m."Standard2", ',', '.'), '[^0-9\\.]', '', 'g'),
								 ''
							 )::numeric
				END AS s2_num
			FROM material m
		),
		unit_info AS (  -- material ↔ unit: 낱개 여부
			SELECT
				m.id AS material_id,
				u."Name" AS unit_name,
				COALESCE(NULLIF(upper(u."PieceYN"),''), 'N') AS piece_yn
			FROM material m
			LEFT JOIN unit u ON u.id = m."Unit_id"
		),
		unitw AS (  -- 낱개품목만 단위중량 사용
			SELECT
				s2.material_id,
				ui.unit_name,
				ui.piece_yn,
				s2.s2_num AS unit_weight_kg_any,                    -- ★ 항상 사용 가능한 Standard2(숫자)
				CASE WHEN ui.piece_yn = 'Y' THEN s2.s2_num END AS unit_weight_kg_per_ea
			FROM s2
			JOIN unit_info ui ON ui.material_id = s2.material_id
		)
		-- 기존 쿼리의 FROM 이하에 조인 추가 후, SELECT에서 SujuQty2를 '개수'로 환산
		SELECT
			b.id,
			b."JumunNumber",
			b."Material_id" AS "Material_id",
			mg."Name" AS "MaterialGroupName",
			mg.id AS "MaterialGroup_id",
			fn_code_name('mat_type', mg."MaterialType") AS "MaterialTypeName",
			m.id AS "Material_id",
			m."Code" AS product_code,
			m."Name" AS product_name,
			u."Name" AS unit,
			b."SujuQty" AS "SujuQty",
			to_char(b."JumunDate", 'yyyy-mm-dd') AS "JumunDate",
			to_char(b."DueDate", 'yyyy-mm-dd') AS "DueDate",
			b."CompanyName",
			b."Company_id",
			b."SujuType",
			fn_code_name('Balju_type', b."SujuType") AS "BaljuTypeName",
			to_char(b."ProductionPlanDate", 'yyyy-mm-dd') AS production_plan_date,
			to_char(b."ShipmentPlanDate", 'yyyy-mm-dd') AS shiment_plan_date,
			b."Description",
			b."AvailableStock" AS "AvailableStock",
			b."ReservationStock" AS "ReservationStock",
			-- ▼ 입고 중량/반품 중량 (그대로)
			COALESCE(mi."SujuQty2", 0) AS "SujuQty2_kg",
			CASE
			WHEN uw.piece_yn = 'Y'
				THEN COALESCE(mi."SujuQty2",0) / NULLIF(uw.unit_weight_kg_per_ea, 0)   -- 낱개단위: EA당 kg로 나눔
			ELSE
				COALESCE(mi."SujuQty2",0) / NULLIF(uw.unit_weight_kg_any, 0)           -- 낱개가 아니어도 Standard2로 개수 환산
		END AS "SujuQty2_ea",
			fn_code_name('balju_state', b."State") AS "StateName",
			fn_code_name('shipment_state', b."ShipmentState") AS "ShipmentStateName",
			b."State",
			to_char(b."_created", 'yyyy-mm-dd') AS create_date,
			CASE b."PlanTableName"
				WHEN 'prod_week_term' THEN '주간계획'
				WHEN 'bundle_head'    THEN '임의계획'
				ELSE b."PlanTableName"
			END AS plan_state
		FROM balju b
		JOIN material m  ON m.id = b."Material_id"
		JOIN mat_grp mg  ON mg.id = m."MaterialGroup_id"
		LEFT JOIN unit u ON m."Unit_id" = u.id
		LEFT JOIN company c ON c.id = b."Company_id"
		-- 입고/반품 집계
		LEFT JOIN (
			SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
			FROM mat_inout
			WHERE "SourceTableName" = 'balju'
				AND COALESCE("_status",'a') = 'a'
				AND "InOut" = 'in'
			GROUP BY "SourceDataPk"
		) mi ON mi."SourceDataPk" = b.id
		LEFT JOIN (
			SELECT "SourceDataPk", SUM("InputQty") AS "ReturnQty"
			FROM mat_inout
			WHERE "SourceTableName" = 'balju'
				AND COALESCE("_status",'a') = 'a'
				AND "InOut" = 'return'
			GROUP BY "SourceDataPk"
		) mi_return ON mi_return."SourceDataPk" = b.id
		-- ★ 단위/단위중량 CTE 조인
		LEFT JOIN unitw uw ON uw.material_id = m.id
		WHERE b."JumunDate" BETWEEN :start AND :end
			AND b.spjangcd = :spjangcd
			AND COALESCE(mi."SujuQty2", 0) > 0
         """;

		if (StringUtils.isEmpty(keyword)==false) sql +=" and upper(m.\"Name\") like concat('%%',upper(:keyword),'%%') ";
		if(choComp != null) {
			sql += """ 
					and b."Company_id" = :choComp
					""";
		}

		sql += " order by b.\"JumunDate\" desc,  m.\"Name\" ";

//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
		List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

		return itmes;
	}

	public Map<String, Object> getUnitId(String unit_name) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("unit_name", unit_name);

		String sql= """
				SELECT id, "Name" AS name
				FROM unit
				WHERE "Name" = :unit_name
				LIMIT 1
				""";

		log.info("UnitId SQL: {}", sql);
		log.info("SQL Parameters: {}", param.getValues());
		return this.sqlRunner.getRow(sql, param);
	}

	public BigDecimal toBaseQty(Integer mat_pk, BigDecimal qty, Integer inputUnitId, String inputUnitName ) {

		MapSqlParameterSource p = new MapSqlParameterSource()
				.addValue("mat_pk", mat_pk)
				.addValue("qty", qty)
				.addValue("inputUnitId", inputUnitId)
				.addValue("inputUnitName", inputUnitName);

		String sql = """
				WITH m0 AS (
				          SELECT m.id,
				                 m."Unit_id"                              AS base_unit_id,
				                 bu."Name"                                AS base_unit_name,
				                 m."Standard2"
				          FROM public.material m
				          JOIN public.unit bu ON bu.id = m."Unit_id"
				          WHERE m.id = :mat_pk
				        ),
				        -- Standard2 문자열 파싱(숫자만 뽑고 소수점 통일) + 단위 인식 → kg로 환산
				        s2 AS (
				          SELECT
				            m0.*,
				            lower(COALESCE(m0."Standard2", '')) AS s2_raw,
				            NULLIF(
				              regexp_replace(
				                replace(lower(COALESCE(m0."Standard2", '')), ',', '.'),
				                '[^0-9.\\\\-]', '', 'g'
				              ),
				              ''
				            )::numeric AS s2_num
				          FROM m0
				        ),
				        m AS (
				          SELECT
				            id, base_unit_id, base_unit_name,
				            CASE
				              WHEN s2_num IS NULL THEN NULL
				              WHEN s2_raw LIKE '%t%'    THEN s2_num * 1000      -- ton → kg
				              WHEN s2_raw LIKE '%kg%'   THEN s2_num             -- kg
				              WHEN s2_raw ~ '(^|[^k])g' THEN s2_num / 1000      -- g → kg (kg와 구분)
				              ELSE s2_num                                       -- 단위 없으면 kg 가정
				            END AS unit_weight_kg
				          FROM s2
				        ),
				        u AS (
				          /* 입력단위 결정: id → name → 'EA' → 기본단위 */
				          SELECT COALESCE(
				                   :inputUnitId,
				                   (SELECT id FROM public.unit
				                      WHERE lower("Name") = lower(NULLIF(:inputUnitName,'')) LIMIT 1),
				                   (SELECT id FROM public.unit WHERE "Name"='EA' LIMIT 1),
				                   (SELECT base_unit_id FROM m)
				                 ) AS from_unit_id
				        ),
				        f AS (
				          SELECT
				            CASE
				              WHEN u.from_unit_id = m.base_unit_id THEN 1::numeric
				              /* 개수 단위(EA/BOX/pack/ROLL/…): PieceYN='Y' → Standard2(kg/ea) 사용 */
				              WHEN lower(m.base_unit_name) = 'kg' AND fu."PieceYN" = 'Y' THEN m.unit_weight_kg
				              /* 질량 단위: g/t 매핑 */
				              WHEN lower(m.base_unit_name) = 'kg' AND lower(fu."Name") = 'g' THEN 0.001::numeric
				              WHEN lower(m.base_unit_name) = 'kg' AND lower(fu."Name") = 't' THEN 1000::numeric
				              WHEN lower(m.base_unit_name) = 'kg' AND lower(fu."Name") = 'kg' THEN 1::numeric
				              ELSE NULL
				            END AS factor
				          FROM m
				          JOIN u ON true
				          JOIN public.unit fu ON fu.id = u.from_unit_id
				        )
				        SELECT
				          CASE
				            WHEN (SELECT factor FROM f) IS NULL THEN NULL
				            ELSE (:qty * (SELECT factor FROM f))::numeric(18,6)
				          END AS base_qty
    """;
		log.info("toBaseQty SQL: {}", sql);
		log.info("SQL Parameters: {}", p.getValues());
		Map<String,Object> row = this.sqlRunner.getRow(sql, p);
		return row != null ? (BigDecimal) row.get("base_qty") : BigDecimal.ZERO.setScale(6);
	}
}
