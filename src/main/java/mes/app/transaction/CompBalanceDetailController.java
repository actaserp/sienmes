package mes.app.transaction;

import lombok.extern.slf4j.Slf4j;
import mes.app.transaction.service.CompBalanceDetailServicr;
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
@RequestMapping("/api/transaction/CompBalanceDetail")
public class CompBalanceDetailController {

  @Autowired
  CompBalanceDetailServicr compBalanceDetailServicr;

  // 거래처잔액 명세(입금)
  @GetMapping("/read")
  public AjaxResult getList(
      @RequestParam(value="srchStartDt", required=false) String start_date,
      @RequestParam(value="srchEndDt", required=false) String end_date,
      @RequestParam(value = "cboCompany", required=false) String company,
      HttpServletRequest request) {
    //log.info("거래처잔액 명세(입금) read ---  :start:{}, end:{} ,company:{} ", start_date, end_date, company);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.compBalanceDetailServicr.getList(start, end, company);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }
}
