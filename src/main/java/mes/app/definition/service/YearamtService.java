package mes.app.definition.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class YearamtService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Map<String, Object>> getYearamtList(String year, String ioflag, String cltid, String name,String spjangcd) {
        // 공통 파라미터 설정
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("searchid", cltid);
        dicParam.addValue("name", name);
        dicParam.addValue("spjangcd", spjangcd);

        int targetYear = Integer.parseInt(year) - 1;
        String yyyymm = targetYear + "12";
        dicParam.addValue("yyyymm", yyyymm);

        String sql;
            if ("0".equals(ioflag)) {
                sql = """
            SELECT
                c.id,
                c."Name" AS company_name,
                COALESCE(
                    COALESCE(y.yearamt, 0) + COALESCE(s.totalamt_sum, 0) - COALESCE(b.accin_sum, 0),
                    0
                ) AS balance,
                 COALESCE(y.ioflag, :ioflag) AS ioflag,
                :year || '12' AS yyyymm,
                COALESCE(m.endyn, 'N') AS endyn
            FROM company c
            LEFT JOIN (
                SELECT cltcd, yearamt, ioflag
                FROM tb_yearamt
                WHERE yyyymm = :yyyymm
            ) y ON c.id = y.cltcd
            LEFT JOIN (
                SELECT cltcd, endyn
                FROM tb_yearamt
                WHERE yyyymm = :year || '12'
            ) m ON c.id = m.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(totalamt) AS totalamt_sum
                FROM tb_salesment
                WHERE misdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) s ON c.id = s.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(accin) AS accin_sum
                FROM tb_banktransit
                WHERE trdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) b ON c.id = b.cltcd
            WHERE c.relyn = '0'
              AND c.id::text LIKE concat('%', :searchid, '%')
              AND c."Name" LIKE concat('%', :name, '%')
              AND c.spjangcd = :spjangcd
            ORDER BY c.id
        """;
            } else {
                sql = """
            WITH client AS (
                 SELECT id, '0' AS cltflag, "Name" AS cltname
                 FROM company WHERE spjangcd = :spjangcd
                 UNION ALL
                 SELECT id, '1' AS cltflag, "Name" AS cltname
                 FROM person WHERE spjangcd = :spjangcd
                 UNION ALL
                 SELECT bankid AS id, '2' AS cltflag, banknm AS cltname
                 FROM tb_xbank WHERE spjangcd = :spjangcd
                 UNION ALL
                 SELECT id, '3' AS cltflag, cardnm AS cltname
                 FROM tb_iz010 WHERE spjangcd = :spjangcd
             ),
             yearamt AS (
                 SELECT cltcd, yearamt, ioflag
                 FROM tb_yearamt
                 WHERE yyyymm = :yyyymm
             ),
             end_flag AS (
                 SELECT cltcd, endyn
                 FROM tb_yearamt
                 WHERE yyyymm = :year || '12'
             ),
             invo_sum AS (
                 SELECT cltcd, SUM(totalamt) AS totalamt_sum
                 FROM tb_invoicement
                 WHERE misdate BETWEEN '20000101' AND :year || '1231'
                   AND spjangcd = :spjangcd
                 GROUP BY cltcd
             ),
             bank_sum AS (
                 SELECT cltcd, SUM(accout) AS accout_sum
                 FROM tb_banktransit
                 WHERE trdate BETWEEN '20000101' AND :year || '1231'
                   AND spjangcd = :spjangcd
                   AND ioflag = '1'
                 GROUP BY cltcd
             )
             SELECT
                 c.id,
                c.cltflag,
                CASE c.cltflag
                      WHEN '0' THEN '업체'
                      WHEN '1' THEN '직원정보'
                      WHEN '2' THEN '은행계좌'
                      WHEN '3' THEN '카드사'
                  END AS cltflagnm,
                 c.cltname AS company_name,
                 COALESCE(
                     COALESCE(y.yearamt, 0) + COALESCE(s.totalamt_sum, 0) - COALESCE(b.accout_sum, 0),
                     0
                 ) AS balance,
                 COALESCE(y.ioflag, :ioflag) AS ioflag,
                 :year || '12' AS yyyymm,
                 COALESCE(m.endyn, 'N') AS endyn
             FROM client c
             LEFT JOIN yearamt y ON y.cltcd = c.id
             LEFT JOIN end_flag m ON m.cltcd = c.id
             LEFT JOIN invo_sum s ON s.cltcd = c.id
             LEFT JOIN bank_sum b ON b.cltcd = c.id
             WHERE c.id::text LIKE concat('%', :searchid, '%')
               AND c.cltname LIKE concat('%', :name, '%')
             ORDER BY c.cltflag
        """;
        }
//        log.info("매입매출 년마감 SQL: {}", sql);
//        log.info("SQL Parameters: {}", dicParam.getValues());
        return namedParameterJdbcTemplate.queryForList(sql, dicParam);

    }

}
