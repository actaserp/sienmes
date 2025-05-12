package mes.app.transaction.service;


import mes.app.util.UtilClass;
import mes.domain.enums.IssueState;
import mes.domain.services.SqlRunner;
import org.eclipse.jdt.internal.compiler.codegen.ObjectCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Map;
import java.util.List;

@Service
public class SalesListService {

    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> getList(Map<String, Object> parameter){
        MapSqlParameterSource param = new MapSqlParameterSource();

        String spjangcd = UtilClass.getStringSafe(parameter.get("spjangcd"));
        String searchfrdate = UtilClass.getStringSafe(parameter.get("searchfrdate"));
        String searchtodate = UtilClass.getStringSafe(parameter.get("searchtodate"));
        Integer cltcd = UtilClass.parseInteger(parameter.get("cltcd"));
        String taxtype = UtilClass.getStringSafe(parameter.get("taxtype"));
        String misgubun = UtilClass.getStringSafe(parameter.get("misgubun"));



        param.addValue("spjangcd", spjangcd);
        param.addValue("searchfrdate", searchfrdate);
        param.addValue("searchtodate", searchtodate);
        param.addValue("cltcd", cltcd);
        param.addValue("taxtype", taxtype);
        param.addValue("misgubun", misgubun);


        String sql = """
                select
                to_char(to_date(a.misdate, 'YYYYMMDD'), 'YYYY-MM-DD') as misdate
                ,b.spjangcd
                ,s."Value" as misgubun
                ,c."Code" as companyCode
                ,c."Name" as companyName
                ,b.iveremail
                ,a.itemnm
                ,a.spec
                ,COALESCE(a.supplycost, 0) AS supplycost
                ,COALESCE(a.taxtotal, 0) AS taxtotal
                ,(COALESCE(a.supplycost, 0) + COALESCE(a.taxtotal, 0)) as totalamt
                ,b.statecode
                from tb_salesdetail a
                left join tb_salesment b
                on a.misdate = b.misdate
                and a.misnum = b.misnum
                left join company c on b.cltcd = c.id
                left join sys_code s on s."Code" = b.misgubun
                where b.spjangcd = :spjangcd
                and a.misdate between :searchfrdate and :searchtodate
                """;

        if(cltcd != null){
            sql += """
                    and b.cltcd = :cltcd
                    """;
        }

        if(taxtype != null && !taxtype.isEmpty()){
            sql += """
                    and b.taxtype = :taxtype
                    """;
        }

        if(misgubun != null && !misgubun.isEmpty()){
            sql += """
                    and b.misgubun = :misgubun
                    """;
        }

        sql += """
                order by a.misdate, b.misgubun desc;
                """;

        List<Map<String, Object>> rows = sqlRunner.getRows(sql, param);
        return rows;
    }

    public void bindEnumLabels(List<Map<String, Object>> list){
        for(Map<String, Object> row : list){
            Object statecodeObj = row.get("statecode");
            if(statecodeObj != null){
                try{
                    int code = UtilClass.parseInteger(statecodeObj);
                    row.put("stateLabel", IssueState.getLabel(code));
                }catch (NumberFormatException e){
                    row.put("stateLabel", "잘못된 코드");
                }
            }

        }
    }
}
