package mes.app.definition.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class AccSubjectService {
    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> getAccList() {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
            SELECT
                A.acccd,
                A.accnm,
                A.accprtnm,
                A.uacccd,
                A.acclv,
                A.drcr,
                A.dcpl,
                A.spyn,
                A.useyn,
                A.cacccd,
                A.etccode,
                B.accnm AS uaccde_name
            FROM tb_accsubject A
            LEFT JOIN tb_accsubject B
                ON A.uacccd = B.acccd
            ORDER BY A.acccd
        """;

        return this.sqlRunner.getRows(sql, dicParam);
    }



    public List<Map<String, Object>> getAccSearchitem(String code, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("code", code);
        dicParam.addValue("name", name);

        String sql = """
                 select
                 A.acccd as acccd
                 , A.accnm as accnm
                 from tb_accsubject A
                where A.acccd like concat('%',:code,'%')
                AND A.accnm like concat('%',:name,'%')
                order by A.acccd
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }


    public List<Map<String, String>> getAccCodeAndAccnmAndAcclvList() {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT acccd, accnm, acclv
                FROM tb_accsubject
            """;
        // SQL 실행
        List<Map<String, Object>> rows = this.sqlRunner.getRows(sql, dicParam);

        List<Map<String, String>> result = rows.stream()
                .map(row -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("acccd", (String) row.get("acccd"));
                    map.put("accnm", (String) row.get("accnm"));
                    map.put("acclv", String.valueOf(row.get("acclv")));
                    return map;
                })
                .toList();

        return result;
    }

}
