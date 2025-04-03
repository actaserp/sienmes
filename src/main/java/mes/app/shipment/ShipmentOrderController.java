package mes.app.shipment;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.shipment.service.ShipmentOrderService;
import mes.domain.entity.RelationData;
import mes.domain.entity.Shipment;
import mes.domain.entity.ShipmentHead;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.RelationDataRepository;
import mes.domain.repository.ShipmentHeadRepository;
import mes.domain.repository.ShipmentRepository;
import mes.domain.services.CommonUtil;

@RestController
@RequestMapping("/api/shipment/shipment_order")
public class ShipmentOrderController {

	@Autowired 
	private ShipmentOrderService shipmentOrderService;
	
	@Autowired
	ShipmentRepository shipmentRepository;
	
	@Autowired
	ShipmentHeadRepository shipmentHeadRepository;
	
	@Autowired
	RelationDataRepository relationDataRepository;

	@Autowired
	TransactionTemplate transactionTemplate;
	
	@GetMapping("/suju_list")
	public AjaxResult getSujuList(
			@RequestParam("srchStartDt") String dateFrom,
			@RequestParam("srchEndDt") String dateTo,
			@RequestParam("not_ship") String notShip,
			@RequestParam("cboCompany") String compPk,
			@RequestParam("cboMatGroup") String matGrpPk,
			@RequestParam("cboMaterial") String matPk,
			@RequestParam("keyword") String keyword ){
		
		List<Map<String, Object>> items = this.shipmentOrderService.getSujuList(dateFrom,dateTo,notShip,compPk,matGrpPk,matPk,keyword);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}

	@GetMapping("/product_list")
	public AjaxResult getProductList(
			@RequestParam("cboMatGroup") String matGrpPk,
			@RequestParam("cboMaterial") String matPk,
			@RequestParam("keyword") String keyword){
		
		List<Map<String, Object>> items = this.shipmentOrderService.getProductList(matGrpPk,matPk,keyword);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}
	
	@PostMapping("/save_shipment_order")
	@Transactional
	public AjaxResult saveShipmentOrder(
			@RequestParam("Company_id") Integer CompanyId,
			@RequestParam("Description") String Description,
			@RequestParam("ShipDate") String Ship_date,
			@RequestBody MultiValueMap<String,Object> Q,
			@RequestParam("TableName") String TableName,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		Timestamp today = new Timestamp(System.currentTimeMillis());  //shipment_head의 OrderDate 컬럼값 yyyy-MM-dd
		Timestamp shipDate = CommonUtil.tryTimestamp(Ship_date); //shipment_head의 ShipDate 컬럼값 yyyy-MM-dd
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
		ShipmentHead smh = new ShipmentHead();
		
		smh.setCompanyId(CompanyId);
		smh.setShipDate(shipDate);
		smh.setOrderDate(today);
		smh.setDescription(Description);
		smh.set_audit(user);
		smh.setState("ordered");
		
		smh = this.shipmentHeadRepository.save(smh);
		
		int orderSum = 0;
		for(int i = 0; i < data.size(); i++) {
			Shipment sm = new Shipment();
			int orderQty =  Integer.parseInt(data.get(i).get("order_qty").toString());
			if (orderQty <=  0) {
				continue;
			}
			sm.setShipmentHeadId(smh.getId());
			sm.setMaterialId((int)data.get(i).get("mat_id"));
			sm.setOrderQty((float)orderQty);
			sm.setQty((float) 0);
			if (data.get(i).get("description") != null) {
			sm.setDescription((String)data.get(i).get("description"));
			}
			
			if(TableName.equals("product")) {
				sm.setSourceDataPk((int)data.get(i).get("suju_pk"));
				sm.setSourceTableName(TableName);
			} else if (TableName.equals("suju")) {
				sm.setSourceTableName("rela_data");
			}
			sm.set_audit(user);
			sm = this.shipmentRepository.save(sm);
			if(TableName.equals("suju")) {
				String[] sujuData = data.get(i).get("suju_pk").toString().split(",");
				for(String sujuItem: sujuData) {
					RelationData rd = new RelationData();
					String[] foo = sujuItem.split(":");
					String sujuPk = foo[0].substring(1);
					String sujuOrderQty = foo[1];
					rd.setTableName1("suju");
					rd.setDataPk1(Integer.parseInt(sujuPk));
					rd.setTableName2("shipment");
					rd.setDataPk2(sm.getId());
					rd.set_audit(user);
					rd.setRelationName("");
					rd.setNumber1(Integer.parseInt(sujuOrderQty));
					rd = this.relationDataRepository.save(rd);
				}
			}
			orderSum += orderQty; 
		}
		smh.setTotalQty((float)orderSum);
		
		smh = this.shipmentHeadRepository.save(smh);
		
		result.data = smh;
		
		return result;
	}
		

