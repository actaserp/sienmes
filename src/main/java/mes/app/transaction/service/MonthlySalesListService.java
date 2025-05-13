package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MonthlySalesListService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getSalesList(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);
    paramMap.addValue("spjangcd", spjangcd);

    String data_year = cboYear;
    paramMap.addValue("date_form", data_year + "0101");
    paramMap.addValue("date_to", data_year + "1231");

    StringBuilder sql = new StringBuilder();

    // CTE: parsed_sales
    sql.append("""
        WITH parsed_sales AS (
            SELECT
                ts.*,
                TO_CHAR(TO_DATE(ts.misdate, 'YYYYMMDD'), 'MM') AS sales_month
            FROM tb_salesment ts
            WHERE ts.misdate BETWEEN :date_form AND :date_to 
            and ts.spjangcd = :spjangcd
        """);
// and ts.spjangcd = :spjangcd
    // 회사 필터 조건을 CTE 내부에 삽입
    if (cboCompany != null) {
      sql.append(" AND ts.cltcd = :cboCompany");
    }

    sql.append(")\n");

    // SELECT 본문
    sql.append("""
        SELECT
            c."Name" AS comp_name,
            sc."Value" AS misgubun,
            ps.iverpernm,
            ps.iverdeptnm
        """);

    // 월별 합계 컬럼 추가 (mon_1 ~ mon_12)
    for (int i = 1; i <= 12; i++) {
      String month = String.format("%02d", i);
      sql.append(",\n  SUM(CASE WHEN sales_month = '").append(month)
          .append("' THEN COALESCE(ps.totalamt, 0) ELSE 0 END) AS mon_")
          .append(i);
    }

    // 총합계 컬럼
    sql.append(",\n  SUM(COALESCE(ps.totalamt, 0)) AS total_sum\n");

    // FROM, JOIN, GROUP BY, ORDER BY 절
    sql.append("""
        FROM parsed_sales ps
        LEFT JOIN company c ON c.id = ps.cltcd
        LEFT JOIN sys_code sc ON sc."Code" = ps.misgubun
        GROUP BY c."Name", sc."Value", ps.iverpernm, ps.iverdeptnm
        ORDER BY c."Name", ps.iverpernm, ps.iverdeptnm
        """);

    // 로그 출력
//    log.info("월별 매출현황 (salesment 기준) SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());

    // 실행 및 반환
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), paramMap);
    return items;
  }

  // 입금
  public List<Map<String, Object>> getMonthDepositList(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);
    paramMap.addValue("spjangcd", spjangcd);

    String data_year = cboYear;
    paramMap.addValue("date_form", data_year + "0101");
    paramMap.addValue("date_to", data_year + "1231");

    StringBuilder sql = new StringBuilder();

    // CTE: parsed_deposit
    sql.append("""
        WITH parsed_deposit AS (
            SELECT
                tb.*,
                TO_CHAR(TO_DATE(tb.trdate, 'YYYYMMDD'), 'MM') AS deposit_month
            FROM tb_banktransit tb
            WHERE tb.ioflag = '0'
              AND tb.trdate BETWEEN :date_form AND :date_to
              AND tb.spjangcd =:spjangcd
        """);

    // 회사 필터 조건을 CTE 내부에 삽입
    if (cboCompany != null) {
      sql.append(" AND tb.cltcd = :cboCompany");
    }

    sql.append(")\n");

    // SELECT 본문 시작
    sql.append("""
        SELECT
            c."Name" AS comp_name
        """);

    // 월별 합계 컬럼 (mon_1 ~ mon_12)
    for (int i = 1; i <= 12; i++) {
      String month = String.format("%02d", i);
      sql.append(",\n  SUM(CASE WHEN deposit_month = '").append(month)
          .append("' THEN COALESCE(pd.accin, 0) ELSE 0 END) AS mon_").append(i);
    }

    // 총합 컬럼
    sql.append(",\n  SUM(COALESCE(pd.accin, 0)) AS total_sum\n");

    // FROM, JOIN, GROUP BY, ORDER BY
    sql.append("""
        FROM parsed_deposit pd
        LEFT JOIN company c ON c.id = pd.cltcd
        GROUP BY c."Name"
        ORDER BY c."Name"
        """);

//    log.info("월별 입금현황 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), paramMap);
    return items;
  }

  //미수금
  public List<Map<String, Object>> getMonthReceivableList(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);
    paramMap.addValue("spjangcd", spjangcd);

    String dateFrom = cboYear + "0101";
    String dateTo = cboYear + "1231";
    paramMap.addValue("date_form", dateFrom);
    paramMap.addValue("date_to", dateTo);

    StringBuilder sql = new StringBuilder();

    sql.append("""
        WITH parsed_sales AS (
            SELECT
                s.cltcd,
                TO_CHAR(TO_DATE(s.misdate, 'YYYYMMDD'), 'MM') AS sale_month,
                SUM(s.totalamt) AS sale_amt
            FROM tb_salesment s
            WHERE s.misdate BETWEEN :date_form AND :date_to
              AND s.spjangcd = :spjangcd
        """);

    if (cboCompany != null) {
      sql.append(" AND s.cltcd = :cboCompany");
    }

    sql.append("""
            GROUP BY s.cltcd, TO_CHAR(TO_DATE(s.misdate, 'YYYYMMDD'), 'MM')
        ),
        parsed_deposit AS (
            SELECT
                b.cltcd,
                TO_CHAR(TO_DATE(b.trdate, 'YYYYMMDD'), 'MM') AS deposit_month,
                SUM(b.accin) AS accin_amt
            FROM tb_banktransit b
            WHERE b.trdate BETWEEN :date_form AND :date_to
              AND b.ioflag = '0'
              AND b.spjangcd = :spjangcd
        """);

    if (cboCompany != null) {
      sql.append(" AND b.cltcd = :cboCompany");
    }

    sql.append("""
            GROUP BY b.cltcd, TO_CHAR(TO_DATE(b.trdate, 'YYYYMMDD'), 'MM')
        ),
        base AS (
            SELECT DISTINCT cltcd FROM parsed_sales
            UNION
            SELECT DISTINCT cltcd FROM parsed_deposit
        ),
        final_data AS (
            SELECT
                b.cltcd,
                c."Name" AS comp_name,
                m.month,
                COALESCE(s.sale_amt, 0) AS sale_amt,
                COALESCE(d.accin_amt, 0) AS accin_amt,
                COALESCE(s.sale_amt, 0) - COALESCE(d.accin_amt, 0) AS remain_amt
            FROM base b
            CROSS JOIN (
                SELECT TO_CHAR(GENERATE_SERIES(1,12), 'FM00') AS month
            ) m
            LEFT JOIN company c ON c.id = b.cltcd
            LEFT JOIN parsed_sales s ON s.cltcd = b.cltcd AND s.sale_month = m.month
            LEFT JOIN parsed_deposit d ON d.cltcd = b.cltcd AND d.deposit_month = m.month
        )
        SELECT 
            comp_name
        """);

    for (int i = 1; i <= 12; i++) {
      String month = String.format("%02d", i);
      sql.append(",\n  SUM(CASE WHEN month = '").append(month)
          .append("' THEN remain_amt ELSE 0 END) AS mon_").append(i);
    }

    sql.append("""
        ,SUM(remain_amt) AS total_sum
        FROM final_data
        GROUP BY comp_name
        ORDER BY comp_name
    """);

//    log.info("월별 미수금 집계 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());

    return this.sqlRunner.getRows(sql.toString(), paramMap);
  }
}
