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

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("cboYear", cboYear);
        dicParam.addValue("cboCompany", cboCompany);

        String sql = """
                
        		""";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

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

