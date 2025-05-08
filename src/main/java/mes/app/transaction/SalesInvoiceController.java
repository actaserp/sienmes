package mes.app.transaction;

import mes.app.transaction.service.SalesInvoiceService;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.repository.TB_SalesDetailRepository;
import mes.domain.repository.TB_SalesmentRepository;
import mes.domain.services.CommonUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tran/tran")
public class SalesInvoiceController {
	
	@Autowired
	private SalesInvoiceService salesInvoiceService;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TB_SalesmentRepository tb_salesmentRepository;

	@Autowired
	private TB_SalesDetailRepository tb_salesDetailRepository;
	
	@GetMapping("/shipment_head_list")
	public AjaxResult getShipmentHeadList(
			@RequestParam("srchStartDt") String dateFrom,
			@RequestParam("srchEndDt") String dateTo
	) {
		
		List<Map<String, Object>> items = this.salesInvoiceService.getShipmentHeadList(dateFrom,dateTo);

		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}

	@GetMapping("/invoicer_read")
	public AjaxResult getInvoicerDatail(
			@RequestParam("spjangcd") String spjangcd
	) {

		Map<String, Object> item = this.salesInvoiceService.getInvoicerDatail(spjangcd);

		AjaxResult result = new AjaxResult();
		result.data = item;

		return result;
	}

	// 공급받는자 저장
	@PostMapping("/invoicee_save")
	public AjaxResult InvoiceeSave(
			@RequestParam(value="InvoiceeID", required=false) Integer id,
			@RequestParam(value="InvoiceeCorpNum", required=false) String BusinessNumber,
			@RequestParam(value="InvoiceeAddr", required=false) String Address,
			@RequestParam(value="InvoiceeBizClass", required=false) String BusinessItem,
			@RequestParam(value="InvoiceeBizType", required=false) String BusinessType,
			@RequestParam(value="InvoiceeCEOName", required=false) String CEOName,
			@RequestParam(value="InvoiceeContactName1", required=false) String AccountManager,
			@RequestParam(value="InvoiceeCorpName", required=false) String Name,
			@RequestParam(value="InvoiceeEmail1", required=false) String Email,
			@RequestParam(value="InvoiceeTEL1", required=false) String AccountManagerPhone,
			@RequestParam(value="InvoiceeTaxRegID", required=false) String InvoiceeTaxRegID,
			HttpServletRequest request,
			Authentication auth ) {

		AjaxResult result = new AjaxResult();

		try {
			User user = (User) auth.getPrincipal();

			// **1. 회사명 중복 체크**
			boolean nameExists;
			if (id != null) {
				nameExists = this.companyRepository.existsByNameAndIdNot(Name, id);
			} else {
				nameExists = this.companyRepository.existsByName(Name);
			}
			if (nameExists) {
				result.success = false;
				result.message = "이미 등록된 회사명이 존재합니다.";
				return result;
			}

			// **2. 사업자등록번호 중복 체크**
			boolean businessNumberExists;
			if (id != null) {
				businessNumberExists = this.companyRepository.existsByBusinessNumberAndIdNot(BusinessNumber, id);
			} else {
				businessNumberExists = this.companyRepository.existsByBusinessNumber(BusinessNumber);
			}
			if (businessNumberExists) {
				result.success = false;
				result.message = "이미 등록된 사업자등록번호가 존재합니다.";
				return result;
			}

			Company company = null;

			if (id != null) {
				company = this.companyRepository.getCompanyById(id);
			} else {
				company = new Company();
			}

			company.setBusinessNumber(BusinessNumber);
			company.setAddress(Address);
			company.setBusinessItem(BusinessItem);
			company.setBusinessType(BusinessType);
			company.setCEOName(CEOName);
			company.setAccountManager(AccountManager);
			company.setName(Name);
			company.setEmail(Email);
			company.setAccountManagerPhone(AccountManagerPhone);
			company.setCompanyType("sale");
			company.set_audit(user);
			company.setRelyn("0");

			company = this.companyRepository.save(company);

			result.data = company;
			result.success = true;

		} catch (Exception e) {
			result.success = false;
			result.message = "저장 중 오류가 발생했습니다.";
		}

		return result;
	}

	@GetMapping("/read")
	public AjaxResult getSujuList(
			@RequestParam(value="invoice_kind", required=false) String invoice_kind,
			@RequestParam(value="start", required=false) String start_date,
			@RequestParam(value="end", required=false) String end_date,
			@RequestParam(value="cboCompany", required=false) Integer cboCompany,
			HttpServletRequest request) {

		start_date = start_date + " 00:00:00";
		end_date = end_date + " 23:59:59";

		Timestamp start = Timestamp.valueOf(start_date);
		Timestamp end = Timestamp.valueOf(end_date);

		List<Map<String, Object>> items = this.salesInvoiceService.getList(invoice_kind, cboCompany, start, end);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@PostMapping("/invoice_save")
	public AjaxResult saveInvoice(@RequestBody Map<String, Object> form) {

		AjaxResult result = new AjaxResult();
		// 1. 기본 키 생성
		String misdate = sanitizeNumericString(form.get("writeDate"));
		String misnum = generateMisnum(misdate);
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

//		salesment.setSpjangcd((String) form.get("spjangcd")); // sessionStorage에서 넘겨받은 값

		// 3. 상세 목록 매핑
		List<TB_SalesDetail> details = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			String prefix = "detailList[" + i + "]";
			String itemName = (String) form.get(prefix + ".ItemName");

			if (itemName == null || itemName.trim().isEmpty()) continue;

			TB_SalesDetail detail = new TB_SalesDetail();
			detail.setId(new TB_SalesDetailId(
					misdate,
					misnum,
					form.get(prefix + ".SerialNum") != null ? form.get(prefix + ".SerialNum").toString() : String.format("%03d", i + 1)
			));
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

	@GetMapping("/invoice_detail")
	public AjaxResult getInvoiceDetail(
			@RequestParam("misdate") String misdate,
			@RequestParam("misnum") String misnum,
			HttpServletRequest request) {
		misdate = misdate.replaceAll("-", "");

		Map<String, Object> item = this.salesInvoiceService.getInvoiceDetail(misdate, misnum);

		AjaxResult result = new AjaxResult();
		result.data = item;

		return result;
	}

	@PostMapping("/invoice_delete")
	@Transactional
	public AjaxResult deleteSalesment(@RequestBody List<Map<String, String>> deleteList) {
		AjaxResult result = new AjaxResult();

		if (deleteList == null || deleteList.isEmpty()) {
			result.success = false;
			result.message = "삭제할 데이터가 없습니다.";
			return result;
		}

		List<String> keyList = deleteList.stream()
				.map(item -> item.get("misdate") + "_" + item.get("misnum"))
				.collect(Collectors.toList());

		tb_salesDetailRepository.deleteByKeyList(keyList);
		tb_salesmentRepository.deleteByKeyList(keyList);

		result.success = true;
		return result;
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
		if (obj == null || obj.toString().trim().isEmpty()) return 0;
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
