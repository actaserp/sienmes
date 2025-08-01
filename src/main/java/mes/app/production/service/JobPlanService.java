package mes.app.production.service;

import mes.domain.entity.Suju;
import mes.domain.repository.JobPlanHeadRepository;
import mes.domain.repository.JobPlanRepository;
import mes.domain.repository.SujuRepository;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Service
public class JobPlanService {

	@Autowired
	SqlRunner sqlRunner;
	
	@Autowired
	JobPlanRepository jobPlanRepository;

	@Autowired
	JobPlanHeadRepository jobPlanHeadRepository;
	
	
	// 수주 내역 조회 
	public List<Map<String, Object>> getList(String date_kind, String start, String end, String spjangcd) {
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("date_kind", date_kind);
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		dicParam.addValue("spjangcd", spjangcd);
		
		String sql = """
				select * from job_plan_head
			""";

		List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);
		
		return itmes;
	}
	
	// 수주 상세정보 조회
	public Map<String, Object> getDetail(int id) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", id);

		String sql = """ 
			
		""";

		String detailSql = """ 
			
				 
		""";

		Map<String, Object> head = this.sqlRunner.getRow(sql, paramMap);
		List<Map<String, Object>> planList = this.sqlRunner.getRows(detailSql, paramMap);

		head.put("planList", planList);
		
		return head;
	}


}
