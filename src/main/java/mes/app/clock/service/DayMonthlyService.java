package mes.app.clock.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DayMonthlyService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getDayList(String work_division, String serchday) {
        MapSqlParameterSource paramMap = new MapSqlParameterSource();

        // serchday이 20250514 형식으로 들어와서 202505 / 14 으로 형식 분리
        String workym = null;
        String workday = null;
        if (serchday != null && serchday.length() == 8) {
            workym = serchday.substring(0, 6);
            workday = serchday.substring(6, 8);
        }

        Integer divisionInt = (work_division != null && !work_division.isEmpty())
                ? Integer.parseInt(work_division)
                : null;
        paramMap.addValue("work_division", divisionInt);

        paramMap.addValue("workym", workym);
        paramMap.addValue("workday", workday);

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
                s."Value" as jik_id
            FROM tb_pb201 t
            LEFT JOIN auth_user a ON a.personid = t.personid
            LEFT JOIN person p ON p.id = a.personid
           LEFT JOIN sys_code g ON g."Code" = p."PersonGroup_id"::text
             LEFT JOIN (
                 SELECT "Code", "Value"
                 FROM sys_code
                 WHERE "CodeType" = 'jik_type'
             ) s ON s."Code" = p.jik_id
            WHERE t.workym = :workym
              AND t.workday = :workday
              AND p."PersonGroup_id" = :work_division
        """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
        return items;
    }






}
