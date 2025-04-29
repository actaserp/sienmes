package mes.app.transaction.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VendorBalanceDetailService {
    @Autowired
    SqlRunner sqlRunner;

    // 지급현황 리스트 조회
    public List<Map<String, Object>> getPaymentList(String date_from,
                                                    String date_to,
                                                    Integer companyCode) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("date_from", date_from);
        dicParam.addValue("date_to", date_to);
        dicParam.addValue("companyCode", companyCode);

        String sql = """
                WITH detail_summary AS (
                          SELECT\s
                              d.misdate,
                              d.misnum,
                              MIN(d.itemnm) AS 대표항목,
                              COUNT(*) - 1 AS 기타건수
                          FROM tb_invoicdetail d
                          GROUP BY d.misdate, d.misnum
                      ), invoice_data AS (
                          SELECT
                              i.cltcd,
                              i.misdate,
                              i.totalamt,
                              ds.대표항목,
                              CASE
                                  WHEN ds.기타건수 > 0 THEN ds.대표항목 || ' 외 ' || ds.기타건수 || '건'
                                  ELSE ds.대표항목
                              END AS item_summary
                          FROM tb_invoicement i
                          LEFT JOIN detail_summary ds
                              ON i.misdate = ds.misdate AND i.misnum = ds.misnum
                      ), bank_data AS (
                          SELECT
                              b.cltcd,
                              b.accout,
                              b.balance,
                              b.iotype,
                              b.remark1,
                              COALESCE(NULLIF(TRIM(b.banknm), ''), b.eumnum) AS bank_info,
                              b.eumtodt AS todate,
                              b.trid
                          FROM tb_banktransit b
                      )
                
                      SELECT\s
                          c."Name",
                          i.misdate ,
                          i.totalamt ,
                          i.item_summary,
                          bd.accout ,
                          bd.balance,
                          bd.iotype,
                          bd.bank_info,
                          bd.todate,
                          bd.remark1,
                          bd.trid
                      FROM company c
                      LEFT JOIN invoice_data i
                          ON c.id = i.cltcd
                      LEFT JOIN bank_data bd
                          ON c.id = bd.cltcd
                      
        		""";
        if(companyCode != null){
            sql += " WHERE c.Code = :companyCode";
        }
        sql += " ORDER BY c.\"Name\", i.misdate NULLS LAST, bd.todate NULLS LAST";


        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}

