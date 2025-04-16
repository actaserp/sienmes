package mes.app.summary.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import mes.domain.services.SqlRunner;

@Service
public class ProductionDefectTypeMonthService {
	
	@Autowired
	SqlRunner sqlRunner;
	

	public List<Map<String, Object>> getList(String cboYear, Integer cboMatType, Integer cboMatGrpPk,
											 Integer txtProductId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("cboYear", cboYear);
		paramMap.addValue("cboMatType", cboMatType);
		paramMap.addValue("cboMatGrpPk", cboMatGrpPk);
		paramMap.addValue("txtProductId", txtProductId);
		
		//String data_column = "";
		
		String data_year = cboYear;
		
		paramMap.addValue("date_form",data_year+"-01-01" );
		paramMap.addValue("date_to",data_year+"-12-31" );
		
		
		String sql = """
				 with A as 
		        (
		            select dt.id as defect_pk, dt."Name" as defect_type
		            , extract (month from jr."ProductionDate") as data_month
		            --, m.id as mat_pk
		            --, m."Code" as mat_code
		            --, m."Name" as mat_name
		            --, sum(jrd."DefectQty") as defect_qty
		            , sum(jrd."DefectQty")::decimal as defect_qty
		            from job_res jr 
		            inner join job_res_defect jrd on jrd."JobResponse_id" = jr.id
		            inner join defect_type dt on dt.id = jrd."DefectType_id"
	       	        inner join material m on m.id = jr."Material_id"
	                left join mat_grp mg on mg.id = m."MaterialGroup_id"		
		            where jr."ProductionDate" between cast(:date_form as date) and cast(:date_to as date) 
                    and jr."State" = 'finished'
				""";
		
		if(cboMatType != null) {
			sql += """
					and dt."id" = :cboMatType
					""";
		}
		
		if(cboMatGrpPk != null) {
			sql += """
					and mg.id = :cboMatGrpPk
					""";
		}

		if(txtProductId != null) {
			sql += """
					and m.id = :txtProductId
					""";
		}

		sql += """
				 group by dt.id, dt."Name", extract (month from jr."ProductionDate") 
		        ), B as (
		            select sum(A.defect_qty) as year_defect 
				""";
		
		for(int i=1; i<13; i++) {
			sql += ", sum(case when A.data_month = "+ i +" then A.defect_qty end) as month_"+i+"  ";
		}
		
		
		sql += """
				from A 
		    )
		    select A.defect_pk, A.defect_type
		    , sum(A.defect_qty) as year_count
		    , round(sum(A.defect_qty) / nullif(B.year_defect,0) * 100,1) as year_portion
				""";
		
		for(int i=1; i<13; i++) {
			sql += ", min(case when A.data_month = " + i + " then A.defect_qty end) as count_" + i + " ";
			sql +=", round(min(case when A.data_month = "+i+" then A.defect_qty / nullif(B.month_"+i+", 0) * 100 end)::decimal,1) as portion_"+i+"  ";
		}
		
		sql +="""
				from A 
		    inner join B on 1 = 1
		    group by A.defect_pk, A.defect_type, B.year_defect
				""";
		
		
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}

}
