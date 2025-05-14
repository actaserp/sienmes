package mes.app.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.popbill.api.*;
import mes.app.aop.DecryptField;
import mes.app.transaction.service.SalesInvoiceService;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.repository.MaterialRepository;
import mes.domain.repository.TB_SalesDetailRepository;
import mes.domain.repository.TB_SalesmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/tran/tran")
public class SalesInvoiceController {
	
	@Autowired
	private SalesInvoiceService salesInvoiceService;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private MaterialRepository materialRepository;

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

	@GetMapping("/get_material")
	public AjaxResult getMaterialName(
			@RequestParam("material_id") Integer id
	) {

		Material item = materialRepository.getMaterialById(id);

		String spec = Stream.of(
						item.getWidth() != null ? "폭:" + formatNumber(item.getWidth()) : null,
						item.getLength() != null ? "길이:" + formatNumber(item.getLength()) : null,
						item.getHeight() != null ? "높이:" + formatNumber(item.getHeight()) : null,
						item.getThickness() != null ? "두께:" + formatNumber(item.getThickness()) : null
				).filter(Objects::nonNull)
				.collect(Collectors.joining(" × "));

		Map<String, Object> data = new HashMap<>();
		data.put("material_name", item.getName());
		data.put("spec", spec);

		AjaxResult result = new AjaxResult();
		result.data = data;
		return result;
	}

	private String formatNumber(Number number) {
		return new DecimalFormat("###,###.##").format(number);
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
	@PostMapping("/invoicee_check")
	public AjaxResult invoiceeCheck(
			@RequestParam("b_no") String bno,
			@RequestParam("compid") Integer compid,
			Authentication auth) {

		AjaxResult result = new AjaxResult();

		try {
			JsonNode data = salesInvoiceService.validateSingleBusiness(bno);

			String statusCode = data.path("b_stt_cd").asText();
			String statusText = data.path("b_stt").asText();
			String taxTypeText = data.path("tax_type").asText();

			if ("01".equals(statusCode)) {
				result.success = true;
				result.data = data; // 단건 결과 JSON 문자열로 반환
			} else {
				Company company = companyRepository.getCompanyById(compid);
				company.setRelyn("1");
				companyRepository.save(company);

				if (statusText == null || statusText.isBlank()) {
					result.success = false;
					result.message = taxTypeText + "\n거래중지 처리되었습니다.";
				} else {
					result.success = false;
					result.message = "사업자 상태: " + statusText + " 거래중지 처리되었습니다.";
				}
			}

		} catch (Exception e) {
			result.success = false;
			result.message = "사업자 진위 확인 실패: " + e.getMessage();
		}

		return result;
	}

	// 공급받는자 저장
	@PostMapping("/invoicee_save")
	public AjaxResult invoiceeSave(
			@RequestParam Map<String, String> paramMap,
			Authentication auth) {

		User user = (User) auth.getPrincipal();
		return salesInvoiceService.saveInvoicee(paramMap, user);
	}

	// 검색
	@DecryptField(columns = {"ivercorpnum"}, masks = 3)
	@GetMapping("/read")
	public AjaxResult getInvoiceList(
			@RequestParam(value="invoice_kind", required=false) String invoice_kind,
			@RequestParam(value="start", required=false) String start_date,
			@RequestParam(value="end", required=false) String end_date,
			@RequestParam(value="cboCompany", required=false) Integer cboCompany,
			@RequestParam(value="cboStatecode", required=false) Integer cboStatecode,
			HttpServletRequest request) {

		start_date = start_date + " 00:00:00";
		end_date = end_date + " 23:59:59";

		Timestamp start = Timestamp.valueOf(start_date);
		Timestamp end = Timestamp.valueOf(end_date);

		List<Map<String, Object>> items = this.salesInvoiceService.getList(invoice_kind, cboStatecode, cboCompany, start, end);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	// 세금계산서 저장
	@PostMapping("/invoice_save")
	public AjaxResult saveInvoice(@RequestBody Map<String, Object> form) {

        return salesInvoiceService.saveInvoice(form);
	}

	@PostMapping("/invoice_issue")
	public AjaxResult issueInvoice(@RequestBody List<Map<String, String>> issueList) {

		return salesInvoiceService.issueInvoice(issueList);
	}

	@GetMapping("/invoice_detail")
	public AjaxResult getInvoiceDetail(
			@RequestParam("misnum") Integer misnum,
			HttpServletRequest request) throws IOException {

		Map<String, Object> item = this.salesInvoiceService.getInvoiceDetail(misnum);

		AjaxResult result = new AjaxResult();
		result.data = item;

		return result;
	}

	@PostMapping("/invoice_delete")
	public AjaxResult deleteSalesment(@RequestBody List<Map<String, String>> deleteList) {

		return salesInvoiceService.deleteSalesment(deleteList);
	}



}
