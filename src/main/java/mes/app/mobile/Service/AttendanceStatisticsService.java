package mes.app.mobile.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AttendanceStatisticsService {
    @Autowired
    SqlRunner sqlRunner;

    // 휴가통계 조회
    public List<Map<String, Object>> getUserInfo(String user) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();


        String sql = """
                
        		""";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
}
