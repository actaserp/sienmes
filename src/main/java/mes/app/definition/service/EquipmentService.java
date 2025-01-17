package mes.app.definition.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import mes.domain.services.SqlRunner;

@Service
public class EquipmentService {
	
	@Autowired
	SqlRunner sqlRunner;
	
	// 설비 목록 조회
	public List<Map<String, Object>> getEquipmentList(Integer group, Integer workcenter, String keyword){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("group_id", group);
        dicParam.addValue("workcenter_id", workcenter);
        dicParam.addValue("keyword", keyword);
        
        String sql = """
			select e.id
             , e."Code"
             , e."Name"
             , e."Description"
             , e."Maker"
             , e."Model"
             , e."ManageNumber"
             , e."SerialNumber"
             , e."SupplierName"
             , e."ProductionYear"
             , to_char(e."PurchaseDate",'yyyy-mm-dd') as "PurchaseDate"
             , e."Manager"
             , e."PurchaseCost" 
             , e."ServiceCharger"
             , e."InstallDate"
             , e."DisposalDate"
             , e."OperationRateYN"
             , e."Status"
             , e."EquipmentGroup_id"
             , eg."Name" as group_name
             , e."WorkCenter_id"
             , wc."Name" as workcenter_name
      		 , e."AttentionRemark"
             , e."PowerWatt"
             , e."Voltage"
             , e."Usage"
             , e."Inputdate" as "InputDate"
             , d."Name"  as "DepartName"
             , e."ASTelNumber"
             , to_char(e._created ,'yyyy-mm-dd hh24:mi') as _created 
            from equ e
            left join equ_grp eg on e."EquipmentGroup_id" =eg.id 
            left join work_center wc  on wc.id = e."WorkCenter_id" 
            left join depart d on d.id = e."Depart_id" 
            where 1 = 1
		    """;
        if (group != null) sql +=" and e.\"EquipmentGroup_id\"= :group_id ";
        if (workcenter != null) sql +=" and e.\"WorkCenter_id\"= :workcenter_id ";
        if (StringUtils.hasText(keyword)) sql +=" and upper(e.\"Name\") like concat('%%', :keyword,'%%') ";
        
        sql += " order by e.id desc ";
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        
        return items;
	}
	
	// 설비 상세정보 조회
	public Map<String, Object> getEquipmentpDetail(int id){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("id", id);
        
        String sql = """
			select e.id
             , e."Code"
             , e."Name"
             , e."Description"
             , e."Maker"
             , e."Model"
             , e."ManageNumber"
             , e."SerialNumber"
             , e."SupplierName"
             , e."ProductionYear"
             , to_char(e."PurchaseDate",'yyyy-mm-dd') as "PurchaseDate"
             , e."Manager"
             , e."PurchaseCost" 
             , e."ServiceCharger"
             , e."InstallDate"
             , e."DisposalDate"
             , e."OperationRateYN"
             , e."Status"
             , e."EquipmentGroup_id"
             , eg."Name" as group_name
             , e."WorkCenter_id"
             , wc."Name" as workcenter_name
             , e."AttentionRemark"
             , e."PowerWatt"
             , e."Voltage"
             , e."Usage"
             , e."Inputdate" as "InputDate"
             , e."Depart_id"
             , e."ASTelNumber"
             , to_char(e._created ,'yyyy-mm-dd hh24:mi') as _created 
            from equ e
              left join equ_grp eg on e."EquipmentGroup_id" =eg.id 
              left join work_center wc  on wc.id = e."WorkCenter_id"   
            where e.id = :id
		    """;
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        
        return item;
	}

	public List<Map<String, Object>> getEquipmentStopList(String dateFrom, String dateTo, String equipment) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom + " 00:00:00");
		paramMap.addValue("dateTo", dateTo + " 23:59:59");
		paramMap.addValue("equipment", equipment);

		String sql = """
            select  er.id
            , to_char(er."StartDate", 'yyyy-mm-dd') as start_date
            , to_char(er."EndDate", 'yyyy-mm-dd') as end_date           
	        , e."Name"
	        , e."Code"
	        , er."StartDate"
	        , to_char(er."StartDate",'hh24:mi') as "StartTime"
	        , er."EndDate"
	        , to_char(er."EndDate",'hh24:mi') as "EndTime"
	        , EXTRACT(day from (er."EndDate" - er."StartDate")) * 60 * 24
	            + EXTRACT(hour from (er."EndDate" - er."StartDate")) * 60 
	            + EXTRACT(min from ("EndDate" - "StartDate")) as "GapTime"
            , er."WorkOrderNumber" 
	        , er."Equipment_id" 
	        , er."RunState" 
            , sc."StopCauseName" 
            , er."Description" 
            from equ e 
            inner join equ_run er on e.id = er."Equipment_id"
            left join stop_cause sc on sc.id = er."StopCause_id"
            where er."StartDate" >= cast(:dateFrom as timestamp) and er."EndDate" <= cast(:dateTo as timestamp)
			""";
		
		if (StringUtils.hasText(equipment)) {
			sql += "  and er.\"Equipment_id\" = :equipment::INTEGER ";
		}
		

        sql += """
		        and er."RunState" = 'X'
		        order by e."Name", er."StartDate", er."EndDate"
        	   """;
        
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}

	public Map<String, Object> getEquipmentStopInfo(Integer id, String runType) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", id);
		
		String sql = """
	                select er.id
	                , to_char(er."StartDate", 'yyyy-mm-dd') as start_date
	                , to_char(er."EndDate", 'yyyy-mm-dd') as end_date   
		            , e."Name"
		            , e."Code"
		            , er."StartDate"
		            , to_char(er."StartDate",'hh24:mi') as "StartTime"
		            , er."EndDate"
		            , to_char(er."EndDate",'hh24:mi') as "EndTime"
		            , EXTRACT(day from (er."EndDate" - er."StartDate")) * 60 * 24
		                + EXTRACT(hour from (er."EndDate" - er."StartDate")) * 60 
		                + EXTRACT(min from ("EndDate" - "StartDate")) as "GapTime"
	                , er."WorkOrderNumber" 
		            , er."Equipment_id" 
		            , er."Description" 
		            , er."RunState" 
	                , er."StopCause_id"
	                , sc."StopCauseName" 
	                from equ e
	                inner join equ_run er on e.id = er."Equipment_id"         
	                left join stop_cause sc on sc.id = er."StopCause_id"
	                where er.id = :id
	                and er."RunState" = 'X'
					""";

		Map<String, Object> items = this.sqlRunner.getRow(sql, paramMap);
		
		return items;
	}

}
