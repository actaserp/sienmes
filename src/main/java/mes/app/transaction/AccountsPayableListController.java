package mes.app.transaction;

import mes.app.transaction.service.AccountsPayableListService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transaction/accounts_payable_list")
public class AccountsPayableListController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    AccountsPayableListService accountsPayableListService;

    // 미지급현황 리스트 조회
    @GetMapping("/read")
    public AjaxResult getEquipmentRunChart(
            @RequestParam(value="date_from", required=false) String date_from,
            @RequestParam(value="date_to", required=false) String date_to,
            @RequestParam(value="companyCode", required=false) Integer companyCode,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = accountsPayableListService.getPayableList(date_from, date_to, companyCode);

        return result;
    }

    // 미지급현황 상세 리스트 조회
    @GetMapping("/detail")
    public AjaxResult getEquipmentRunChartDetail(
            @RequestParam(value="date_from", required=false) String date_from,
            @RequestParam(value="date_to", required=false) String date_to,
            @RequestParam(value="companyCode", required=false) Integer companyCode,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = accountsPayableListService.getPayableDetailList(date_from, date_to, companyCode);

        return result;
    }
}
