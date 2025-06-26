package mes.app.balju;

import lombok.extern.slf4j.Slf4j;
import mes.app.balju.service.BaljuOrderService;
import mes.domain.entity.Balju;
import mes.domain.entity.BaljuHead;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.BalJuHeadRepository;
import mes.domain.repository.BujuRepository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/balju/balju_order")
public class BaljuOrderController {

  @Autowired
  BaljuOrderService baljuOrderService;

  @Autowired
  BujuRepository bujuRepository;

  @Autowired
  BalJuHeadRepository balJuHeadRepository;

  // 발주 목록 조회
  @GetMapping("/read")
  public AjaxResult getSujuList(
      @RequestParam(value="date_kind", required=false) String date_kind,
      @RequestParam(value="start", required=false) String start_date,
      @RequestParam(value="end", required=false) String end_date,
      @RequestParam(value ="spjangcd") String spjangcd,
      HttpServletRequest request) {
    //log.info("발주 read--- date_kind:{}, start_date:{},end_date:{} , spjangcd:{} " ,date_kind,start_date , end_date, spjangcd);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  // 발주 등록
  @PostMapping("/multi_save")
  @Transactional
  public AjaxResult saveBaljuMulti(@RequestBody Map<String, Object> payload, Authentication auth) {
//    log.info("발주등록 들어옴");
//    log.info("📦 payload keys: {}", payload.keySet());  // items가 포함되어야 함
//    log.info("🧾 items 내용: {}", payload.get("items"));
    User user = (User) auth.getPrincipal();

    // 기본 정보 추출
    String jumunDateStr = (String) payload.get("JumunDate");
    String dueDateStr = (String) payload.get("DueDate");
    Integer companyId = Integer.parseInt(payload.get("Company_id").toString());
    String CompanyName = (String) payload.get("CompanyName");
    String spjangcd = (String) payload.get("spjangcd");
    String isVat = (String) payload.get("invatyn");
    String specialNote = (String) payload.get("special_note");
    String sujuType = (String) payload.get("SujuType") ;

    Date jumunDate = CommonUtil.trySqlDate(jumunDateStr);
    Date dueDate = CommonUtil.trySqlDate(dueDateStr);

    Integer headId = CommonUtil.tryIntNull(payload.get("id")); // 발주 헤더 ID

    BaljuHead head;

    if (headId != null) {
      //log.info("🔄 기존 발주 수정 - headId: {}", headId);
      head = balJuHeadRepository.findById(headId).orElseThrow(() -> new RuntimeException("발주 헤더 없음"));

    } else {
      //log.info("신규 발주 생성");
      head = new BaljuHead();
      head.setCreated(new Timestamp(System.currentTimeMillis()));
      head.setCreaterId(user.getId());
      head.set_status("manual");
      String jumunNumber = baljuOrderService.makeJumunNumber(jumunDate);
      head.setJumunNumber(jumunNumber);
    }

    // 공통 필드 설정
    head.setSujuType(sujuType);
    head.setJumunDate(jumunDate);
    head.setDeliveryDate(dueDate);
    head.setCompanyId(companyId);
    head.setSpjangcd(spjangcd);
    head.setSpecialNote(specialNote);

    balJuHeadRepository.save(head);
    //log.info("✅ BaljuHead 저장 완료 - ID: {}", head.getId());

    // 하위 품목 저장
    List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
    double totalPriceSum = 0;

    for (Map<String, Object> item : items) {
      Integer baljuId = CommonUtil.tryIntNull(item.get("id")); // id가 전달되면 수정

      Integer materialId = Integer.parseInt(item.get("Material_id").toString());
      Double qty = Double.parseDouble(item.get("quantity").toString());
      Double unitPrice = Double.parseDouble(item.get("unit_price").toString());
      Double totalAmount = Double.parseDouble(item.get("total_price").toString());
      Double supply_price = Double.parseDouble(item.get("supply_price").toString());
      Double vat = Double.parseDouble(item.get("vat").toString());

      Balju detail;

      if (baljuId != null) {
        // 수정인 경우
        detail = bujuRepository.findById(baljuId)
            .orElseThrow(() -> new RuntimeException("상세 항목 없음"));
        detail._modified = new Timestamp(System.currentTimeMillis());
        detail._modifier_id = user.getId(); // 로그인한 사용자 ID
      } else {
        // 신규 등록인 경우
        detail = new Balju();
        detail._created = new Timestamp(System.currentTimeMillis());
        detail._creater_id = user.getId();
        detail.setBaljuHeadId(head.getId());
        detail.setJumunNumber(head.getJumunNumber());
      }

      // 공통 필드 세팅
      detail.setMaterialId(materialId);
      detail.setCompanyId(companyId);
      detail.setCompanyName(CompanyName);
      detail.setSujuQty(qty);
      detail.setUnitPrice(unitPrice);
      detail.setPrice(supply_price);
      detail.setVat(vat);
      detail.setTotalAmount(totalAmount);
      detail.setDescription(CommonUtil.tryString(item.get("description")));
      detail.setSpjangcd(spjangcd);
      detail.setJumunDate(jumunDate);
      detail.setDueDate(dueDate);
      detail.setInVatYN("Y".equalsIgnoreCase(isVat) ? "Y" : "N");
      detail.setSujuType(sujuType);
      detail.setState("draft");
      detail.setSujuQty2(0.0d);
      detail.set_status("manual");

      totalPriceSum += detail.getTotalAmount();
      bujuRepository.save(detail);
    }

    head.setTotalPrice(totalPriceSum);
    balJuHeadRepository.save(head);

    AjaxResult result = new AjaxResult();
    result.data = Map.of("headId", head.getId(), "totalPrice", totalPriceSum);
    return result;
  }

  // 발주 상세정보 조회
  @GetMapping("/detail")
  public AjaxResult getBaljuDetail(
      @RequestParam("id") int id,
      HttpServletRequest request) {
//    log.info("상세 정보 들어옴 : id:{}", id);
    Map<String, Object> item = this.baljuOrderService.getBaljuDetail(id);

    AjaxResult result = new AjaxResult();
    result.data = item;

    return result;
  }

  // 발주 삭제
  @PostMapping("/delete")
  @Transactional
  public AjaxResult deleteSuju(
      @RequestParam("id") Integer id,
      @RequestParam("State") String State) {

    AjaxResult result = new AjaxResult();

    if (!"draft".equalsIgnoreCase(State)) {
      result.success = false;
      result.message = "미입고 상태일 때만 삭제할 수 있습니다.";
      return result;
    }

    Optional<BaljuHead> optionalHead = balJuHeadRepository.findById(id);
    if (!optionalHead.isPresent()) {
      result.success = false;
      result.message = "해당 발주 정보가 존재하지 않습니다.";
      return result;
    }

    BaljuHead head = optionalHead.get();

    // 1. 기준 정보 추출
    String jumunNumber = head.getJumunNumber();
    Date jumunDate = head.getJumunDate();
    String spjangcd = head.getSpjangcd();

    // 2. 해당 기준으로 balju 삭제
    bujuRepository.deleteByJumunNumberAndJumunDateAndSpjangcd(jumunNumber, jumunDate, spjangcd);

    // 3. balju_head 삭제
    balJuHeadRepository.deleteById(id);

    result.success = true;
    return result;
  }


  //중지 처리
  @PostMapping("/balju_stop")
  public AjaxResult balju_stop(@RequestParam(value="id", required=false) Integer id){

    List<Map<String, Object>> items = this.baljuOrderService.balju_stop(id);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  //단가 찾기
  @GetMapping("/price")
  public AjaxResult BaljuPrice(@RequestParam("mat_pk") int materialId,
                               @RequestParam("JumunDate") String jumunDate,
                               @RequestParam("company_id") int companyId){
    //log.info("발주단가 찾기 --- matPk:{}, ApplyStartDate:{},company_id:{} ",materialId,jumunDate , companyId);
    List<Map<String, Object>> items = this.baljuOrderService.getBaljuPrice(materialId, jumunDate, companyId);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @PostMapping("/savePrice")
  public AjaxResult savePriceByMat(@RequestBody Map<String, Object> data) {
    AjaxResult result = new AjaxResult();

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      User user = (User) auth.getPrincipal();
      data.put("user_id", user.getId());

      int saveCount = this.baljuOrderService.saveCompanyUnitPrice(data);

      if (saveCount > 0) {
        result.success = true;
      } else {
        result.success = false;
        result.message = "저장 실패: 중복된 데이터이거나 입력값이 올바르지 않습니다.";
      }
    } catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }

    return result;
  }


}
