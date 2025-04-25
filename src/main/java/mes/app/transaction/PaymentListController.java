package mes.app.transaction;

import mes.app.transaction.Service.PaymentListService;
import mes.domain.model.AjaxResult;
import mes.domain.repository.EquRunRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transaction/payment_list")
public class PaymentListController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    PaymentListService paymentListService;

    // 지급현황 리스트 조회
    @GetMapping("/read")
    public AjaxResult getEquipmentRunChart(
            @RequestParam(value="date_from", required=false) String date_from,
            @RequestParam(value="date_to", required=false) String date_to,
            @RequestParam(value="companyCode", required=false) Integer companyCode,
            @RequestParam(value="accountNum", required=false) String accountNum,
            @RequestParam(value="depositType", required=false) String depositType,
            @RequestParam(value="remark", required=false) String remark,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = paymentListService.getPaymentList(date_from, date_to, companyCode, accountNum, depositType, remark);

        return result;
    }
}
