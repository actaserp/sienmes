package mes.app.transaction.service;

import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.repository.TB_SalesDetailRepository;
import mes.domain.repository.TB_SalesmentRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalesInvoiceService {

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TB_SalesmentRepository tb_salesmentRepository;

	@Autowired
	private TB_SalesDetailRepository tb_salesDetailRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
            CASE\s
				WHEN COUNT(d.itemnm) > 1 THEN\s
					(
						SELECT d2.itemnm
						FROM tb_salesdetail d2
						WHERE d2.misdate = m.misdate AND d2.misnum = m.misnum
						ORDER BY d2.misseq
						LIMIT 1
					) || ' 외 ' || (COUNT(d.itemnm) - 1) || '개'
				WHEN COUNT(d.itemnm) = 1 THEN\s
					MIN(d.itemnm)
				ELSE NULL
			END AS item_summary
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

	@Transactional
	public AjaxResult saveInvoicee(Map<String, String> paramMap, User user) {
		AjaxResult result = new AjaxResult();

		String idStr = paramMap.get("InvoiceeID");
		Integer id = (idStr != null && !idStr.isEmpty()) ? Integer.parseInt(idStr) : null;

		String name = paramMap.get("InvoiceeCorpName");
		String businessNumber = paramMap.get("InvoiceeCorpNum");

		boolean nameExists = (id != null)
				? companyRepository.existsByNameAndIdNot(name, id)
				: companyRepository.existsByName(name);

		if (nameExists) {
			result.success = false;
			result.message = "이미 등록된 회사명이 존재합니다.";
			return result;
		}

		boolean businessNumberExists = (id != null)
				? companyRepository.existsByBusinessNumberAndIdNot(businessNumber, id)
				: companyRepository.existsByBusinessNumber(businessNumber);

		if (businessNumberExists) {
			result.success = false;
			result.message = "이미 등록된 사업자등록번호가 존재합니다.";
			return result;
		}

		Company company = (id != null) ? companyRepository.getCompanyById(id) : new Company();

		company.setBusinessNumber(businessNumber);
		company.setAddress(paramMap.get("InvoiceeAddr"));
		company.setBusinessItem(paramMap.get("InvoiceeBizClass"));
		company.setBusinessType(paramMap.get("InvoiceeBizType"));
		company.setCEOName(paramMap.get("InvoiceeCEOName"));
		company.setAccountManager(paramMap.get("InvoiceeContactName1"));
		company.setName(name);
		company.setEmail(paramMap.get("InvoiceeEmail1"));
		company.setAccountManagerPhone(paramMap.get("InvoiceeTEL1"));
		company.setCompanyType("sale");
		company.set_audit(user);
		company.setRelyn("0");

		company = companyRepository.save(company);
		result.data = company;
		result.success = true;

		return result;
	}

	@Transactional
	public AjaxResult saveInvoice(@RequestBody Map<String, Object> form) {

		AjaxResult result = new AjaxResult();
		// 1. 기본 키 생성
		String misdate = sanitizeNumericString(form.get("writeDate"));
		String misnum = (String) form.get("misnum");

		boolean isUpdate = misnum != null && !misnum.trim().isEmpty();

		// 신규일 경우 새 번호 생성
		if (!isUpdate) {
			misnum = generateMisnum(misdate);
		}

		TB_SalesmentId id = new TB_SalesmentId(misdate, misnum);

		TB_Salesment salesment = new TB_Salesment();
		salesment.setId(id);

		// 2. 필드 매핑
		salesment.setIssuetype((String) form.get("IssueType")); // 발행형태
		salesment.setTaxtype((String) form.get("TaxType")); // 과세형태
		salesment.setInvoiceetype((String) form.get("InvoiceeType")); // 거래처 유형
		salesment.setMisgubun(form.get("sale_type").toString()); // 매출구분
		salesment.setKwon(parseInt(form.get("Kwon"))); // 권
		salesment.setHo(parseInt(form.get("Ho"))); // 호
		salesment.setSerialnum((String) form.get("SerialNum")); // 일련번호

		// 공급자
		salesment.setIcercorpnum(sanitizeNumericString(form.get("InvoicerCorpNum"))); // 사업자번호
		salesment.setIcerregid((String)form.get("InvoicerTaxRegID")); // 종사업장
		salesment.setIcercorpnm((String) form.get("InvoicerCorpName")); // 사업장
		salesment.setIcerceonm((String) form.get("InvoicerCEOName")); // 대표자명
		salesment.setIceraddr((String) form.get("InvoicerAddr")); // 주소
		salesment.setIcerbiztype((String) form.get("InvoicerBizType")); // 업태
		salesment.setIcerbizclass((String) form.get("InvoicerBizClass")); // 종목
		salesment.setIcerpernm((String)form.get("InvoicerContactName")); // 담당자명
		salesment.setIcertel(sanitizeNumericString(form.get("InvoicerTEL"))); // 담당자 연락처
		salesment.setIceremail((String) form.get("InvoicerEmail")); // 이메일

		// 공급받는자
		salesment.setCltcd(parseInt(form.get("InvoiceeID")));
		salesment.setIvercorpnum(sanitizeNumericString(form.get("InvoiceeCorpNum"))); // 사업자번호
		salesment.setIverregid((String)form.get("InvoiceeTaxRegID")); // 종사업장
		salesment.setIvercorpnm((String)form.get("InvoiceeCorpName")); // 사업장
		salesment.setIverceonm((String)form.get("InvoiceeCEOName")); // 대표자명
		salesment.setIveraddr((String)form.get("InvoiceeAddr")); // 주소
		salesment.setIverbiztype((String)form.get("InvoiceeBizType")); // 업태
		salesment.setIverbizclass((String)form.get("InvoiceeBizClass")); // 종목
		salesment.setIverpernm((String) form.get("InvoiceeContactName1")); // 담당자명
		salesment.setIvertel(sanitizeNumericString(form.get("InvoiceeTEL1"))); // 담당자 연락처
		salesment.setIveremail((String)form.get("InvoiceeEmail1")); // 이메일

		salesment.setSupplycost(parseIntSafe(form.get("SupplyCostTotal"))); // 총 공급가액
		salesment.setTaxtotal(parseIntSafe(form.get("TaxTotal"))); // 총 세액
		salesment.setRemark1((String) form.get("Remark1")); // 비고1

		Object remark2 = form.get("Remark2");
		if (remark2 != null && !remark2.toString().trim().isEmpty()) {
			salesment.setRemark2(remark2.toString().trim()); // 비고2
		}

		Object remark3 = form.get("Remark3");
		if (remark3 != null && !remark3.toString().trim().isEmpty()) {
			salesment.setRemark3(remark3.toString().trim()); // 비고3
		}

		salesment.setTotalamt(parseMoney(form.get("TotalAmount"))); // 합계금액
		salesment.setCash(parseMoney(form.get("Cash"))); // 현금
		salesment.setChkbill(parseMoney(form.get("ChkBill"))); // 수표
		salesment.setNote(parseMoney(form.get("Note"))); // 어음
		salesment.setCredit(parseMoney(form.get("Credit"))); // 외상미수금
		salesment.setPurposetype((String) form.get("PurposeType"));
		salesment.setStatecode("저장");

		if (isUpdate) {
			tb_salesDetailRepository.deleteByMisdateAndMisnum(misdate, misnum);
		}

//		salesment.setSpjangcd((String) form.get("spjangcd")); // sessionStorage에서 넘겨받은 값
		int serialIndex = 1;
		// 3. 상세 목록 매핑
		List<TB_SalesDetail> details = new ArrayList<>();
		for (int i = 0; i < 99; i++) {
			String prefix = "detailList[" + i + "]";
			String itemName = (String) form.get(prefix + ".ItemName");

			if (itemName == null || itemName.trim().isEmpty()) continue;

			String serialNum = String.valueOf(serialIndex++);

			TB_SalesDetail detail = new TB_SalesDetail();
			detail.setId(new TB_SalesDetailId(misdate, misnum, serialNum));
			detail.setMaterialId(parseInt(form.get(prefix + ".ItemId")));
			detail.setItemnm(itemName);
			detail.setSpec((String) form.get(prefix + ".Spec"));
			detail.setQty(parseInt(form.get(prefix + ".Qty")));
			detail.setUnitcost(parseMoney(form.get(prefix + ".UnitCost")));
			detail.setSupplycost(parseMoney(form.get(prefix + ".SupplyCost")));
			detail.setTaxtotal(parseMoney(form.get(prefix + ".Tax")));
			detail.setRemark((String) form.get(prefix + ".Remark"));
			detail.setPurchasedt((String) form.get(prefix + ".PurchaseDT"));
			detail.setSalesment(salesment); // 양방향 설정

			details.add(detail);
		}

		salesment.setDetails(details);

		// 4. 저장
		tb_salesmentRepository.save(salesment); // cascade = ALL 이면 detail도 함께 저장됨

		result.success = true;

		return result;
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

	public Map<String, Object> getInvoiceDetail(String misdate, String misnum) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("misdate", misdate);
		paramMap.addValue("misnum", misnum);

		String sql = """ 
		SELECT\s
			m.misdate,
			m.misnum,
			m.issuetype AS "IssueType",
			m.taxtype AS "TaxType",
			m.misgubun AS "sale_type",
			m.kwon AS "Kwon",
			m.ho AS "Ho",
			m.serialnum AS "SerialNum",
			m."invoiceetype" AS "InvoiceeType",
			
			m.icercorpnum AS "InvoicerCorpNum",
			m.icerregid AS "InvoicerTaxRegID",
			m.icercorpnm AS "InvoicerCorpName",
			m.icerceonm AS "InvoicerCEOName",
			m.iceraddr AS "InvoicerAddr",
			m.icerbiztype AS "InvoicerBizType",
			m.icerbizclass AS "InvoicerBizClass",
			m.icerpernm AS "InvoicerContactName",
			m.icertel AS "InvoicerTEL",
			m.iceremail AS "InvoicerEmail",
	  
			m.cltcd AS "InvoiceeID",
			m.ivercorpnum AS "InvoiceeCorpNum",
			m.iverregid AS "InvoiceeTaxRegID",
			m.ivercorpnm AS "InvoiceeCorpName",
			m.iverceonm AS "InvoiceeCEOName",
			m.iveraddr AS "InvoiceeAddr",
			m.iverbiztype AS "InvoiceeBizType",
			m.iverbizclass AS "InvoiceeBizClass",
			m.iverpernm AS "InvoiceeContactName1",
			m.ivertel AS "InvoiceeTEL1",
			m.iveremail AS "InvoiceeEmail1",
	  
			m.supplycost AS "SupplyCostTotal",
			m.taxtotal AS "TaxTotal",
			m.remark1 AS "Remark1",
			m.remark2 AS "Remark2",
			m.remark3 AS "Remark3",
			m.totalamt AS "TotalAmount",
			m.cash AS "Cash",
			m.chkbill AS "ChkBill",
			m.note AS "Note",
			m.credit AS "Credit",
			m.purposetype AS "PurposeType",
			m.statecode AS "StateCode"
		FROM tb_salesment m
		WHERE m.misdate = :misdate AND m.misnum = :misnum
		""";

		String detailSql = """ 
		SELECT
			 d."Material_id" AS "ItemId",
			 d.itemnm AS "ItemName",
			 d.spec AS "Spec",
			 d.qty AS "Qty",
			 d.unitcost AS "UnitCost",
			 d.supplycost AS "SupplyCost",
			 d.taxtotal AS "Tax",
			 d.remark AS "Remark",
			 d.purchasedt AS "PurchaseDT",
			 d.misseq AS "SerialNum"
		 FROM tb_salesdetail d
		 WHERE d.misdate = :misdate AND d.misnum = :misnum
		 ORDER BY d.misseq
		""";

		Map<String, Object> master = this.sqlRunner.getRow(sql, paramMap);
		List<Map<String, Object>> detailList = this.sqlRunner.getRows(detailSql, paramMap);

		master.put("detailList", detailList);
		return master;
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

	@Transactional
	public AjaxResult deleteSalesment(List<Map<String, String>> deleteList) {
		AjaxResult result = new AjaxResult();

		if (deleteList == null || deleteList.isEmpty()) {
			result.success = false;
			result.message = "삭제할 데이터가 없습니다.";
			return result;
		}

		List<TB_SalesmentId> idList = deleteList.stream()
				.map(item -> new TB_SalesmentId(item.get("misdate"), item.get("misnum")))
				.toList();

		deleteBySalesdetailIds(idList);
		tb_salesmentRepository.deleteAllById(idList);


		result.success = true;
		return result;
	}

	public void deleteBySalesdetailIds(List<TB_SalesmentId> idList) {
		if (idList == null || idList.isEmpty()) return;

		StringBuilder sql = new StringBuilder("DELETE FROM tb_salesdetail WHERE (misdate, misnum) IN (");
		List<Object> params = new ArrayList<>();

		for (int i = 0; i < idList.size(); i++) {
			sql.append("(?, ?)");
			if (i < idList.size() - 1) sql.append(", ");
			params.add(idList.get(i).getMisdate());
			params.add(idList.get(i).getMisnum());
		}

		sql.append(")");
		jdbcTemplate.update(sql.toString(), params.toArray());
	}

	public String generateMisnum(String misdate) {
		// 가장 큰 misnum 조회
		Optional<String> maxMisnumOpt = tb_salesmentRepository.findMaxMisnumByMisdate(misdate);

		int nextNum = 1; // 기본값

		if (maxMisnumOpt.isPresent()) {
			try {
				nextNum = Integer.parseInt(maxMisnumOpt.get()) + 1;
			} catch (NumberFormatException e) {
				// 로그 찍고 기본값 유지
			}
		}

		return String.format("%04d", nextNum); // "0001", "0002" 형식
	}


	private Integer parseInt(Object obj) {
		if (obj == null || obj.toString().trim().isEmpty()) return null;
		return Integer.parseInt(obj.toString().trim());
	}

	private Integer parseMoney(Object obj) {
		if (obj == null || obj.toString().trim().isEmpty()) return null;
		return Integer.parseInt(obj.toString().replaceAll(",", "").trim());
	}

	private String sanitizeNumericString(Object obj) {
		if (obj == null) return null;
		return obj.toString().replaceAll("[^0-9]", "");  // 숫자만 남김
	}

	private Integer parseIntSafe(Object obj) {
		String numStr = sanitizeNumericString(obj);
		return (numStr == null || numStr.isEmpty()) ? null : Integer.parseInt(numStr);
	}




}
