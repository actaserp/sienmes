package mes.app.mobile.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CommuteCurrentService {
    @Autowired
    SqlRunner sqlRunner;

    // 차트 데이터 조회
    public List<Map<String, Object>> getUserInfo(String username, Integer workcd, String searchFromDate, String searchToDate) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);
        dicParam.addValue("workcd", workcd);
        dicParam.addValue("searchFromDate", searchFromDate);
        dicParam.addValue("searchToDate", searchToDate);

        String sql = """
                SELECT
                t.workym,
                t.workday,
                t.personid,
                t.worknum,
                t.holiyn,
                t.workyn,
                t.workcd,
                t.starttime,
                t.endtime,
                t.worktime,
                t.nomaltime,
                t.overtime,
                t.nighttime,
                t.holitime,
                t.jitime,
                t.jotime,
                t.yuntime,
                t.abtime,
                t.bantime,
                t.adttime01,
                t.adttime02,
                t.adttime03,
                t.adttime04,
                t.adttime05,
                t.adttime06,
                t.adttime07,
                t.remark,
                t.fixflag,
                a.first_name,
                s."Value" as group_name
            FROM tb_pb201 t
            LEFT JOIN auth_user a ON a.personid = t.personid
            LEFT JOIN person p ON p.id = a.personid
            LEFT JOIN sys_code s ON s."Code" = p."PersonGroup_id"::text
            WHERE 1=1
              AND a.username = :username
        		""";

        if(workcd != null && !searchToDate.isEmpty()){
            sql += " AND t.workcd = :workcd";
        }
        if(searchFromDate != null && !searchToDate.isEmpty()){
            sql += " workym >= :searchFromDate";
        }
        if(searchToDate != null && !searchToDate.isEmpty()){
            sql += " workym <= :searchToDate";
        }


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}
