package mes.app.transaction;

import mes.app.transaction.service.MonthlyPurchaseListService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction/monthly_purchase_list")
public class MonthlyPurchaseListController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    MonthlyPurchaseListService monthlyPurchaseListService;

    @GetMapping("/PurchaseDetails")
    public AjaxResult getMonthlyPurchaseList(
        @RequestParam(value="cboYear",required=false) String cboYear,
        @RequestParam(value="cboCompany",required=false) Integer cboCompany,
        @RequestParam(value = "spjangcd") String spjangcd
    ) {
        //log.info("월별 매입현황(매입)read : cboYear:{}, cboCompany:{} , spjangcd:{}", cboYear, cboCompany,spjangcd);
        List<Map<String,Object>> items = this.monthlyPurchaseListService.getMonthDepositList(cboYear,cboCompany, spjangcd);

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }

    @GetMapping("/ProvisionRead")
    public AjaxResult getMonthDepositList(
        @RequestParam(value="cboYear",required=false) String cboYear,
        @RequestParam(value="cboCompany",required=false) Integer cboCompany,
        @RequestParam(value = "spjangcd") String spjangcd
    ) {
        //log.info("월별 매입현황(지급) read : cboYear:{}, cboCompany:{} , spjangcd:{} ", cboYear, cboCompany, spjangcd);
        List<Map<String,Object>> items = this.monthlyPurchaseListService.getProvisionList(cboYear,cboCompany, spjangcd);

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }

    @GetMapping("/PaymentRead")
    public AjaxResult getMonthReceivableList(
        @RequestParam(value="cboYear",required=false) String cboYear,
        @RequestParam(value="cboCompany",required=false) Integer cboCompany,
        @RequestParam(value = "spjangcd") String spjangcd
    ) {
        //log.info("월별 매입현황(미지급금) read : cboYear:{}, cboCompany:{} , spjangcd:{} ", cboYear, cboCompany,spjangcd);
        List<Map<String,Object>> items = this.monthlyPurchaseListService.getPaymentList(cboYear,cboCompany, spjangcd);

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }

}
