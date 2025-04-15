package mes.app.sales;

import java.util.List;
import java.util.Map;
import java.sql.Date;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.sales.service.SujuService;
import mes.domain.entity.Suju;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.SujuRepository;
import mes.domain.services.CommonUtil;

@RestController
@RequestMapping("/api/sales/suju")
public class SujuController {

	@Autowired
	SujuRepository SujuRepository;
	
	@Autowired
	private SujuService sujuService;
	
	// 수주 목록 조회 
	@GetMapping("/read")
	public AjaxResult getSujuList(
			@RequestParam(value="date_kind", required=false) String date_kind,
			@RequestParam(value="start", required=false) String start_date,
			@RequestParam(value="end", required=false) String end_date,
			HttpServletRequest request) {
		
		start_date = start_date + " 00:00:00";
		end_date = end_date + " 23:59:59";
		
		Timestamp start = Timestamp.valueOf(start_date);
		Timestamp end = Timestamp.valueOf(end_date);
		
		List<Map<String, Object>> items = this.sujuService.getSujuList(date_kind, start, end);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}
	
	// 수주 상세정보 조회
	@GetMapping("/detail")
	public AjaxResult getSujuDetail(
			@RequestParam("id") int id,
			HttpServletRequest request) {
		Map<String, Object> item = this.sujuService.getSujuDetail(id);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		
		return result;
	}
	
	// 제품 정보 조회
	@GetMapping("/product_info")
	public AjaxResult getSujuMatInfo(
			@RequestParam("product_id") int id,
			HttpServletRequest request) {
		Map<String, Object> item = this.sujuService.getSujuMatInfo(id);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		
		return result;
	}
	
	// 수주 등록 
	@PostMapping("/manual_save")
	public AjaxResult SujuSave(
			@RequestParam(value="id", required=false) Integer id,
			@RequestParam(value="SujuQty") Integer sujuQty,
			@RequestParam(value="Company_id") Integer companyId,
			@RequestParam(value="CompanyName") String companyName,
			@RequestParam(value="Description", required=false) String description,
			@RequestParam(value="DueDate") String dueDate,
			@RequestParam(value="JumunDate") String jumunDate,
			@RequestParam(value="Material_id") Integer materialId,
			@RequestParam(value="AvailableStock", required=false) Float availableStock,
			@RequestParam(value="SujuType") String sujuType,
			@RequestParam(value="unitPrice") String unitPriceStr,
			@RequestParam(value="price") String priceStr,
			@RequestParam(value="vat") String vatStr,
			HttpServletRequest request,
			Authentication auth	) {
		User user = (User)auth.getPrincipal();
		
		Suju suju = null;
		
		if (id!=null) {
			suju = this.SujuRepository.getSujuById(id);
		}else {
			suju = new Suju();
			suju.setState("received");
		}
		
		availableStock = availableStock==null?0:availableStock;
		Date due_Date = CommonUtil.trySqlDate(dueDate);
		Date jumun_Date = CommonUtil.trySqlDate(jumunDate);
		int unitPrice = Integer.parseInt(unitPriceStr.replace(",", ""));
		int price = Integer.parseInt(priceStr.replace(",", ""));
		int vat = "면세".equals(vatStr) ? 0 : Integer.parseInt(vatStr.replace(",", ""));
		
		suju.setSujuQty(sujuQty);
		suju.setSujuQty2(sujuQty);
		suju.setCompanyId(companyId);
		suju.setCompanyName(companyName);
		suju.setDescription(description);
		suju.setDueDate(due_Date);
		suju.setJumunDate(jumun_Date);
		suju.setMaterialId(materialId);
		suju.setAvailableStock(availableStock); // 없으면 0으로 보내기 추가
		suju.setSujuType(sujuType);
		suju.set_status("manual");
		suju.set_audit(user);
		suju.setUnitPrice(unitPrice);
		suju.setPrice(price);
		suju.setVat(vat);
		
		suju = this.SujuRepository.save(suju);
		
		AjaxResult result = new AjaxResult();
		result.data=suju;
		return result;
	}
	
	// 수주 삭제
	@PostMapping("/delete")
	public AjaxResult deleteSuju(
			@RequestParam("id") Integer id,
			@RequestParam("State") String State,
			@RequestParam("ShipmentStateName") String ShipmentStateName) {
		
		AjaxResult result = new AjaxResult();
		
		if (State.equals("received")==false) {
			//received 아닌것만
			result.success = false;
			result.message = "수주상태만 삭제할 수 있습니다";
			return result;
		}
		if (ShipmentStateName != null && !ShipmentStateName.isEmpty()) {
			result.success = false;
			result.message = "출하된 수주는 삭제할 수 없습니다";
			return result;
		}
		
		this.SujuRepository.deleteById(id);
		
		return result;
	}

	// 단가 정보 가져오기
	@GetMapping("/readPriceSuju")
	public AjaxResult getPriceHistory(@RequestParam("mat_pk") int matPk,
									  @RequestParam("company_id") int company_id,
									  @RequestParam("JumunDate") String ApplyStartDate) {

		List<Map<String, Object>> items = this.sujuService.getPriceByMatAndComp(matPk, company_id, ApplyStartDate);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}
	
	
}
