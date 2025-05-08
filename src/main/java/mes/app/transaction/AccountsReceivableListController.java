package mes.app.transaction;

import lombok.extern.slf4j.Slf4j;
import mes.app.transaction.service.AccountsReceivableListServie;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transaction/AccRList")
public class AccountsReceivableListController {

  @Autowired
  AccountsReceivableListServie accountsReceivableListServie;

  // 미수금 현황 집계
  @GetMapping("/TotalList")
  public AjaxResult getTotalList(
      @RequestParam(value="srchStartDt", required=false) String start_date,
      @RequestParam(value="srchEndDt", required=false) String end_date,
      @RequestParam(value = "cboCompany", required=false) Integer company,
      HttpServletRequest request) {
    //log.info("미수금 현황 집계 read ---  :start:{}, end:{} ,company:{} ", start_date, end_date, company);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.accountsReceivableListServie.getTotalList(start, end, company);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }


  // 미수금 현황 상세
  @GetMapping("/DetailList")
  public AjaxResult getDetailList(
      @RequestParam(value="srchStartDt", required=false) String start_date,
      @RequestParam(value="srchEndDt", required=false) String end_date,
      @RequestParam(value = "code", required=false) String company,
      HttpServletRequest request) {
    log.info("미수금 현황 상세 read ---  :start:{}, end:{} ,company:{} ", start_date, end_date, company);

    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.accountsReceivableListServie.getDetailList(start, end, company);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

}
