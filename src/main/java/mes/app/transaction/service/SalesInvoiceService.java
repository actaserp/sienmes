package mes.app.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popbill.api.*;
import com.popbill.api.taxinvoice.MgtKeyType;
import com.popbill.api.taxinvoice.Taxinvoice;
import com.popbill.api.taxinvoice.TaxinvoiceDetail;
import mes.Encryption.EncryptionUtil;
import mes.app.util.UtilClass;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.repository.ShipmentHeadRepository;
import mes.domain.repository.TB_SalesDetailRepository;
import mes.domain.repository.TB_SalesmentRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

	@Autowired
	private CloseDownService closeDownService;

	@Autowired
	private TaxinvoiceService taxinvoiceService;

	@Value("${invoice.api.key}")
	private String invoiceeCheckApiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

	@Autowired
	private ShipmentHeadRepository shipmentHeadRepository;

	public List<Map<String, Object>> getList(String invoice_kind, Integer cboStatecode, Integer cboCompany, Timestamp start, Timestamp end, String spjangcd) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("invoice_kind", invoice_kind);
		dicParam.addValue("cboStatecode", cboStatecode);
		dicParam.addValue("cboCompany", cboCompany);
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		dicParam.addValue("spjangcd", spjangcd);

		String sql = """
			WITH detail_summary AS (
				SELECT DISTINCT ON (misnum)
					misnum,
					itemnm AS first_itemnm,
					COUNT(*) OVER (PARTITION BY misnum) AS item_count
				FROM tb_salesdetail
				ORDER BY misnum, misseq
			)
		   
		   SELECT
			   TO_CHAR(TO_DATE(m.misdate, 'YYYYMMDD'), 'YYYY-MM-DD') AS misdate,
			   m.misnum,
			   m.misgubun,
			   sale_type_code."Value" AS misgubun_name,  -- fn_code_name 제거
			   m.cltcd,
			   m.ivercorpnum,
			   m.ivercorpnm,
			   m.totalamt,
			   m.supplycost,
			   m.taxtotal,
			   m.statecode,
			   state_code."Value" AS statecode_name, -- fn_code_name 제거
			   TO_CHAR(TO_TIMESTAMP(m.statedt, 'YYYYMMDDHH24MISS'), 'YYYY-MM-DD HH24:MI:SS') AS statedt_formatted,
			   m.iverceonm,
			   m.iveremail,
			   m.iveraddr,
			   m.taxtype,
			   CASE
				   WHEN ds.item_count > 1 THEN ds.first_itemnm || ' 외 ' || (ds.item_count - 1) || '개'
				   WHEN ds.item_count = 1 THEN ds.first_itemnm
				   ELSE NULL
			   END AS item_summary
		   
		   FROM tb_salesment m
		   
		   LEFT JOIN tb_salesdetail d
			   ON m.misnum = d.misnum
		   
		   LEFT JOIN detail_summary ds
			   ON m.misnum = ds.misnum
		   
		   LEFT JOIN sys_code sale_type_code
			   ON sale_type_code."CodeType" = 'sale_type'
			   AND sale_type_code."Code" = m.misgubun
		   
		   LEFT JOIN sys_code state_code
			   ON state_code."CodeType" = 'state_code_pb'
			   AND state_code."Code" = m.statecode::text
		   
		   WHERE 1 = 1
		   and m.spjangcd = :spjangcd 
        """; // 조건은 아래에서 붙임

		if (invoice_kind != null && !invoice_kind.isEmpty()) {
			sql += " and m.taxtype = :invoice_kind ";
		}

		if (cboStatecode != null) {
			sql += " and m.statecode = :cboStatecode ";
		}

		if (cboCompany != null) {
			sql += " and m.cltcd = :cboCompany ";
		}

		if (start != null && end != null) {
			sql += " and to_date(m.misdate, 'YYYYMMDD') between :start and :end ";
		}

		sql += """
		GROUP BY
			m.misdate, m.misnum, m.misgubun, sale_type_code."Value", m.cltcd, m.ivercorpnum,
			m.ivercorpnm, m.totalamt, m.supplycost, m.taxtotal, m.statecode,
			state_code."Value", m.statedt, m.iverceonm, m.iveremail,
			m.iveraddr, m.taxtype, ds.first_itemnm, ds.item_count
        ORDER BY m.misdate DESC, m.misnum DESC
        """;

		return this.sqlRunner.getRows(sql, dicParam);
	}

	@Transactional
	public AjaxResult saveInvoicee(Map<String, Object> paramMap, User user) {
		AjaxResult result = new AjaxResult();

		String idStr = (String) paramMap.get("InvoiceeID");
		Integer id = (idStr != null && !idStr.isEmpty()) ? Integer.parseInt(idStr) : null;

		String name = (String) paramMap.get("InvoiceeCorpName");
		String businessNumber =(String) paramMap.get("InvoiceeCorpNum");

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
		company.setAddress((String) paramMap.get("InvoiceeAddr"));
		company.setBusinessItem((String) paramMap.get("InvoiceeBizClass"));
		company.setBusinessType((String) paramMap.get("InvoiceeBizType"));
		company.setCEOName((String) paramMap.get("InvoiceeCEOName"));
		company.setAccountManager((String) paramMap.get("InvoiceeContactName1"));
		company.setName(name);
		company.setEmail((String) paramMap.get("InvoiceeEmail1"));
		company.setAccountManagerPhone((String) paramMap.get("InvoiceeTEL1"));
		company.setCompanyType("sale");
		company.set_audit(user);
		company.setRelyn("0");
		company.setSpjangcd((String) paramMap.get("spjangcd"));

		company = companyRepository.save(company);

		if (company.getCode() == null || company.getCode().isEmpty()) {
			company.setCode("Corp-" + company.getId());
			company = companyRepository.save(company);
		}

		result.data = company;
		result.success = true;

		return result;
	}

	@Transactional
	public AjaxResult saveInvoice(@RequestBody Map<String, Object> form) {

		AjaxResult result = new AjaxResult();

		// 1. 기본 키 생성
		Integer misnum = parseInt(form.get("misnum"));
		boolean isUpdate = misnum != null ;

		TB_Salesment salesment = new TB_Salesment();

		if (isUpdate) {
			salesment.setMisnum(misnum); // 기존 데이터 수정
		}

		LocalDateTime now = LocalDateTime.now();
		String statedt = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

		// 2. 필드 매핑
		salesment.setIssuetype((String) form.get("IssueType")); // 발행형태
		salesment.setTaxtype((String) form.get("TaxType")); // 과세형태

		String invoiceeType = (String) form.get("InvoiceeType");
		salesment.setInvoiceetype(invoiceeType); // 거래처 유형

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
		String corpNum = sanitizeNumericString(form.get("InvoiceeCorpNum"));

		if ("개인".equals(invoiceeType)) {
			try {
				corpNum = EncryptionUtil.encrypt(corpNum); // 암호화된 값으로 교체
			} catch (Exception e) {
				throw new RuntimeException("암호화 실패", e);
			}
		}
		// 등록번호
		salesment.setIvercorpnum(corpNum);

		salesment.setIverregid((String)form.get("InvoiceeTaxRegID")); // 종사업장
		salesment.setIvercorpnm((String)form.get("InvoiceeCorpName")); // 사업장
		salesment.setIverceonm((String)form.get("InvoiceeCEOName")); // 대표자명
		salesment.setIveraddr((String)form.get("InvoiceeAddr")); // 주소
		salesment.setIverbiztype((String)form.get("InvoiceeBizType")); // 업태
		salesment.setIverbizclass((String)form.get("InvoiceeBizClass")); // 종목
		salesment.setIverpernm((String) form.get("InvoiceeContactName1")); // 담당자명
		salesment.setIvertel(sanitizeNumericString(form.get("InvoiceeTEL1"))); // 담당자 연락처
		salesment.setIveremail((String)form.get("InvoiceeEmail1")); // 이메일
		String misdate = sanitizeNumericString(form.get("writeDate"));
		salesment = tb_salesmentRepository.save(salesment);

		salesment.setMgtkey("TAX-" + misdate + "-" +  salesment.getMisnum());
		salesment.setMisdate(misdate);
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
		salesment.setStatecode(100);
		salesment.setStatedt(statedt);

		if (isUpdate) {
			tb_salesDetailRepository.deleteByMisnum(misnum);   // 현재 PK로 삭제
		}

		salesment.setSpjangcd((String) form.get("spjangcd"));
		TB_Salesment saved = tb_salesmentRepository.save(salesment);

		// 3. 상세 목록 매핑
		int serialIndex = 1;
		List<TB_SalesDetail> details = new ArrayList<>();

		int i = 0;
		while (true) {
			String prefix = "detailList[" + i + "]";
			String itemName = (String) form.get(prefix + ".ItemName");

			if (itemName == null) break; // 더 이상 항목 없음

			if (itemName.trim().isEmpty()) {
				i++;
				continue;
			}

			String serialNum = String.valueOf(serialIndex++);

			TB_SalesDetail detail = new TB_SalesDetail();
			detail.setId(new TB_SalesDetailId(saved.getMisnum(), serialNum));
			detail.setMaterialId(parseInt(form.get(prefix + ".ItemId")));
			detail.setItemnm(itemName);
			detail.setMisdate(misdate);
			detail.setSpec((String) form.get(prefix + ".Spec"));
			detail.setQty(parseMoney(form.get(prefix + ".Qty")));
			detail.setUnitcost(parseMoney(form.get(prefix + ".UnitCost")));
			detail.setSupplycost(parseMoney(form.get(prefix + ".SupplyCost")));
			detail.setTaxtotal(parseMoney(form.get(prefix + ".Tax")));
			detail.setRemark((String) form.get(prefix + ".Remark"));
			detail.setSpjangcd((String) form.get("spjangcd"));

			String purchaseDT = (String) form.get(prefix + ".PurchaseDT");
			if (purchaseDT != null && purchaseDT.length() == 4) {
				String fullPurchaseDT = misdate.substring(0, 4) + purchaseDT;
				detail.setPurchasedt(fullPurchaseDT);
			} else {
				detail.setPurchasedt(null);
			}

			detail.setSalesment(saved);
			details.add(detail);

			i++;
		}

		saved.getDetails().clear();
		saved.getDetails().addAll(details);

		tb_salesmentRepository.save(saved);


		// 5. shipment_head 업데이트
		Object shipIdsObj = form.get("shipids");
		System.out.println("shipIdsObj: " + shipIdsObj);
		if (shipIdsObj != null) {
			String shipIdsStr = shipIdsObj.toString(); // "165,162,164"
			List<Integer> shipIds = Arrays.stream(shipIdsStr.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Integer::parseInt)
					.toList();

			// shipment_head 엔티티들 조회 후 misnum 설정
			List<ShipmentHead> shipments = shipmentHeadRepository.findAllById(shipIds);
			for (ShipmentHead shipment : shipments) {
				shipment.setMisnum(salesment.getMisnum()); // misnum은 auto-generated 이거나 업데이트 대상
			}

			shipmentHeadRepository.saveAll(shipments);
		}

		result.success = true;

		return result;
	}

	// 단건 사업자 검증
	public JsonNode validateSingleBusiness(String businessNumber) {
		try {
			String cleanBno = businessNumber.replaceAll("-", "");

			String url = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=" + invoiceeCheckApiKey + "&returnType=JSON";
			URI uri = URI.create(url);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			String jsonBody = jacksonObjectMapper.writeValueAsString(Map.of("b_no", List.of(cleanBno)));

			HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				JsonNode json = jacksonObjectMapper.readTree(response.getBody());
				JsonNode dataNode = json.path("data");
				if (dataNode.isArray() && dataNode.size() > 0) {
					JsonNode item = dataNode.get(0);
					String state = item.path("b_stt_cd").asText(); // "01": 정상
					if (!"01".equals(state)) {
						return null; // 휴업, 폐업 등 처리
					}
					return item;
				} else {
					throw new RuntimeException("사업자 정보가 없습니다.");
				}
			} else {
				throw new RuntimeException("사업자 진위 확인 실패 - 응답 없음");
			}
		} catch (Exception e) {
			System.out.println("=== 사업자 진위확인 API 예외 발생 ===");
			System.out.println("에러 메시지     : " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("사업자 진위 확인 중 오류: " + e.getMessage(), e);
		}
	}

	// 다건 사업자 검증
	public List<JsonNode> validateMultipleBusinesses(List<String> businessNumbers) {
		try {
			List<String> cleanList = businessNumbers.stream()
					.map(bno -> bno.replaceAll("-", ""))
					.toList();

			String url = "https://api.odcloud.kr/api/nts-businessman/v1/validate?serviceKey=" +
					URLEncoder.encode(invoiceeCheckApiKey, StandardCharsets.UTF_8);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = Map.of("b_no", cleanList);
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				JsonNode json = jacksonObjectMapper.readTree(response.getBody());
				JsonNode dataNode = json.path("data");
				if (dataNode.isArray()) {
					List<JsonNode> results = new ArrayList<>();
					dataNode.forEach(results::add);
					return results;
				} else {
					throw new RuntimeException("응답 데이터가 배열이 아닙니다.");
				}
			} else {
				throw new RuntimeException("사업자 진위 확인 실패 - 응답 없음");
			}
		} catch (Exception e) {
			throw new RuntimeException("사업자 진위 확인 중 오류: " + e.getMessage(), e);
		}
	}

	public List<Map<String, Object>> getShipmentHeadList(String dateFrom, String dateTo) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom);
		paramMap.addValue("dateTo", dateTo);
		
		String sql = """
			WITH material_summary AS (
				SELECT\s
					s."ShipmentHead_id",
					STRING_AGG(s."Material_id"::text, ',' ORDER BY s."Material_id") AS material_ids
				FROM shipment s
				GROUP BY s."ShipmentHead_id"
			)
			
			SELECT
				sh.id,
				sh."Company_id" AS company_id,
				c."Name" AS company_name,
				sh."ShipDate" AS ship_date,
				sh."TotalQty" AS total_qty,
				sh."TotalPrice" AS total_price,
				sh."TotalVat" AS total_vat,
				sh."TotalPrice" + sh."TotalVat" AS total_amount,
				sh."Description" AS description,
				sh."State" AS state,
				fn_code_name('shipment_state', sh."State") AS state_name,
				TO_CHAR(COALESCE(sh."OrderDate", sh."_created"), 'yyyy-mm-dd') AS order_date,
				sh."StatementIssuedYN" AS issue_yn,
				sh."StatementNumber" AS stmt_number,
				sh."IssueDate" AS issue_date,
				ms.material_ids
			FROM shipment_head sh
			JOIN company c
				ON c.id = sh."Company_id"
			LEFT JOIN material_summary ms
				ON ms."ShipmentHead_id" = sh.id
			WHERE sh."ShipDate" BETWEEN CAST(:dateFrom AS DATE) AND CAST(:dateTo AS DATE)
			  AND sh."State" = 'shipped'
			  AND sh."misnum" IS NULL
			ORDER BY sh."ShipDate" DESC;
			
		 		""";
        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}

	public Map<String, Object> getInvoiceDetail(Integer misnum) throws IOException {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("misnum", misnum);

		String sql = """ 
		SELECT\s
			m.misdate,
			TO_CHAR(TO_DATE(m.misdate, 'YYYYMMDD'), 'YYYY-MM-DD') AS "writeDate",
			m.misdate AS "mowriteDate",
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
		WHERE m.misnum = :misnum
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
			 SUBSTRING(d.purchasedt FROM 5 FOR 4) AS "PurchaseDT",
			 d.misseq AS "SerialNum"
		 FROM tb_salesdetail d
		 WHERE d.misnum = :misnum
		 ORDER BY d.misseq
		""";

		Map<String, Object> master = this.sqlRunner.getRow(sql, paramMap);
		List<Map<String, Object>> detailList = this.sqlRunner.getRows(detailSql, paramMap);

		UtilClass.decryptItem(master, "InvoiceeCorpNum", 0);

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

	public AjaxResult issueInvoice(List<Map<String, String>> issueList) {
		AjaxResult result = new AjaxResult();

		if (issueList.isEmpty()) {
			result.success = false;
			result.message = "세금계산서가 선택되지 않았습니다.";
			return result;
		}

		List<Integer> idList = issueList.stream()
				.map(item -> Integer.parseInt(item.get("misnum")))
				.toList();

		List<TB_Salesment> salesList = tb_salesmentRepository.findAllByMisnumIn(idList);
		if (salesList.isEmpty()) {
			result.success = false;
			result.message = "해당 세금계산서를 찾을 수 없습니다.";
			return result;
		}

		// 발행 처리
		AjaxResult issueResult = callPopbillIssue(salesList);

		System.out.println("callPopbillIssue result:");
		System.out.println("  success: " + issueResult.success);
		System.out.println("  message: " + issueResult.message);
		System.out.println("  data: " + issueResult.data);

		result.success = issueResult.success;
		result.message = issueResult.message;
		return result;
	}

	private AjaxResult callPopbillIssue(List<TB_Salesment> salesList) {
		AjaxResult result = new AjaxResult();
		List<String> successList = new ArrayList<>();
		List<String> failList = new ArrayList<>();

		for (TB_Salesment sales : salesList) {

			AjaxResult singleResult = callSingleIssue(sales); // 별도 트랜잭션
			if (singleResult.success) {
				successList.add("상호: " + sales.getIvercorpnm());
			} else {
				failList.add("상호: " + sales.getIvercorpnm() + " (" + singleResult.message + ")");
			}
		}

		if (failList.isEmpty()) {
			result.success = true;
			result.message = "총 " + salesList.size() + "건이 성공적으로 발행되었습니다.";
		} else {
			result.success = false;
			result.message = "일부 발행 실패: " + failList.size() + "건\n" + String.join("\n", failList);
		}

		return result;
	}


	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public AjaxResult callSingleIssue(TB_Salesment sm) {
		AjaxResult result = new AjaxResult();

		try {
			Taxinvoice taxinvoice = makeTaxInvoiceObject(sm);


			try {
				ObjectMapper mapper = new ObjectMapper();
				System.out.println("=== 팝빌 요청 세금계산서 JSON ===");
				System.out.println(mapper.writeValueAsString(taxinvoice));
			} catch (JsonProcessingException e) {
				System.err.println("JSON 출력 실패: " + e.getMessage());
			}


			// 고유 관리번호 필수
			String mgtKey = sm.getIcercorpnum();

			System.out.println(taxinvoice);

			// 발행 요청
			IssueResponse response = taxinvoiceService.registIssue(mgtKey, taxinvoice, false, "", false, "", "", "");
			System.out.println("팝빌 발행 결과 ==================");
			System.out.println("code: " + response.getCode());
			System.out.println("message: " + response.getMessage());
			System.out.println("invoiceNum: " + response.getNtsConfirmNum());

			LocalDateTime now = LocalDateTime.now();
			String statedt = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
			sm.setStatecode(300); // 즉시 발행 완료
			sm.setStatedt(statedt);
			tb_salesmentRepository.save(sm);

			result.success = true;
			result.message = "세금계산서가 발행되었습니다.";
		} catch (PopbillException e) {
			result.success = false;
			result.message = "팝빌 단건 발행 실패: " + e.getMessage();
		}

		return result;
	}

	private Taxinvoice makeTaxInvoiceObject(TB_Salesment sm) {
		// LazyInitializationException 방지
		sm.getDetails().size();

		Taxinvoice invoice = new Taxinvoice();

		// 공급자 정보
		invoice.setWriteDate(sm.getMisdate()); // 작성일자 (yyyymmdd)
		invoice.setIssueType(sm.getIssuetype());       // 발행형태 - 정발행, 역발행 등
		invoice.setTaxType(sm.getTaxtype());           // 과세형태 - 과세, 영세, 면세
		invoice.setPurposeType(sm.getPurposetype());   // 영수/청구 구분
		invoice.setChargeDirection("정과금");

		invoice.setSupplyCostTotal(String.valueOf(Optional.ofNullable(sm.getSupplycost()).orElse(0)));
		invoice.setTaxTotal(String.valueOf(Optional.ofNullable(sm.getTaxtotal()).orElse(0)));
		invoice.setTotalAmount(String.valueOf(Optional.ofNullable(sm.getTotalamt()).orElse(0)));

		// 공급자 정보 설정
		invoice.setInvoicerCorpNum(sm.getIcercorpnum());
		invoice.setInvoicerCorpName(sm.getIcercorpnm());
		invoice.setInvoicerCEOName(sm.getIcerceonm());
		invoice.setInvoicerAddr(sm.getIceraddr());
		invoice.setInvoicerBizType(sm.getIcerbiztype());
		invoice.setInvoicerBizClass(sm.getIcerbizclass());
		invoice.setInvoicerContactName(sm.getIcerpernm());
		invoice.setInvoicerEmail(sm.getIceremail());
		invoice.setInvoicerTEL(sm.getIcertel());
//		invoice.setInvoicerMgtKey(sm.getId().getMisdate() + "-" + sm.getId().getMisnum());
		invoice.setInvoicerMgtKey(sm.getMgtkey());

		// 공급받는자 정보 설정
		invoice.setInvoiceeType(sm.getInvoiceetype());

		String invoicerCorpNum = sm.getIvercorpnum();
		if ("개인".equals(sm.getInvoiceetype())) {
			try {
				Map<String, Object> tempMap = new HashMap<>();
				tempMap.put("ivercorpnum", invoicerCorpNum);
				UtilClass.decryptItem(tempMap, "ivercorpnum", 0); // 마스킹 없이 복호화
				invoicerCorpNum = (String) tempMap.get("ivercorpnum");
			} catch (IOException e) {
				System.err.println("주민번호 복호화 실패: " + e.getMessage());
			}
		}

		invoice.setInvoiceeCorpNum(invoicerCorpNum);
		invoice.setInvoiceeCorpName(sm.getIvercorpnm());
		invoice.setInvoiceeCEOName(sm.getIverceonm());
		invoice.setInvoiceeAddr(sm.getIveraddr());
		invoice.setInvoiceeBizType(sm.getIverbiztype());
		invoice.setInvoiceeBizClass(sm.getIverbizclass());
		invoice.setInvoiceeContactName1(sm.getIverpernm());
		invoice.setInvoiceeEmail1(sm.getIveremail());
		invoice.setInvoiceeTEL1(sm.getIvertel());
		invoice.setInvoiceeMgtKey(""); // 공급받는자 문서 번호 > 역발행일 경우 필수

		// 메모 및 기타 정보
		invoice.setRemark1(sm.getRemark1());
		invoice.setRemark2(sm.getRemark2());
		invoice.setRemark3(sm.getRemark3());

		// 세부 품목 정보 설정
		List<TaxinvoiceDetail> detailList = sm.getDetails().stream().map(d -> {
			TaxinvoiceDetail detail = new TaxinvoiceDetail();
			detail.setSerialNum(Short.parseShort(d.getId().getMisseq()));
			detail.setItemName(Optional.ofNullable(d.getItemnm()).orElse(""));
			detail.setSpec(Optional.ofNullable(d.getSpec()).orElse(""));
			detail.setQty(String.valueOf(Optional.ofNullable(d.getQty()).orElse(0)));
			detail.setUnitCost(String.valueOf(Optional.ofNullable(d.getUnitcost()).orElse(0)));
			detail.setSupplyCost(String.valueOf(Optional.ofNullable(d.getSupplycost()).orElse(0)));
			detail.setTax(String.valueOf(Optional.ofNullable(d.getTaxtotal()).orElse(0)));
			detail.setRemark(Optional.ofNullable(d.getRemark()).orElse(""));
			return detail;
		}).toList();

		invoice.setDetailList(detailList);


		return invoice;
	}

	@Transactional
	public AjaxResult deleteSalesment(List<Map<String, String>> deleteList) {
		AjaxResult result = new AjaxResult();

		if (deleteList == null || deleteList.isEmpty()) {
			result.success = false;
			result.message = "삭제할 데이터가 없습니다.";
			return result;
		}

		List<Integer> idList = deleteList.stream()
				.map(item -> Integer.parseInt(item.get("misnum")))
				.toList();

		// 1. 관련된 shipment_head의 misnum 컬럼을 null로 변경
		List<ShipmentHead> relatedShipments = shipmentHeadRepository.findByMisnumIn(idList);
		for (ShipmentHead shipment : relatedShipments) {
			shipment.setMisnum(null);
		}
		shipmentHeadRepository.saveAll(relatedShipments);

		// 2. salesdetail 삭제
		deleteBySalesdetailIds(idList);

		// 3. salesment 삭제
		tb_salesmentRepository.deleteAllById(idList);

		result.success = true;
		return result;
	}

	public void deleteBySalesdetailIds(List<Integer> idList) {
		if (idList == null || idList.isEmpty()) return;

		String placeholders = idList.stream()
				.map(id -> "?")
				.collect(Collectors.joining(", "));

		String sql = "DELETE FROM tb_salesdetail WHERE misnum IN (" + placeholders + ")";
		jdbcTemplate.update(sql, idList.toArray());
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

	@Transactional
	public AjaxResult cancelIssue(List<Map<String, String>> cancelList) {
		AjaxResult result = new AjaxResult();

		if (cancelList == null || cancelList.isEmpty()) {
			result.success = false;
			result.message = "취소할 데이터가 없습니다.";
			return result;
		}

		List<String> successList = new ArrayList<>();
		List<String> failList = new ArrayList<>();

		for (Map<String, String> item : cancelList) {
			try {
				Integer misnum = Integer.parseInt(item.get("misnum"));
				TB_Salesment sm = tb_salesmentRepository.findById(misnum).orElse(null);
				if (sm == null) {
					failList.add("misnum: " + misnum + " (해당 내역 없음)");
					continue;
				}

				String corpNum = sm.getIcercorpnum();
				String mgtKey = sm.getMgtkey();

				Response response = taxinvoiceService.cancelIssue(
						corpNum,
						MgtKeyType.SELL,
						mgtKey,
						"",
						""
				);

				System.out.println("팝빌 발행 취소 결과 === misnum: " + misnum);
				System.out.println("code: " + response.getCode());
				System.out.println("message: " + response.getMessage());

				if (response.getCode() == 1) {
					sm.setStatecode(600); // 발행 취소
					LocalDateTime now = LocalDateTime.now();
					String statedt = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
					sm.setStatedt(statedt);
					tb_salesmentRepository.save(sm);
					successList.add("상호: " + sm.getIvercorpnm());
				} else {
					failList.add("상호: " + sm.getIvercorpnm() + " (" + response.getMessage() + ")");
				}

			} catch (Exception e) {
				failList.add("처리 중 오류 발생 (" + e.getMessage() + ")");
				e.printStackTrace();
			}
		}

		if (failList.isEmpty()) {
			result.success = true;
			result.message = "총 " + successList.size() + "건의 발행이 취소되었습니다.";
		} else {
			result.success = false;
			result.message = "일부 발행 취소 실패: " + failList.size() + "건\n" + String.join("\n", failList);
		}

		return result;
	}



}
