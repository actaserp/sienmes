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

    public List<Map<String, Object>> getYearamtList(String year, String ioflag, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("name", name);
        dicParam.addValue("yyyymm", year + "12");

        // 1. tb_yearamt에서 yearamt 가져오기
        String yearamtSql = """
        SELECT COALESCE(SUM(yearamt), 0) 
        FROM tb_yearamt 
        WHERE ioflag = :ioflag 
          AND yyyymm = :yyyymm
    """;
        BigDecimal yearamt = this.sqlRunner.queryForObject(
                yearamtSql,
                dicParam,
                (rs, rowNum) -> rs.getBigDecimal(1)
        );

        // 2. tb_salesment에서 해당 연도 totalamt 합
        String salesamtSql = """
        SELECT COALESCE(SUM(totalamt), 0) 
        FROM tb_salesment 
        WHERE misdate BETWEEN :startDate AND :endDate
    """;
        dicParam.addValue("startDate", year + "-01-01");
        dicParam.addValue("endDate", year + "-12-31");
        BigDecimal salesamt = this.sqlRunner.queryForObject(
                salesamtSql,
                dicParam,
                (rs, rowNum) -> rs.getBigDecimal(1)
        );

        // 3. tb_banktransit에서 accin 합 (trdate 기준)
        String accinSql = """
        SELECT COALESCE(SUM(accin), 0) 
        FROM tb_banktransit 
        WHERE ioflag = :ioflag 
          AND trdate BETWEEN :startDate AND :endDate
    """;
        BigDecimal accin = this.sqlRunner.queryForObject(
                accinSql,
                dicParam,
                (rs, rowNum) -> rs.getBigDecimal(1)
        );

        // 4. 계산: (yearamt + salesamt) - accin
        BigDecimal result = yearamt.add(salesamt).subtract(accin);

        // 5. 결과 맵 반환
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("yearamt", yearamt);
        resultMap.put("salesamt", salesamt);
        resultMap.put("accin", accin);
        resultMap.put("finalResult", result);

        return List.of(resultMap);
    }


/*  
*  매입에 대한 코드는 적용아안되어있음 
* 
* 매입코드는
* 마감+매입-출금액
* */



}
