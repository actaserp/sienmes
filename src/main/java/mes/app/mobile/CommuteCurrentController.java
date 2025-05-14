package mes.app.mobile;

import mes.app.mobile.Service.CommuteCurrentService;
import mes.app.mobile.Service.MobileMainService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/commute_current")
public class CommuteCurrentController {
    // 출퇴근현황
    @Autowired
    CommuteCurrentService commuteCurrentService;

    // 사용자 정보 조회(부서 이름 직급 출근여부)
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        String user = auth.getPrincipal().toString();

//        result.data = commuteCurrentService.getUserInfo(user);

        return result;
    }
}
