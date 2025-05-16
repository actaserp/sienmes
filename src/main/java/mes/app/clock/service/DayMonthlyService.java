package mes.app.clock.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DayMonthlyService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getDayList(String work_division, String serchday, String spjangcd) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();

        // serchday이 20250514 형식으로 들어와서 202505 / 14 으로 형식 분리
        String workym = null;
        String workday = null;
        if (serchday != null && serchday.length() == 8) {
            workym = serchday.substring(0, 6);
            workday = serchday.substring(6, 8);
        }

        // 빈 문자열 허용을 위해 String 그대로 사용
        String divisionStr = (work_division != null) ? work_division : "";
        paramMap.addValue("work_division", divisionStr);


        paramMap.addValue("workym", workym);
        paramMap.addValue("workday", workday);
        paramMap.addValue("spjangcd", spjangcd);

        String sql = """
            SELECT
           ROW_NUMBER() OVER (ORDER BY CAST(s."Code" AS INTEGER)) AS row_num,
                t.workym,
                t.workday,
                SUBSTRING(t.workym, 1, 4) || '-' || SUBSTRING(t.workym, 5, 2) || '-' || LPAD(t.workday, 2, '0') AS workymd,
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
                g."Value" AS group_name,
                s."Value" as jik_id,
                tp210.worknm as worknm
            FROM tb_pb201 t
            LEFT JOIN auth_user a ON a.personid = t.personid
            LEFT JOIN person p ON p.id = a.personid
           LEFT JOIN (
              SELECT "Code", "Value"
              FROM sys_code
               WHERE "CodeType" = 'work_division'
           ) g ON g."Code" = LPAD(p."PersonGroup_id"::text, 2, '0')
             LEFT JOIN (
                 SELECT "Code", "Value"
                 FROM sys_code
                 WHERE "CodeType" = 'jik_type'
             ) s ON s."Code" = p.jik_id
             LEFT JOIN tb_pb210 tp210 ON tp210.workcd = t.workcd
            WHERE t.workym = :workym
              AND t.workday = :workday
              AND (
               :work_division = '' OR
               LPAD(p."PersonGroup_id"::text, 2, '0') = :work_division
                )
              AND t.spjangcd =:spjangcd
        """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
        return items;
    }


    public List<Map<String, String>> workcdList(String spjangcd) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("spjangcd", spjangcd);


        String sql = """
                SELECT worknm, workcd
                FROM tb_pb210
                where spjangcd = :spjangcd
            """;
        // SQL 실행
        List<Map<String, Object>> rows = this.sqlRunner.getRows(sql, dicParam);

        List<Map<String, String>> result = rows.stream()
                .map(row -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("worknm", (String) row.get("worknm"));
                    map.put("workcd", (String) row.get("workcd"));
                    return map;
                })
                .toList();

        return result;
    }



}
