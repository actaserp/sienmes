package mes.app.transaction;

import mes.app.transaction.service.SalesInvoiceService;
import mes.domain.entity.Company;
import mes.domain.entity.Suju;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.CompanyRepository;
import mes.domain.services.CommonUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tran/tran")
public class SalesInvoiceController {
	
	@Autowired
	private SalesInvoiceService salesInvoiceService;

	@Autowired
	private CompanyRepository companyRepository;
	
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



}
