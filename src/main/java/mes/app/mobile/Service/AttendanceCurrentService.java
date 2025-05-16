package mes.app.mobile.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AttendanceCurrentService {
    @Autowired
    SqlRunner sqlRunner;

    // 사용자 연차정보 조회
    public Map<String, Object> getAnnInfo(int personId) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("personid", personId);

        String sql = """
                SELECT t.ewolnum,
                    t.holinum,
                    t.daynum,
                    t.restnum,
                    p.rtdate
                FROM tb_pb209 t
                LEFT JOIN person p ON p.id = t.personid
                WHERE personid = :personid
        		""";

        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);

        return item;
    }
    // 사용자 휴가정보 조회
    public List<Map<String, Object>> getVacInfo(Integer workcd, String searchYear, int personId) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("personid", personId);

        String sql = """
                SELECT t.reqdate,
                    t.workcd,
                    i.worknm,
                    t.yearflag,
                    t.frdate,
                    t.todate,
                    t.daynum,
                    t.remark
                FROM tb_pb204 t
                LEFT JOIN tb_pb210 i ON t.workcd = i.workcd 
                WHERE personid = :personid
        		""";
        if(workcd != null){
            dicParam.addValue("workcd", workcd);
            sql += " AND workcd = :workcd";
        }
        if(searchYear != null && !searchYear.isEmpty()){
            dicParam.addValue("searchYear", searchYear);
            sql += " AND EXTRACT(YEAR FROM TO_DATE(reqdate, 'YYYYMMDD'))::text = :searchYear";
        }

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
}
