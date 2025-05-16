package mes.app.mobile;

import mes.app.mobile.Service.AttendanceStatisticsService;
import mes.app.mobile.Service.MobileMainService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/attendance_statistics")
public class AttendanceStatisticsController {
    @Autowired
    AttendanceStatisticsService attendanceStatisticsService;

    // 사용자 정보 조회(부서 이름 직급 출근여부)
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();


        result.data = attendanceStatisticsService.getUserInfo(username);

        return result;
    }
}
