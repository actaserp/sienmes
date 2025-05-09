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

@RestController
@RequestMapping("/api/transaction/monthly_purchase_list")
public class MonthlyPurchaseListController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    MonthlyPurchaseListService monthlyPurchaseListService;

    // 지급현황 리스트 조회
    @GetMapping("/read")
    public AjaxResult getEquipmentRunChart(
            @RequestParam(value="cboYear", required=false) String cboYear,
            @RequestParam(value="cboCompany", required=false) Integer cboCompany,
            @RequestParam(value = "spjangcd") String spjangcd,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = monthlyPurchaseListService.getPurchaseList(cboYear, cboCompany, spjangcd);

        return result;
    }
}
