package mes.app.test.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TestProcessService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getProdResult(String dateFrom, String dateTo, String shiftCode,
                                                   String workcenterPk, String matType, String isIncludeComp, String spjangcd) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("dateFrom", dateFrom);
        dicParam.addValue("dateTo", dateTo);
        dicParam.addValue("shiftCode", shiftCode);
        dicParam.addValue("workcenterPk", workcenterPk);
        dicParam.addValue("matType", matType);
        dicParam.addValue("isIncludeComp", isIncludeComp);
        dicParam.addValue("spjangcd", spjangcd);

        String sql = """
                select jr.id
                 , jr."WorkOrderNumber" as order_num
                 , to_char(jr."ProductionDate", 'yyyy-mm-dd') as prod_date
                 , jr."LotNumber" as lot_num
                 , to_char(jr."StartTime", 'hh24:mi') as start_time
                 , to_char(jr."EndTime", 'hh24:mi') as end_time
                 , jr."WorkIndex" as work_idx
                 , fn_code_name('job_state', jr."State") as job_state
                 , jr."State" as state
                 , jr."WorkerCount" as worker_count
                 , m.id as mat_pk
                 , m."Code" as mat_code
                 , m."Name" as mat_name
                 , fn_code_name('mat_type', mg."MaterialType") as mat_type
                 , m."LotSize" as lot_size
                 , m."Weight" as weight
                 , jr."Description" as description
                 , jr."OrderQty" as order_qty
                 , jr."GoodQty" as good_qty
                 , jr."DefectQty" as defect_qty
                 , jr."LossQty" as loss_qty
                 , jr."ScrapQty" as scrap_qty
                 , to_char(jr."ProductionDate"+ m."ValidDays", 'yyyy-mm-dd') as "ValidDays"
                 , pt.remark
                 , pt.workcenter_name
                 , pt."validate"
                 from job_res jr
                 left join material m on m.id = jr."Material_id"
                 left join mat_grp mg on mg.id = m."MaterialGroup_id"
                 left join process_test pt on pt."job_res_id" = jr."id"
                 where jr."ProductionDate" between cast(:dateFrom as date) and cast(:dateTo as date)
                 and jr.spjangcd = :spjangcd
                 and jr."State" = 'finished'
                """;
//        if (StringUtils.isEmpty(matType) == false) sql += "and mg.\"MaterialType\" = :matType ";
//        if (!shiftCode.equals("")) sql += " and jr.\"ShiftCode\" = :shiftCode ";
//        if (!workcenterPk.equals("")) sql += " and jr.\"WorkCenter_id\" = cast(:workcenterPk as Integer) ";

        sql += " order by jr.\"ProductionDate\", jr.\"WorkOrderNumber\", jr.id ";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public Map<String, Object> getTestMethodDetail(int jrId){

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("job_res_id", jrId);
        String sql = """
            select pt.*
                , to_char(jr."ProductionDate", 'yyyy-mm-dd') as prod_date
                , jr."WorkOrderNumber" as order_num
            from process_test pt
            left join job_res jr on jr.id = pt.job_res_id
            where pt.job_res_id = :job_res_id
		""";

        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        if (item != null && item.containsKey("production_date")) {
            String rawDate = (String) item.get("production_date");
            item.put("production_date", rawDate.substring(0,4) + "-"
                                        + rawDate.substring(4,6) + "-"
                                        + rawDate.substring(6,8));
        }
        return item;
    }

    public Map<String, Object> findJobResData(int jrId){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("jrId", jrId);
        String sql = """
            select wc."Name" as workcenter_name
                  , m."Name" as mat_name
                  , jr."WorkOrderNumber" as order_num
                  , jr.id as job_res_id
                  -- , '식품유형'
                  , jr."ProductionPlanDate" as production_date
                  , m."ValidDays" as validate
                  , m."storage_method"
                  , m."Standard1" as packaging_spec
                  , ROUND(SUM(mpi."RequestQty")::numeric, 3) as mixing_amount
                  , m."packaging_mat"
                  , mg."Name" as food_type
            from job_res jr
            left join material m on m.id = jr."Material_id"
            left join work_center wc on jr."FirstWorkCenter_id" = wc.id
            inner join mat_proc_input_req mpir on mpir.id = jr."MaterialProcessInputRequest_id"
            inner join mat_proc_input mpi on mpi."MaterialProcessInputRequest_id" = mpir.id
            left join mat_grp mg on m."MaterialGroup_id" = mg.id
            where jr.id = :jrId
            -- and jr.spjangcd = :spjangcd
            group by
                 wc."Name",
                 jr."ProductionPlanDate",
                 m."ValidDays",
                 m."Standard1",
                 jr."WorkOrderNumber",
                 jr.id,
                 m."Name",
                 m."storage_method",
                 m."packaging_mat",
                 mg."Name"
		""";
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        return item;
    }


}
