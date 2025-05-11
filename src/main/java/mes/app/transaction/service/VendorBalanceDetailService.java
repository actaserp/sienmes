package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VendorBalanceDetailService {
  @Autowired
  SqlRunner sqlRunner;

  // 지급현황 리스트 조회
  /*public List<Map<String, Object>> getPaymentList(String date_from, String date_to, Integer companyCode, String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("date_from", date_from);
    dicParam.addValue("date_to", date_to);
    dicParam.addValue("companyCode", companyCode);
    dicParam.addValue("spjangcd", spjangcd);

    String sql = """
              WITH detail_summary AS (
                        SELECT 
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
                         sc."Value" as iotype,
                         b.accnum ,
                         b.remark1,
                         b.eumnum,
                        CASE 
                          WHEN b.eumtodt IS NULL OR b.eumtodt = '' THEN NULL
                          ELSE TO_CHAR(TO_DATE(b.eumtodt, 'YYYYMMDD'), 'YYYY-MM-DD')
                        END AS eumtodt,
                         COALESCE(NULLIF(TRIM(b.banknm), ''), b.eumnum) AS bank_info,
                         b.eumtodt AS todate,
                         tt.tradenm as trid
                     FROM tb_banktransit b
                     left join  sys_code sc on sc."Code" = b.iotype
                     left join tb_trade tt on b.trid = tt.trid and b.spjangcd = tt.spjangcd
                     )
                    SELECT 
                         c."Name",
                         i.misdate ,
                         i.totalamt ,
                         i.item_summary,
                         bd.accout ,
                         bd.accnum,
                         bd.balance,
                         bd.iotype,
                         bd.bank_info,
                         bd.todate,
                         bd.remark1,
                         bd.trid,
                         bd.eumnum,
                         bd.eumtodt
                    FROM company c
                    LEFT JOIN invoice_data i ON c.id = i.cltcd 
                    LEFT JOIN bank_data bd ON c.id = bd.cltcd 
        """;
    sql += " WHERE c.id = :companyCode ";

    sql += " ORDER BY c.\"Name\", i.misdate NULLS LAST, bd.todate NULLS LAST";
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
    log.info("거래처별잔액명세서(출금) read SQL: {}", sql);
    log.info("SQL Parameters: {}", dicParam.getValues());
    return items;
  }*/

  public List<Map<String, Object>> getPaymentList(Timestamp start, Timestamp end, String company, String spjangcd) {

    if (company == null || company.trim().isEmpty()) {
      //log.warn("⚠️ 거래처 코드가 비어 있습니다. 빈 리스트를 반환합니다.");
      return Collections.emptyList(); // 빈 결과 반환
    }

    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("company", Integer.parseInt(company));
    paramMap.addValue("spjangcd", spjangcd);


    // Timestamp → LocalDate
    LocalDate startDate = start.toLocalDateTime().toLocalDate();

    // 전월 구하기
    LocalDate prevMonth = startDate.minusMonths(1);

    // yyyymm
    String prevYm = prevMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

    // 기준일 (예: 20250301)
    String baseDate = prevMonth.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    // SQL 파라미터 등록
    paramMap.addValue("prevYm", prevYm);
    paramMap.addValue("baseDate", baseDate);


    String sql = """
        WITH lasttbl AS (
                    SELECT cltcd, yearamt, MAX(yyyymm) AS yyyymm
                    FROM tb_yearamt
                    WHERE yyyymm < :prevYm
                      AND ioflag = '1'
                      AND cltcd = :company
                      AND spjangcd = :spjangcd
                    GROUP BY cltcd, yearamt
                ),        
                invoice_data as (
                	select  
                	ti.cltcd,
                	ti.totalamt
                	from tb_invoicement ti -- 매입 내역 관리
                	left join tb_invoicdetail tid on ti.misdate = tid.misdate and ti.misnum = tid.misnum
                ),        
                incomtbl AS (
                    SELECT s.cltcd, SUM(s.accout) AS totaccout
                    FROM tb_banktransit s
                    WHERE s.trdate BETWEEN :start AND :end
                      AND s.cltcd = :company
                      AND s.spjangcd = :spjangcd
                    GROUP BY s.cltcd
                ),
                union_data_raw AS (
                    -- 전잔액
                    SELECT
        			    y.id,                                    
        			    y."Name" AS comp_name,                 
        			    TO_DATE(:baseDate, 'YYYYMMDD') AS date,
        			    '전잔액' AS summary,                  
        			    (SUM(h.yearamt) + SUM(P.totalamt) - SUM(Q.totaccout))::numeric AS amount,
        			    NULL::text AS itemnm,                 
        			    NULL::numeric AS misgubun,              
        			    NULL::text AS iotype,                
        			    NULL::text AS banknm,                  
        			    NULL::text AS accnum,                 
        			    NULL::text AS eumnum,                  
        			    NULL::text AS eumtodt,                  
        			    NULL::text AS tradenm,                 
        			    NULL::numeric AS accin,               
        			    NULL::numeric AS accout,             
        			    NULL::text AS memo,                  
        			    NULL::text AS remark1,
        			    0 AS remaksseq                     
                    FROM company y
                    JOIN lasttbl h ON y.id = h.cltcd
                    JOIN invoice_data P ON y.id = P.cltcd
                    JOIN incomtbl Q ON y.id = Q.cltcd
                    GROUP BY y.id, y."Name"
                    UNION ALL
                    -- 매입
        				SELECT
        			    s.cltcd,                                 
        			    c."Name" AS comp_name,                
        			    TO_DATE(s.misdate, 'YYYYMMDD'),       
        			    '매출',                                
        			    s.totalamt AS amount,                 
        			    CONCAT(
        			        MAX(CASE WHEN d.misseq::int = 1 THEN d.itemnm END),
        			        CASE WHEN COUNT(DISTINCT d.itemnm) > 1 THEN ' 외 ' || (COUNT(DISTINCT d.itemnm) - 1) || '건' ELSE '' END
        			    ) AS itemnm,                           
        			    s.misgubun,                            
        			    NULL::text AS iotype,                 
        			    NULL::text AS banknm,               
        			    NULL::text AS accnum,               
        			    NULL::text AS eumnum,              
        			    NULL::text AS eumtodt,                
        			    NULL::text AS tradenm,               
        			    NULL::numeric AS accin,            
        			    NULL::numeric AS accout,             
        			    NULL::text AS memo,                   
        			    NULL::text AS remark1,
        			    1 AS remaksseq                   
        				FROM tb_invoicement s
        				LEFT JOIN tb_invoicdetail d ON s.misdate = d.misdate AND s.misnum = d.misnum
        				JOIN company c ON c.id = s.cltcd AND c.spjangcd = s.spjangcd
        				WHERE s.misdate BETWEEN :start AND :end
        				  AND s.spjangcd = :spjangcd
        				  AND s.cltcd = :company
        				GROUP BY s.cltcd, c."Name", s.misdate, s.totalamt, s.misgubun
                    UNION ALL
                    -- 지급
                   SELECT
        		    b.cltcd,                                 
        		    c."Name" AS comp_name,               
        		    TO_DATE(b.trdate, 'YYYYMMDD'),        
        		    '지급액',                             
        		    NULL::numeric AS amount,             
        		    NULL::text AS itemnm,                 
        		    NULL::numeric AS misgubun,             
        		    sc."Value" AS iotype,               
        		    b.banknm,                            
        		    b.accnum,                              
        		    b.eumnum,                               
        		    TO_CHAR(TO_DATE(NULLIF(b.eumtodt, ''), 'YYYYMMDD'), 'YYYY-MM-DD') AS eumtodt,  
        		    tt.tradenm,                           
        		    b.accin,                   
        		    b.accout,                     
        		    b.memo,                               
        		    b.remark1,
        		    2 AS remaksseq                  
                    FROM tb_banktransit b
                    JOIN company c ON c.id = b.cltcd AND c.spjangcd = b.spjangcd
                    LEFT JOIN sys_code sc ON sc."Code" = b.iotype
                    LEFT JOIN tb_trade tt ON tt.trid = b.trid AND tt.spjangcd = b.spjangcd
                    WHERE TO_DATE(b.trdate, 'YYYYMMDD') BETWEEN :start AND :end
                      AND b.cltcd = :company
                      AND b.spjangcd = :spjangcd
                ),
                union_data AS (
                    SELECT *,
                           ROW_NUMBER() OVER (PARTITION BY id, date ORDER BY remaksseq) AS rn
                    FROM union_data_raw
                )
                SELECT
                    x.id AS cltid,
                    x.comp_name,
                    x.date,
                    x.summary,
                    x.amount,
                    x.itemnm,
                    x.misgubun,
                    x.iotype,
                    x.banknm,
                    x.accnum,
                    x.eumnum,
                    x.eumtodt,
                    x.tradenm,
                    x.accin,
                    x.accout,
                    x.remark1,
                    x.memo,
                    SUM(
                         CASE
                             WHEN x.summary = '지급액' THEN -1 * COALESCE(x.accout, 0)
                             ELSE 0
                         END
                     ) OVER (
                         PARTITION BY x.id
                         ORDER BY x.date, x.remaksseq
                         ROWS UNBOUNDED PRECEDING
                     ) AS balance
                FROM union_data x
                ORDER BY x.id, x.date, x.remaksseq
        """;
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("거래처별잔액명세서(출금) read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

}

