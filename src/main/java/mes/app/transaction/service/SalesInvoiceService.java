package mes.app.transaction.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
public class SalesInvoiceService {

	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getList(String invoice_kind, Integer cboCompany, Timestamp start, Timestamp end) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("invoice_kind", invoice_kind);
		dicParam.addValue("cboCompany", cboCompany);
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);

		String sql = """
        select
            TO_CHAR(TO_DATE(m."misdate", 'YYYYMMDD'), 'YYYY-MM-DD') AS misdate,
            m.misnum,
            m.misgubun,
            fn_code_name('sale_type', m.misgubun) as misgubun_name,
            m.cltcd,
            SUBSTRING(m.ivercorpnum FROM 1 FOR 3) || '-' ||
            SUBSTRING(m.ivercorpnum FROM 4 FOR 2) || '-' ||
            SUBSTRING(m.ivercorpnum FROM 6 FOR 5) AS ivercorpnum,
            m.ivercorpnm,
            m.totalamt,
            m.supplycost,
            m.taxtotal,
            m.statecode,
            m.iverceonm,
            m.iveremail,
            m.iveraddr,
            m.taxtype,
            CASE 
                WHEN COUNT(d.itemnm) > 1 THEN 
                    MIN(d.itemnm) || ' 외 ' || (COUNT(d.itemnm) - 1) || '개'
                WHEN COUNT(d.itemnm) = 1 THEN 
                    MIN(d.itemnm)
                ELSE NULL
            END as item_summary
        from tb_salesment m
        left join tb_salesdetail d
            on m.misdate = d.misdate and m.misnum = d.misnum
        where 1 = 1
        """; // 조건은 아래에서 붙임

		if (invoice_kind != null && !invoice_kind.isEmpty()) {
			sql += " and m.taxtype = :invoice_kind ";
		}

		if (cboCompany != null) {
			sql += " and m.cltcd = :cboCompany ";
		}

		if (start != null && end != null) {
			sql += " and to_date(m.misdate, 'YYYYMMDD') between :start and :end ";
		}

		sql += """
        group by 
            m.misdate, m.misnum, m.misgubun, m.cltcd, m.ivercorpnum, m.ivercorpnm,
            m.totalamt, m.supplycost, m.taxtotal, m.statecode, m.iverceonm,
            m.iveremail, m.iveraddr, m.taxtype
        order by m.misdate desc
        """;

		return this.sqlRunner.getRows(sql, dicParam);
	}


	public List<Map<String, Object>> getShipmentHeadList(String dateFrom, String dateTo) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom);
		paramMap.addValue("dateTo", dateTo);
		
		String sql = """
				select sh.id
		        , sh."Company_id" as company_id
                , c."Name" as company_name
		        , sh."ShipDate" as ship_date
		        , sh."TotalQty" as total_qty
	            , sh."TotalPrice" as total_price
	            , sh."TotalVat" as total_vat
	            , sh."TotalPrice" + sh."TotalVat" as total_amount
	            , sh."Description" as description
                , sh."State" as state
                , fn_code_name('shipment_state', sh."State") as state_name
                , to_char(coalesce(sh."OrderDate",sh."_created") ,'yyyy-mm-dd') as order_date
                , sh."StatementIssuedYN" as issue_yn
                , sh."StatementNumber" as stmt_number 
                , sh."IssueDate" as issue_date
                from shipment_head sh 
                join company c on c.id = sh."Company_id"   
                where sh."ShipDate"  between cast(:dateFrom as date) and cast(:dateTo as date)
                and sh."State" = 'shipped'
		 		order by sh."ShipDate" desc
		 		""";
        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}

	public Map<String, Object> getInvoicerDatail(String spjangcd) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("spjangcd", spjangcd);

		String sql = """
			select "saupnum"
			, "spjangnm"
			, "adresa"
			, "adresb"
			, "prenm"
			, ("adresa" || ' ' || COALESCE("adresb", '')) AS address
			, "biztype"
			, "item"
			, "tel1"
			, "agnertel1"
			, "agnertel2"
			, "emailadres"
			from tb_xa012
			where spjangcd = :spjangcd
			""";

		Map<String,Object> item = this.sqlRunner.getRow(sql, paramMap);

		return item;
	}




}
