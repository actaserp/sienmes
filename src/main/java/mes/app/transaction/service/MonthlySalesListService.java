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

  public List<Map<String, Object>> getSalesList(String cboYear, Integer cboCompany) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);

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
        """);

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
            ps.icerdeptnm,
            ps.icerceonm
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
        GROUP BY c."Name", sc."Value", ps.icerdeptnm, ps.icerceonm
        ORDER BY c."Name", ps.icerdeptnm, ps.icerceonm
        """);

    // 로그 출력
//    log.info("월별 매출현황 (salesment 기준) SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());

    // 실행 및 반환
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), paramMap);
    return items;
  }

  // 입금
  public List<Map<String, Object>> getMonthDepositList(String cboYear, Integer cboCompany) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);

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
  public List<Map<String, Object>> getMonthReceivableList(String cboYear, Integer cboCompany) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);

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

    //log.info("월별 매출현황 (미수금) SQL: {}", sql);
    //log.info("SQL Parameters: {}", paramMap.getValues());

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), paramMap);
    return items;
  }
}
