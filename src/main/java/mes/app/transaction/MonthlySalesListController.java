package mes.app.transaction;

import lombok.extern.slf4j.Slf4j;
import mes.app.transaction.service.MonthlySalesListService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transaction/MonthlySalesList")
public class MonthlySalesListController {

  @Autowired
  MonthlySalesListService monthlySalesListService;

  @GetMapping("/SalesRead")
  public AjaxResult getMonthlySalesList(
      @RequestParam(value="cboYear",required=false) String cboYear,
      @RequestParam(value="cboCompany",required=false) Integer cboCompany,
      @RequestParam(value = "spjangcd") String spjangcd
  ) {
    //log.info("월별 매출현황(매출)read : cboYear:{}, cboCompany:{} , spjangcd:{}", cboYear, cboCompany,spjangcd);
    List<Map<String,Object>> items = this.monthlySalesListService.getSalesList(cboYear,cboCompany, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/DepositRead")
  public AjaxResult getMonthDepositList(
      @RequestParam(value="cboYear",required=false) String cboYear,
      @RequestParam(value="cboCompany",required=false) Integer cboCompany,
      @RequestParam(value = "spjangcd") String spjangcd
  ) {
    //log.info("월별 매출현황(입금) read : cboYear:{}, cboCompany:{} , spjangcd:{} ", cboYear, cboCompany, spjangcd);
    List<Map<String,Object>> items = this.monthlySalesListService.getMonthDepositList(cboYear,cboCompany, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/ReceivableRead")
  public AjaxResult getMonthReceivableList(
      @RequestParam(value="cboYear",required=false) String cboYear,
      @RequestParam(value="cboCompany",required=false) Integer cboCompany
  ) {
   //log.info("월별 매출현황(미수금) read : cboYear:{}, cboCompany:{} ", cboYear, cboCompany);
    List<Map<String,Object>> items = this.monthlySalesListService.getMonthReceivableList(cboYear,cboCompany);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

}