	// 출하지시 목록 조회
	@GetMapping("/order_list")
	public AjaxResult getShipmentOrderList(
			@RequestParam(value="srchStartDt", required=false) String date_from, 
			@RequestParam(value="srchEndDt", required=false) String date_to,
			@RequestParam(value="chkNotShipped", required=false) String not_ship, 
			@RequestParam(value="cboCompany", required=false) Integer comp_pk,
			@RequestParam(value="cboMatGroup", required=false) Integer mat_grp_pk, 
			@RequestParam(value="cboMaterial", required=false) Integer mat_pk,
			@RequestParam(value="keyword", required=false) String keyword,
			HttpServletRequest request) {
			
		String state = "";
		if("Y".equals(not_ship)) {
			state= "ordered";
		} else {
			state = "";
		}
		
		List<Map<String, Object>> items = this.shipmentOrderService.getShipmentOrderList(date_from, date_to, state, comp_pk, mat_grp_pk, mat_pk, keyword);
        AjaxResult result = new AjaxResult();
        result.data = items;
		return result;
	}
	
	// 출하 품목 목록 조회
	@GetMapping("/shipment_item_list")
	public AjaxResult getShipmentItemList(
			@RequestParam(value="head_id", required=false) Integer head_id,
			HttpServletRequest request) {
		List<Map<String, Object>> items = this.shipmentOrderService.getShipmentItemList(head_id);
        AjaxResult result = new AjaxResult();
        result.data = items;
		return result;
	}
	
	// 출하일 변경
	@PostMapping("/update_ship_date")
	public AjaxResult updateShipDate(
			@RequestParam(value="head_id", required=false) Integer head_id,
			@RequestParam(value="ship_date", required=false) String ship_date,
			HttpServletRequest request,
			Authentication auth) {
		
        AjaxResult result = new AjaxResult();
		User user = (User)auth.getPrincipal();
		
		ShipmentHead shipmentHead = this.shipmentHeadRepository.getShipmentHeadById(head_id);

		if ("shipped".equals(shipmentHead.getState())) {		//if (shipmentHead.getState().equals("shipped")) {
			result.success = false;
		} else {
			shipmentHead.setShipDate(CommonUtil.tryTimestamp(ship_date));
			shipmentHead.set_audit(user);
			
			shipmentHead = this.shipmentHeadRepository.save(shipmentHead);
			
			result.data = shipmentHead;
		}
		
		return result;
	}
	
	// 출하지시 취소
	@PostMapping("/cancel_order")
	public AjaxResult cancelOrder(
			@RequestParam(value="shipmenthead_id", required=false) Integer head_id,
			HttpServletRequest request,
			Authentication auth) {
		
        AjaxResult result = new AjaxResult();
		
		ShipmentHead head = this.shipmentHeadRepository.getShipmentHeadById(head_id);

		if ("shipped".equals(head.getState())) {
			result.success = false;
		} else {
			this.transactionTemplate.executeWithoutResult(status->{			
				try {
					
					this.shipmentRepository.deleteByShipmentHeadId(head_id);
					this.shipmentHeadRepository.deleteById(head_id);
				}
				catch(Exception ex) {
					TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
					result.success=false;
					result.message = ex.toString();
				}				
			});					
		}
		return result;
	}
}
