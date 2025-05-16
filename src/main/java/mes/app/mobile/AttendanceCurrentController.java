package mes.app.mobile;

import mes.app.mobile.Service.AttendanceCurrentService;
import mes.app.mobile.Service.AttendanceStatisticsService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance_current")
public class AttendanceCurrentController {
    @Autowired
    AttendanceCurrentService attendanceCurrentService;

    // 개인별 휴가 현황 조회
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            @RequestParam(value="workcd", required = false) Integer workcd,
            @RequestParam(value="searchYear") String searchYear,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        int personId = user.getPersonid();

        // 개인별 연차정보 조회
        Map<String, Object> annInfo = attendanceCurrentService.getAnnInfo(personId);
        String rtdate = (String) annInfo.get("rtdate");
        annInfo.put("rtdate",rtdate.substring(0, 4) + "." + rtdate.substring(4, 6) + "." + rtdate.substring(6));
        // 개인별 휴가정보 조회
        List<Map<String, Object>> vacInfo = attendanceCurrentService.getVacInfo(workcd, searchYear, personId);
        for(Map<String, Object>vacDetail : vacInfo) {
            String reqdate = (String) vacDetail.get("reqdate"); // YYYYMM
            String frdate = (String) vacDetail.get("frdate");
            String todate = (String) vacDetail.get("todate"); 
            String yearflag = (String) vacDetail.get("yearflag"); // 연차구분
            vacDetail.put("reqdate",reqdate.substring(0, 4) + "." + reqdate.substring(4, 6) + "." + reqdate.substring(6));
            vacDetail.put("frdate",frdate.substring(0, 4) + "." + frdate.substring(4, 6) + "." + frdate.substring(6));
            vacDetail.put("todate",todate.substring(0, 4) + "." + todate.substring(4, 6) + "." + todate.substring(6));
            if("1".equals(yearflag)) {
                vacDetail.put("yearflag","연차");
            }else{
                vacDetail.put("yearflag","");
            }
        }
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("annInfo", annInfo);
        resultMap.put("vacInfo", vacInfo);

        result.data = resultMap;
        return result;
    }
}
