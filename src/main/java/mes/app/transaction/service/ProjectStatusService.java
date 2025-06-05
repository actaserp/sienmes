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
public class ProjectStatusService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getProjectStatusList(String spjangcd, String txtProjectName) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("txtProjectName", txtProjectName);

    String sql = """
        SELECT
          da003.projno,
          da003.projnm,
          (SELECT SUM(COALESCE(s2."TotalAmount", 0))
            FROM suju s2 
            WHERE s2.project_id = da003.projno 
            AND s2.spjangcd = da003.spjangcd ) AS suju_totalamt,
          (SELECT COALESCE(SUM(s.totalamt), 0)
           FROM tb_salesment s 
          WHERE s.projectcode = da003.projno 
          AND s.spjangcd = da003.spjangcd ) AS sales_totalamt, 
        (SELECT SUM(COALESCE(i.supplycost, 0) + COALESCE(i.taxtotal, 0))
          FROM tb_invoicedetail i 
          WHERE i.spjangcd = da003.spjangcd
            AND i.projcd = da003.projno) AS invo_totalamt, 
        (SELECT SUM(COALESCE(b.accin, 0))
          FROM tb_banktransit b
          WHERE b.ioflag = '0'
            AND b.projno = da003.projno
            AND b.spjangcd = da003.spjangcd) AS total_accin,
        (SELECT SUM(COALESCE(b.accout, 0))
          FROM tb_banktransit b
          WHERE b.ioflag = '1'
            AND b.projno = da003.projno
            AND b.spjangcd = da003.spjangcd) AS total_accout,
          TO_CHAR(TO_DATE(da003.contdate, 'YYYYMMDD'), 'YYYY-MM-DD') AS contdate
        FROM tb_da003 da003
        LEFT JOIN tb_salesment s ON s.projectcode = da003.projno AND s.spjangcd = da003.spjangcd
        LEFT JOIN suju s2 ON s2.project_id = da003.projno  AND s2.spjangcd = da003.spjangcd
        LEFT JOIN tb_banktransit b ON b.spjangcd = da003.spjangcd AND b.projno = da003.projno 
        left join tb_invoicedetail i on da003.projno = i.projcd 
        WHERE da003.spjangcd = :spjangcd
        """;
    if (txtProjectName != null && !txtProjectName.isEmpty()) {
      sql += " AND da003.projnm LIKE :txtDescription ";
      dicParam.addValue("txtDescription", "%" + txtProjectName + "%");
    }
    sql += """
        GROUP BY da003.projno, da003.projnm, da003.contdate, da003.spjangcd
        """;

//    log.info("프로젝트 현황 AllRead SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  //경비 사용내역
  public List<Map<String, Object>> getExpenseHistory(String spjangcd, String projno) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("projno", projno);

    String sql = """
        select
        to_char(to_date(i.misdate, 'YYYYMMDD'), 'YYYY-MM-DD') as misdate,
        i.misnum ,
        i.misseq,
        i.artcd ,
        ca648.artnm ,
        i.itemnm,
        i.spec,
        i.qty,
        i.unitcost,
        i.supplycost,
        i.taxtotal,
        i.remark
        from tb_invoicedetail i
        left join tb_ca648 ca648 on i.artcd = ca648.artcd
        where i.spjangcd =:spjangcd 
         and i.projcd =:projno
          order by i.misdate,i.misnum, i.misseq
        """;

    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  //매출내역
  public List<Map<String, Object>> getSalesHistory(String spjangcd, String projno) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("projno", projno);

    String sql = """
        select
         to_char(to_date(s.misdate, 'YYYYMMDD'), 'YYYY-MM-DD') as misdate,
         cs."Value" as misgubun,
         s.icerdeptnm,
         s.misnum ,
         d.misseq ,
         d.itemnm,
         d.spec,
         d.qty,
         d.unitcost,
         d.supplycost,
         d.taxtotal,
         d.totalamt,
         d.remark
         from tb_salesment s
         left join tb_salesdetail d on s.misnum =d.misnum and s.spjangcd =d.spjangcd
         left join sys_code cs on cs."Code" = s.misgubun
         where s.spjangcd = :spjangcd
         and s.projectcode =:projno
        """;

//    log.info("프로젝트 현황_매출내역 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }

  //입출금내역
  public List<Map<String, Object>> getTransactionHistory(String spjangcd, String projno) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);
    dicParam.addValue("projno", projno);
    String sql = """
       SELECT 
         TO_CHAR(TO_DATE(b.trdate, 'YYYYMMDD'), 'YYYY-MM-DD') AS trdate,
         c."Name" AS comp_name,
         CASE b.ioflag
           WHEN '0' THEN '입금'
           WHEN '1' THEN '출금'
         END AS io_type,
         b.accin ,
         b.accout ,
         b.memo
       FROM tb_banktransit b
       LEFT JOIN company c ON b.cltcd = c.id 
       WHERE b.spjangcd = :spjangcd
       AND b.projno =:projno
        ORDER BY b.trdate
        """;
//    log.info("프로젝트 현황_입출금내역 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);

    return itmes;
  }
}
