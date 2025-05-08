package mes.app.mobile;

import mes.app.mobile.Service.MobileMainService;
import mes.app.transaction.service.MonthlyPurchaseListService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/mobile_main")
public class MobileMainController {

    @Autowired
    MobileMainService mobileMainService;

    // 사용자 정보 조회(부서 이름 직급 출근여부)
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        String user = auth.getPrincipal().toString();

        result.data = mobileMainService.getUserInfo(user);

        return result;
    }

    // 출근/퇴근 메서드
    @PostMapping("/submitCommute")
    public AjaxResult submitCommute(
            @RequestParam(value="cboYear", required=false) String cboYear,
            @RequestParam(value="cboCompany", required=false) Integer cboCompany,
            HttpServletRequest request) {
        AjaxResult result = new AjaxResult();

        result.data = mobileMainService.submitCommute(cboYear);

        return result;
    }
}
