package mes.app.transaction;

import mes.app.transaction.Service.PaymentListService;
import mes.app.transaction.Service.VendorBalanceDetailService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transaction/vendor_balance_detail")
public class VendorBalanceDetailController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    VendorBalanceDetailService vendorBalanceDetailService;

    // 거래처별 잔액 명세 리스트 조회
    @GetMapping("/read")
    public AjaxResult getEquipmentRunChart(
            @RequestParam(value="fromDate", required=false) String date_from,
            @RequestParam(value="toDate", required=false) String date_to,
            @RequestParam(value="cboCompany", required=false) Integer companyCode,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = vendorBalanceDetailService.getPaymentList(date_from, date_to, companyCode);

        return result;
    }
}
