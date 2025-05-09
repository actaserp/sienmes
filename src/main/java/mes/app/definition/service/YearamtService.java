package mes.app.definition.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YearamtService {

    @Autowired
    SqlRunner sqlRunner;

    /* tb_salesment 매출 내역T
        tb_invoicement 매입 내역T
        tb_banktransit 입출금 관리T
        */

    public List<Map<String, Object>> getYearamtList(String year, String ioflag, String cltid, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("searchid", cltid);
        dicParam.addValue("name", name);

        /*System.out.println(dicParam.getValues()); */ // 파라미터 값 확인

        String sql = """
                WITH all_cltcds AS (
                    SELECT cltcd FROM (
                        SELECT cltcd, 'salesment' AS src FROM tb_salesment WHERE :ioflag = '0'
                        UNION ALL
                        SELECT cltcd, 'invoicement' AS src FROM tb_invoicement WHERE :ioflag = '1'
                    ) sales_union
                    WHERE cltcd IS NOT NULL
                      AND (
                          (src = 'salesment' AND :ioflag = '0' AND EXISTS (
                              SELECT 1 FROM tb_salesment WHERE cltcd = sales_union.cltcd AND misdate BETWEEN '20000101' AND :year || '1231'
                          ))
                          OR
                          (src = 'invoicement' AND :ioflag = '1' AND EXISTS (
                              SELECT 1 FROM tb_invoicement WHERE cltcd = sales_union.cltcd AND misdate BETWEEN '20000101' AND :year || '1231'
                          ))
                      )
                
                    UNION
                
                    SELECT cltcd FROM tb_banktransit
                    WHERE trdate BETWEEN '20000101' AND :year || '1231'
                      AND cltcd IS NOT NULL
                
                    UNION
                
                    SELECT cltcd FROM tb_yearamt
                    WHERE yyyymm = :year || '12'
                      AND ioflag = :ioflag
                      AND endyn = 'Y'
                      AND cltcd IS NOT NULL
                )
                
                SELECT
                    a.cltcd,
                    c."Name" AS company_name,
                    y.endyn AS endyn,
                    COALESCE(y.ioflag, :ioflag) AS ioflag,
                    COALESCE(y.yearamt, 0) AS yearamt,
                    COALESCE(s.total_sales, 0) AS total_sales,
                    COALESCE(b.total_income, 0) AS total_income,
                    (COALESCE(y.yearamt, 0) + COALESCE(s.total_sales, 0) - COALESCE(b.total_income, 0)) AS balance,
                    :year || '12' AS yyyymm
                
                FROM all_cltcds a
               
                LEFT JOIN (
                    SELECT cltcd, SUM(totalamt) AS total_sales
                    FROM tb_salesment
                    WHERE :ioflag = '0'
                      AND misdate BETWEEN '20000101' AND :year || '1231'
                    GROUP BY cltcd
                
                    UNION ALL
                
                    SELECT cltcd, SUM(totalamt) AS total_sales
                    FROM tb_invoicement
                    WHERE :ioflag = '1'
                      AND misdate BETWEEN '20000101' AND :year || '1231'
                    GROUP BY cltcd
                ) s ON a.cltcd = s.cltcd
                
                LEFT JOIN (
                    SELECT cltcd, SUM(
                        CASE WHEN :ioflag = '0' THEN accin ELSE accout END
                    ) AS total_income
                    FROM tb_banktransit
                    WHERE trdate BETWEEN '20000101' AND :year || '1231'
                    GROUP BY cltcd
                ) b ON a.cltcd = b.cltcd
                
                LEFT JOIN (
                    SELECT cltcd, yearamt, endyn, ioflag, yyyymm
                    FROM tb_yearamt
                    WHERE yyyymm = :year || '12'
                      AND ioflag = :ioflag
                ) y ON a.cltcd = y.cltcd
                
                LEFT JOIN company c ON a.cltcd = c.id
                WHERE a.cltcd::text LIKE concat('%', :searchid, '%')
                AND c."Name" LIKE concat('%', :name, '%')
                ORDER BY a.cltcd 
                """;

        return this.sqlRunner.getRows(sql, dicParam);
    }


}
