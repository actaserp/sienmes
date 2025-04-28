package mes.app.PopBill;

import com.popbill.api.PopbillException;
import com.popbill.api.Response;
import com.popbill.api.TaxinvoiceService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/BaseService")
public class BaseServiceServiceController {

    @Autowired
    private TaxinvoiceService taxinvoiceService;

    @Value("${popbill.linkId}")
    private String LinkId;

    /*@RequestMapping(value = "checkIsMember", method = RequestMethod.GET)
    public String checkIsMember(@RequestParam String corpNum) throws PopbillException {
        *//**
         * 사업자번호를 조회하여 연동회원 가입여부를 확인합니다.
         * - LinkID는 연동신청 시 팝빌에서 발급받은 링크아이디 값입니다.
         * - https://developers.popbill.com/reference/taxinvoice/java/api/member#CheckIsMember
         *//*

        AjaxResult result = new AjaxResult();


        // 조회할 사업자번호, '-' 제외 10자리

        try {
            Response response = taxinvoiceService.checkIsMember(corpNum, LinkId);
            System.out.println("");
        } catch (PopbillException e) {
            return "exception";
        }

        return "response";
    }*/

    @RequestMapping(value = "checkIsMember", method = RequestMethod.GET)
    public String checkIsMember(Model m) throws PopbillException {
        /**
         * 사업자번호를 조회하여 연동회원 가입여부를 확인합니다.
         * - LinkID는 연동신청 시 팝빌에서 발급받은 링크아이디 값입니다.
         * - https://developers.popbill.com/reference/taxinvoice/java/api/member#CheckIsMember
         */

        // 조회할 사업자번호, '-' 제외 10자리
        String corpNum = "4051170594";

        try {
            Response response = taxinvoiceService.checkIsMember(corpNum, LinkId);

            m.addAttribute("Response", response);

        } catch (PopbillException e) {
            m.addAttribute("Exception", e);
            return "exception";
        }

        return "response";
    }
}
