package mes.app.transaction;

import lombok.extern.slf4j.Slf4j;
import mes.app.transaction.service.ExpenseAccountSetupService;
import mes.domain.entity.TB_CA648Id;
import mes.domain.entity.TB_CA648;
import mes.domain.model.AjaxResult;
import mes.domain.repository.TB_ca648Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/transaction/ExpenseAccountSetup")
public class ExpenseAccountSetupController {

  @Autowired
  ExpenseAccountSetupService accountSetupService;

  @Autowired
  TB_ca648Repository tb_ca648Repository;

  @GetMapping("/read")
  public AjaxResult getExpenseAccountList(@RequestParam(value ="spjangcd") String spjangcd) {
    log.info("비용항목 등록 read - spjangcd:{}",spjangcd);

    List<Map<String, Object>> items = this.accountSetupService.getExpenseAccountList(spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  @GetMapping("/readDetail")
  public AjaxResult getExpenseAccountDetail(@RequestParam(value ="groupCode") String groupCode) {
    log.info("비용항목 상세 - groupCode:{}",groupCode);

    List<Map<String, Object>> items = this.accountSetupService.getExpenseAccountDetail(groupCode);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  //저장
  @PostMapping("/save")
  public AjaxResult saveExpenseItems(@RequestBody Map<String, Object> payload) {
    AjaxResult result = new AjaxResult();

    log.info("수신된 전체 payload: {}", payload);

    String spjangcd = (String) payload.get("spjangcd");
    List<Map<String, Object>> details = (List<Map<String, Object>>) payload.get("details");

    log.info("spjangcd: {}", spjangcd);
    log.info("상세 항목 건수: {}", details.size());

    for (Map<String, Object> row : details) {
      String gartcd = (String) row.get("gartcd");
      String artcd = (String) row.get("artcd");

      log.info("처리 중 - gartcd: {}, artcd: {}", gartcd, artcd);

      TB_CA648Id id = new TB_CA648Id(spjangcd, gartcd, artcd);
      Optional<TB_CA648> optional = tb_ca648Repository.findById(id);

      if (optional.isPresent()) {
        log.info("기존 데이터 존재 - 수정");
        TB_CA648 existing = optional.get();
        existing.setArtnm((String) row.get("artnm"));
        existing.setJiflag((String) row.get("jiflag"));
        tb_ca648Repository.save(existing);
      } else {
        log.info("신규 데이터 - 등록");
        TB_CA648 newItem = new TB_CA648();
        newItem.setId(id);
        newItem.setArtnm((String) row.get("artnm"));
        newItem.setJiflag((String) row.get("jiflag"));
        tb_ca648Repository.save(newItem);
      }
    }

    result.success = true;
    result.message = "저장 완료";
    log.info("저장 완료 처리 완료");
    return result;
  }

}
