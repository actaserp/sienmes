package mes.app.PaymentLine;

import mes.app.PaymentLine.Service.PaymentLineService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paymentLine")
public class PaymentLineController {
    @Autowired
    PaymentLineService paymentLineService;

    @Autowired
    private Settings settings;

    // 문서코드 리스트
    @GetMapping("/read")
    public AjaxResult read(@RequestParam String comcd
            , Authentication auth){

        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = paymentLineService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);

        List<Map<String, Object>> items = this.paymentLineService.getPaymentList(splitPerid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("perid");
            Map<String, Object> kcInfo = paymentLineService.getuserInfoPerid(kcperid);
            paperInfo.put("pernm", kcInfo.get("pernm"));
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
    // 문서코드 리스트
    @GetMapping("/readLine")
    public AjaxResult readLine(@RequestParam String comcd,
                               @RequestParam String perid,
                               Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
//        String perid = paymentLineService.getPerid(username);
//        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);

        List<Map<String, Object>> items = this.paymentLineService.getCheckPaymentList(perid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("kcperid");
            Map<String, Object> kcInfo = paymentLineService.getuserInfoPerid(kcperid);
            paperInfo.put("kcpernm", kcInfo.get("pernm"));

            String gubunnm = paymentLineService.getGubuncd((String)paperInfo.get("gubun"));
            paperInfo.put("gubunnm", gubunnm);
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
}
