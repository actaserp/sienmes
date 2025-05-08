package mes.app.transaction;

import mes.app.transaction.service.SalesInvoiceService;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.repository.TB_SalesDetailRepository;
import mes.domain.repository.TB_SalesmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tran/tran")
public class SalesInvoiceController {
	
	@Autowired
	private SalesInvoiceService salesInvoiceService;
	
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
	public AjaxResult invoiceeSave(
			@RequestParam Map<String, String> paramMap,
			Authentication auth) {

		User user = (User) auth.getPrincipal();
		return salesInvoiceService.saveInvoicee(paramMap, user);
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
		return salesInvoiceService.saveInvoice(form);
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
	public AjaxResult deleteSalesment(@RequestBody List<Map<String, String>> deleteList) {
		return salesInvoiceService.deleteSalesment(deleteList);
	}



}
