package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MonthlyPurchaseListService {
    @Autowired
    SqlRunner sqlRunner;

    /*// 월별 매입 현황 리스트 조회
    public List<Map<String, Object>> getPurchaseList(String cboYear, Integer cboCompany) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("cboYear", cboYear);
        paramMap.addValue("cboCompany", cboCompany);

        String data_column = "";

        String data_year = cboYear;

        paramMap.addValue("date_from",data_year+"-01-01" );
        paramMap.addValue("date_to",data_year+"-12-31" );

        data_column = "A.defect_money";

        String sql = """
				with A as 
	            (
                    select
                        i.cltcd
                        , c."Name"
                        , i.icerdeptnm
                        , i.misgubun
                        , i.icerpernm
                        , EXTRACT(MONTH FROM TO_DATE(i.misdate, 'YYYYMMDD')) AS data_month
                         , sum(i.totalamt) as defect_money
                        from tb_invoicement i
                        left join company c on i.cltcd = c.id
                        where TO_DATE(i.misdate, 'YYYYMMDD') between cast(:date_form as date) and cast(:date_to as date)
				""";

        if(cboCompany != null) {
            sql += """
					and c."Code" = :cboCompany
					""";
        }
        sql += """
                group by i.cltcd, c."Name", i.icerdeptnm, i.misgubun , i.icerpernm,
                    EXTRACT(MONTH FROM TO_DATE(i.misdate, 'YYYYMMDD'))
                    )
                select A."Name", A.icerdeptnm, A.misgubun, A.icerpernm
                , sum(defect_money) as year_defect_money
				""";

        for(int i=1; i<13; i++) {
            sql += ", round(min(case when A.data_month = "+i+" then "+data_column+" ::decimal end),3)::float as mon_"+i+"  ";
        }



        sql += """ 
				from A 
				group by A."Name", A.icerdeptnm, A.misgubun, A.icerpernm
				""";

//        sql += """
//				order by A.mat_type_name, A.mat_grp_name, A.mat_name
//				""";

      log.info("월별 매출현황 (입금) SQL: {}", sql);
      log.info("SQL Parameters: {}", paramMap.getValues());
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
        return items;
    }*/
    // 월별 매입 현황 리스트 조회
    public List<Map<String, Object>> getPurchaseList(String cboYear, Integer cboCompany, String spjangcd) {
      MapSqlParameterSource paramMap = new MapSqlParameterSource();
      paramMap.addValue("cboYear", cboYear);
      paramMap.addValue("cboCompany", cboCompany);
      paramMap.addValue("spjangcd", spjangcd);

      String data_column = "A.defect_money";
      String data_year = cboYear;

      // 날짜 파라미터 설정 (주의: SQL에서는 :date_from, :date_to 사용)
      paramMap.addValue("date_from", data_year + "-01-01");
      paramMap.addValue("date_to", data_year + "-12-31");

      // SQL 시작
      StringBuilder sql = new StringBuilder();
      sql.append("""
        WITH A AS (
            SELECT
                i.cltcd,
                c."Name",
                i.icerdeptnm,
                i.misgubun,
                i.icerpernm,
                EXTRACT(MONTH FROM TO_DATE(i.misdate, 'YYYYMMDD')) AS data_month,
                SUM(i.totalamt) AS defect_money
            FROM tb_invoicement i
            LEFT JOIN company c ON i.cltcd = c.id and i.spjangcd = c.spjangcd
            WHERE TO_DATE(i.misdate, 'YYYYMMDD') BETWEEN CAST(:date_from AS date) AND CAST(:date_to AS date)
            and i.spjangcd = :spjangcd
        """);

      // 회사 조건이 있을 경우 필터 추가
      if (cboCompany != null) {
        sql.append(" AND c.\"Code\" = :cboCompany\n");
      }

      // CTE 그룹핑 종료
      sql.append("""
            GROUP BY i.cltcd, c."Name", i.icerdeptnm, i.misgubun, i.icerpernm,
                     EXTRACT(MONTH FROM TO_DATE(i.misdate, 'YYYYMMDD'))
        )
        SELECT
            A."Name",
            A.icerdeptnm,
            A.misgubun,
            A.icerpernm,
            SUM(defect_money) AS year_defect_money
        """);

      // 월별 컬럼 동적 생성 (mon_1 ~ mon_12)
      for (int i = 1; i <= 12; i++) {
        sql.append(", ROUND(MIN(CASE WHEN A.data_month = ").append(i)
            .append(" THEN ").append(data_column)
            .append("::DECIMAL END), 3)::FLOAT AS mon_").append(i).append("\n");
      }

      // GROUP BY 절 마무리
      sql.append("""
        FROM A
        GROUP BY A."Name", A.icerdeptnm, A.misgubun, A.icerpernm
        """);

//      log.info("월별 매출현황 (입금) SQL: {}", sql);
//      log.info("SQL Parameters: {}", paramMap.getValues());

      return this.sqlRunner.getRows(sql.toString(), paramMap);
    }


  // 지급
    public List<Map<String, Object>> getAccoutList(String cboYear, Integer cboCompany) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("cboYear", cboYear);
        dicParam.addValue("cboCompany", cboCompany);

        String sql = """
                
        		""";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 미지급
    public List<Map<String, Object>> getunAccoutList(String cboYear, Integer cboCompany) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("cboYear", cboYear);
        dicParam.addValue("cboCompany", cboCompany);

        String sql = """
                
        		""";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}

