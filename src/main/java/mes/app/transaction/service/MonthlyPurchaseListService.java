package mes.app.transaction.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MonthlyPurchaseListService {
    @Autowired
    SqlRunner sqlRunner;

    // 월별 매입 현황 리스트 조회
    // 매입
    public List<Map<String, Object>> getPurchaseList(String cboYear, Integer cboCompany) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("cboYear", cboYear);
        paramMap.addValue("cboCompany", cboCompany);

        String data_column = "";

        String data_year = cboYear;

        paramMap.addValue("date_form",data_year+"-01-01" );
        paramMap.addValue("date_to",data_year+"-12-31" );

        data_column = "A.defect_pro";

        String sql = """
				 with A as 
	            (
	            select i. *,
	                c."Name",
	            , fn_code_name('mat_type', mg."MaterialType") as mat_type_name
	            , i.misgubun
	            , m."Code" as mat_code
	            , m."Name" as mat_name
                , u."Name" as unit_name
	            , EXTRACT(MONTH FROM TO_DATE(i.misdate, 'YYYYMMDD')) AS data_month
	            , coalesce(sum(jr."DefectQty"),0) as defect_qty
                , sum(jr."DefectQty") * m."UnitPrice" as defect_money
	            ,(coalesce(sum(jr."GoodQty"),0) + coalesce(sum(jr."DefectQty"),0)) as prod_sum
	            , 100 * coalesce(sum(jr."DefectQty"),0) / nullif(coalesce(sum(jr."GoodQty"),0) + coalesce(sum(jr."DefectQty"),0),0 ) as defect_pro
	            from tb_invoicement i
	            left join company c on i.cltcd = c.id
	            where i.misdate between cast(:date_form as date) and cast(:date_to as date)
                and jr."State" = 'finished'
				""";

        if(cboCompany != null) {
            sql += """
					and c."Code" = :cboCompany
					""";
        }
        sql += """
				group by jr."Material_id", mg."MaterialType", mg."Name" , m."Name" , m."Code", m."UnitPrice"
                , u."Name"
                , extract (month from jr."ProductionDate") 
	            )
	            select A.mat_pk, A.mat_type_name, A.mat_grp_name, A.mat_code, A.mat_name
                , A.unit_name
                , round((100 * sum(defect_qty) / nullif(sum(prod_sum),0))::decimal,3) as year_defect_pro 
                , sum(defect_qty) as year_defect_qty 
                , sum(defect_money) as year_defect_money
                , sum(prod_sum) as prod_Sum
				""";

        for(int i=1; i<13; i++) {
            sql += ", round(min(case when A.data_month = "+i+" then "+data_column+" ::decimal end),3)::float as mon_"+i+"  ";
        }



        sql += """ 
				from A 
				group by A.mat_pk, A.mat_type_name, A.mat_grp_name, A.mat_code, A.mat_name, A.unit_name
				""";

        sql += """
				order by A.mat_type_name, A.mat_grp_name, A.mat_name	  
				""";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
        return items;
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

