package mes.app.clock;

import mes.app.clock.service.DayMonthlyService;
import mes.domain.entity.Tb_pb203;
import mes.domain.entity.User;
import mes.domain.entity.Yearamt;
import mes.domain.entity.commute.TB_PB201;
import mes.domain.model.AjaxResult;
import mes.domain.repository.Tb_pb203Repository;
import mes.domain.repository.commute.TB_PB201Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clock/DayMonthly")
public class DayMonthlyController {

    @Autowired
    private DayMonthlyService dayMonthlyService;
    @Autowired
    private TB_PB201Repository tbPb201Repository;

    @Autowired
    private Tb_pb203Repository tb_pb203Repository;



    @GetMapping("/read")
    public AjaxResult getDayList(
            @RequestParam(value="work_division", required=false) String work_division,
            @RequestParam(value="serchday", required=false) String serchday,
            @RequestParam(value="depart", required=false) String depart,
            @RequestParam(value ="spjangcd") String spjangcd,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        if (serchday != null && serchday.contains("-")) {
            serchday = serchday.replaceAll("-", "");
        }

        List<Map<String, Object>> items = this.dayMonthlyService.getDayList(work_division, serchday,spjangcd,depart);
        result.data = items;
        return result;
    }


    @PostMapping("/save")
    @Transactional
    public AjaxResult saveDayList(
            @RequestBody Map<String, List<Map<String, Object>>> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();

        List<Map<String, Object>> dataList = requestData.get("list");

        if (dataList == null || dataList.isEmpty()) {
            result.success=false;
            result.message="저장할 데이터가 없습니다.";
            return result;
        }

        List<TB_PB201> tbpb201List = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            String workym = (String) item.get("workym");
            String workday = (String) item.get("workday");
            Integer personid = ((Number) item.get("personid")).intValue();
            String workcd = (String) item.get("worknm");

            String starttimeStr = (String) item.get("starttime"); // "09:30"
            String endtimeStr = (String) item.get("endtime");
            Object nomaltimeStr = item.get("nomaltime");
            Object jitimeStr = item.get("jitime");
            Object overtimeStr = item.get("overtime");
            Object nighttimeStr = item.get("nighttime");
            Object yuntimeStr =  item.get("yuntime");
            Object abtimeStr = item.get("abtime");
            Object holitimeStr = item.get("holitime");
            Object worktimeStr = item.get("worktime");


            Optional<TB_PB201> optional = tbPb201Repository.findByIdWorkymAndIdWorkdayAndIdPersonid(workym, workday, personid);


            if (optional.isPresent()) {
                TB_PB201 tbpb201 = optional.get();
                tbpb201.setFixflag("1");
                tbpb201.setWorkcd(workcd);

                if (starttimeStr != null && !starttimeStr.trim().isEmpty()) {
                    // "HH:mm" 형식인지 간단한 유효성 검사
                    if (starttimeStr.matches("^\\d{2}:\\d{2}$")) {
                        tbpb201.setStarttime(starttimeStr.trim());
                    } else {
                        result.success = false;
                        result.message = "출근시간 형식이 올바르지 않습니다. (예: 09:30)";
                        return result;
                    }
                }

                if (endtimeStr != null && !endtimeStr.trim().isEmpty()) {
                    // "HH:mm" 형식인지 간단한 유효성 검사
                    if (endtimeStr.matches("^\\d{2}:\\d{2}$")) {
                        tbpb201.setEndtime(endtimeStr.trim());
                    } else {
                        result.success = false;
                        result.message = "퇴근시간 형식이 올바르지 않습니다. (예: 09:30)";
                        return result;
                    }
                }

                if (nomaltimeStr != null ) {
                    try {
                        BigDecimal nomaltime = new BigDecimal(nomaltimeStr.toString());
                        tbpb201.setNomaltime(nomaltime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "근무시간 값이 숫자 형식이 아닙니다: " + nomaltimeStr;
                        return result;
                    }
                }

                if (jitimeStr != null ) {
                    try {
                        int jitime = Integer.parseInt(jitimeStr.toString());
                        tbpb201.setJitime(jitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                        return result;
                    }
                }

                if (overtimeStr != null ) {
                    try {
                        BigDecimal overtime = new BigDecimal(overtimeStr.toString());
                        tbpb201.setOvertime(overtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "연장근무 값이 숫자 형식이 아닙니다: " + overtimeStr;
                        return result;
                    }
                }

                if (nighttimeStr != null) {
                    try {
                        BigDecimal nighttime = new BigDecimal(nighttimeStr.toString());
                        tbpb201.setNighttime(nighttime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "야간근무 값이 숫자 형식이 아닙니다: " + nighttimeStr;
                        return result;
                    }
                }
                
                if (yuntimeStr != null ) {
                    try {
                        int yuntime = Integer.parseInt(yuntimeStr.toString());
                        tbpb201.setYuntime(yuntime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "연차 값이 숫자 형식이 아닙니다: " + yuntimeStr;
                        return result;
                    }
                }

                if (abtimeStr != null ) {
                    try {
                        int abtime = Integer.parseInt(abtimeStr.toString());
                        tbpb201.setAbtime(abtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + abtimeStr;
                        return result;
                    }
                }


                if (holitimeStr != null ) {
                    try {
                        BigDecimal holitime = new BigDecimal(holitimeStr.toString());
                        tbpb201.setHolitime(holitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + holitimeStr;
                        return result;
                    }
                }


                if (worktimeStr != null ) {
                    try {
                        BigDecimal worktime = new BigDecimal(worktimeStr.toString());
                        tbpb201.setWorktime(worktime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "총근무시간 값이 숫자 형식이 아닙니다: " + worktimeStr;
                        return result;
                    }
                }
                tbpb201List.add(tbpb201);
            }
        }

        // 저장
        List<TB_PB201> savedList = tbPb201Repository.saveAll(tbpb201List);

        result.success = true;
        result.data = savedList;
        return result;
    }


    @PostMapping("workcdList")
    public AjaxResult getspjangcd(@RequestParam(value ="spjangcd") String spjangcd){

        AjaxResult result = new AjaxResult();

        List<Map<String, String>> list = dayMonthlyService.workcdList(spjangcd);

        result.data = list;
        return result;
    }


    @GetMapping("/MonthlyRead")
    public AjaxResult getMonthlyList(
            @RequestParam(value="person_name", required=false) String person_name,
            @RequestParam(value="startdate", required=false) String startdate,
            @RequestParam(value="depart", required=false) String depart,
            @RequestParam(value ="spjangcd") String spjangcd,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        if (startdate != null && startdate.contains("-")) {
            startdate = startdate.replaceAll("-", "");
        }

        List<Map<String, Object>> items = this.dayMonthlyService.getMonthlyList(person_name, startdate,spjangcd,depart);
        result.data = items;
        return result;
    }


    /*@PostMapping("/Monthlysave")
    @Transactional
    public AjaxResult saveMonthlyList(
            @RequestBody Map<String, List<Map<String, Object>>> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();

        List<Map<String, Object>> dataList = requestData.get("list");

        if (dataList == null || dataList.isEmpty()) {
            result.success=false;
            result.message="저장할 데이터가 없습니다.";
            return result;
        }

        List<Tb_pb203> tbpb203List = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            String workym = (String) item.get("workym");
            String workday = (String) item.get("workday");
            Integer personid = ((Number) item.get("personid")).intValue();
            String workcd = (String) item.get("worknm");

            String starttimeStr = (String) item.get("starttime"); // "09:30"
            String endtimeStr = (String) item.get("endtime");
            Object nomaltimeStr = item.get("nomaltime");
            Object jitimeStr = item.get("jitime");
            Object overtimeStr = item.get("overtime");
            Object nighttimeStr = item.get("nighttime");
            Object yuntimeStr =  item.get("yuntime");
            Object abtimeStr = item.get("abtime");
            Object holitimeStr = item.get("holitime");
            Object worktimeStr = item.get("worktime");


            Optional<Tb_pb203> optional = tbPb201Repository.findByIdWorkymAndIdWorkdayAndIdPersonid(workym, workday, personid);


            if (optional.isPresent()) {
                Tb_pb203 tbpb203 = optional.get();
                tbpb201.setFixflag("1");
                tbpb201.setWorkcd(workcd);

                if (starttimeStr != null && !starttimeStr.trim().isEmpty()) {
                    // "HH:mm" 형식인지 간단한 유효성 검사
                    if (starttimeStr.matches("^\\d{2}:\\d{2}$")) {
                        tbpb201.setStarttime(starttimeStr.trim());
                    } else {
                        result.success = false;
                        result.message = "출근시간 형식이 올바르지 않습니다. (예: 09:30)";
                        return result;
                    }
                }

                if (endtimeStr != null && !endtimeStr.trim().isEmpty()) {
                    // "HH:mm" 형식인지 간단한 유효성 검사
                    if (endtimeStr.matches("^\\d{2}:\\d{2}$")) {
                        tbpb201.setEndtime(endtimeStr.trim());
                    } else {
                        result.success = false;
                        result.message = "퇴근시간 형식이 올바르지 않습니다. (예: 09:30)";
                        return result;
                    }
                }

                if (nomaltimeStr != null ) {
                    try {
                        BigDecimal nomaltime = new BigDecimal(nomaltimeStr.toString());
                        tbpb201.setNomaltime(nomaltime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "근무시간 값이 숫자 형식이 아닙니다: " + nomaltimeStr;
                        return result;
                    }
                }

                if (jitimeStr != null ) {
                    try {
                        int jitime = Integer.parseInt(jitimeStr.toString());
                        tbpb201.setJitime(jitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                        return result;
                    }
                }

                if (overtimeStr != null ) {
                    try {
                        BigDecimal overtime = new BigDecimal(overtimeStr.toString());
                        tbpb201.setOvertime(overtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "연장근무 값이 숫자 형식이 아닙니다: " + overtimeStr;
                        return result;
                    }
                }

                if (nighttimeStr != null) {
                    try {
                        BigDecimal nighttime = new BigDecimal(nighttimeStr.toString());
                        tbpb201.setNighttime(nighttime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "야간근무 값이 숫자 형식이 아닙니다: " + nighttimeStr;
                        return result;
                    }
                }

                if (yuntimeStr != null ) {
                    try {
                        int yuntime = Integer.parseInt(yuntimeStr.toString());
                        tbpb201.setYuntime(yuntime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "연차 값이 숫자 형식이 아닙니다: " + yuntimeStr;
                        return result;
                    }
                }

                if (abtimeStr != null ) {
                    try {
                        int abtime = Integer.parseInt(abtimeStr.toString());
                        tbpb201.setAbtime(abtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + abtimeStr;
                        return result;
                    }
                }


                if (holitimeStr != null ) {
                    try {
                        BigDecimal holitime = new BigDecimal(holitimeStr.toString());
                        tbpb201.setHolitime(holitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + holitimeStr;
                        return result;
                    }
                }


                if (worktimeStr != null ) {
                    try {
                        BigDecimal worktime = new BigDecimal(worktimeStr.toString());
                        tbpb201.setWorktime(worktime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "총근무시간 값이 숫자 형식이 아닙니다: " + worktimeStr;
                        return result;
                    }
                }
                tbpb203List.add(tbpb201);
            }
        }

        // 저장
        List<TB_PB201> savedList = tb_pb203Repository.saveAll(tbpb203List);

        result.success = true;
        result.data = savedList;
        return result;
    }*/


}
