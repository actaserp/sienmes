package mes.app.clock;

import mes.app.clock.service.DayMonthlyService;
import mes.domain.entity.Tb_pb203;
import mes.domain.entity.Tb_pb203Id;
import mes.domain.entity.User;
import mes.domain.entity.Yearamt;
import mes.domain.entity.commute.TB_PB201;
import mes.domain.model.AjaxResult;
import mes.domain.repository.Tb_pb203Repository;
import mes.domain.repository.commute.TB_PB201Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get("list");
        String spjangcd = (String) requestData.get("spjangcd");

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
            String workcd = (String) item.get("workcd");

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


            Optional<TB_PB201> optional = tbPb201Repository.findByIdSpjangcdAndIdWorkymAndIdWorkdayAndIdPersonid(spjangcd,workym, workday, personid);


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


    @PostMapping("/Monthlysave")
    @Transactional
    public AjaxResult saveMonthlyList(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get("list");
        String spjangcd = (String) requestData.get("spjangcd");

        if (dataList == null || dataList.isEmpty()) {
            result.success=false;
            result.message="저장할 데이터가 없습니다.";
            return result;
        }

        List<Tb_pb203> tbpb203List = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            String workym = (String) item.get("workym"); //년월
            Object workdayStr = item.get("workcount"); //근무일수
            Integer personid = ((Number) item.get("personid")).intValue(); // 사번


            Object nomaltimeStr = item.get("nomaltime");
            Object worktimeStr = item.get("worktime");
            Object jitimeStr = item.get("jitime");
            Object jotimeStr = item.get("jotime");
            Object overtimeStr = item.get("overtime");
            Object nighttimeStr = item.get("nighttime");

            Object yuntimeStr =  item.get("yuntime");
            Object abtimeStr = item.get("abtime");
            Object holitimeStr = item.get("holitime");

            /*Optional<Tb_pb203> optional = tb_pb203Repository.findByIdSpjangcdAndIdWorkymAndIdPersonid(spjangcd,workym, personid);*/

            /*if (optional.isPresent()) {  }*/
                Tb_pb203 tbpb203 = new Tb_pb203();
                Tb_pb203Id id = tbpb203.getId();

                if (id == null) {
                    id = new Tb_pb203Id();
                }


                id.setPersonid(personid);
                id.setSpjangcd(spjangcd);
                id.setWorkym(workym);

                tbpb203.setId(id);


                tbpb203.setFixflag("0");

                // 근무일수 
                if (workdayStr != null ) {
                    try {
                        int workday = Integer.parseInt(workdayStr.toString());
                        tbpb203.setWorkday(workday);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "근무일수 값이 숫자 형식이 아닙니다: " + workdayStr;
                        return result;
                    }
                }

                // 정상근무시간
                if (nomaltimeStr != null ) {
                    try {
                        BigDecimal nomaltime = new BigDecimal(nomaltimeStr.toString());
                        tbpb203.setNomaltime(nomaltime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "근무시간 값이 숫자 형식이 아닙니다: " + nomaltimeStr;
                        return result;
                    }
                }
                
                // 총근무시간
                if (worktimeStr != null ) {
                    try {
                        BigDecimal worktime = new BigDecimal(worktimeStr.toString());
                        tbpb203.setWorktime(worktime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "총근무시간 값이 숫자 형식이 아닙니다: " + worktimeStr;
                        return result;
                    }
                }

                // 지각
                if (jitimeStr != null ) {
                    try {
                        BigDecimal jitime = new BigDecimal(jitimeStr.toString());
                        tbpb203.setJitime(jitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                        return result;
                    }
                }

                // 조퇴
                if (jotimeStr != null ) {
                    try {
                        BigDecimal jotime = new BigDecimal(jitimeStr.toString());
                        tbpb203.setJitime(jotime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                        return result;
                    }
                }

                // 연장
                if (overtimeStr != null ) {
                    try {
                        BigDecimal overtime = new BigDecimal(overtimeStr.toString());
                        tbpb203.setOvertime(overtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "연장근무 값이 숫자 형식이 아닙니다: " + overtimeStr;
                        return result;
                    }
                }

                // 야간
                if (nighttimeStr != null) {
                    try {
                        BigDecimal nighttime = new BigDecimal(nighttimeStr.toString());
                        tbpb203.setNighttime(nighttime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "야간근무 값이 숫자 형식이 아닙니다: " + nighttimeStr;
                        return result;
                    }
                }

                // 휴가
                if (yuntimeStr != null ) {
                    try {
                        BigDecimal yuntime = new BigDecimal(yuntimeStr.toString());
                        tbpb203.setYuntime(yuntime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "휴가 값이 숫자 형식이 아닙니다: " + yuntimeStr;
                        return result;
                    }
                }

                //결근
                if (abtimeStr != null ) {
                    try {
                        BigDecimal abtime = new BigDecimal(abtimeStr.toString());
                        tbpb203.setAbtime(abtime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "결근 값이 숫자 형식이 아닙니다: " + abtimeStr;
                        return result;
                    }
                }

                // 특근
                if (holitimeStr != null ) {
                    try {
                        BigDecimal holitime = new BigDecimal(holitimeStr.toString());
                        tbpb203.setHolitime(holitime);
                    } catch (NumberFormatException e) {
                        result.success = false;
                        result.message = "특근 값이 숫자 형식이 아닙니다: " + holitimeStr;
                        return result;
                    }
                }

                tbpb203List.add(tbpb203);

        }

        // 저장
        List<Tb_pb203> savedList = tb_pb203Repository.saveAll(tbpb203List);

        result.success = true;
        result.data = savedList;
        return result;
    }


    @PostMapping("/MonthlysaveMagam")
    @Transactional
    public AjaxResult saveMonthlyMagamList(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get("list");
        String spjangcd = (String) requestData.get("spjangcd");

        if (dataList == null || dataList.isEmpty()) {
            result.success=false;
            result.message="저장할 데이터가 없습니다.";
            return result;
        }

        List<Tb_pb203> tbpb203List = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            String workym = (String) item.get("workym"); //년월
            Object workdayStr = item.get("workcount"); //근무일수
            Integer personid = ((Number) item.get("personid")).intValue(); // 사번

            // 존재 여부 체크
            boolean exists = tb_pb203Repository.existsByKey(spjangcd, workym, personid);
            if (!exists) {
                result.success = false;
                result.message = "월정산 데이터가 없습니다. 먼저 월정산을 진행해주세요.";
                return result;
            }


            Object nomaltimeStr = item.get("nomaltime");
            Object worktimeStr = item.get("worktime");
            Object jitimeStr = item.get("jitime");
            Object jotimeStr = item.get("jotime");
            Object overtimeStr = item.get("overtime");
            Object nighttimeStr = item.get("nighttime");

            Object yuntimeStr =  item.get("yuntime");
            Object abtimeStr = item.get("abtime");
            Object holitimeStr = item.get("holitime");

            Optional<Tb_pb203> optional = tb_pb203Repository.findByIdSpjangcdAndIdWorkymAndIdPersonid(spjangcd,workym, personid);

            if (optional.isPresent()) {
            Tb_pb203 tbpb203 = optional.get();
            /*Tb_pb203Id id = tbpb203.getId();
            if (id == null) {
                id = new Tb_pb203Id();
            }
            id.setPersonid(personid);
            id.setSpjangcd(spjangcd);
            id.setWorkym(workym);
            tbpb203.setId(id);*/

            tbpb203.setFixflag("1");

            // 근무일수
            if (workdayStr != null ) {
                try {
                    int workday = Integer.parseInt(workdayStr.toString());
                    tbpb203.setWorkday(workday);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "근무일수 값이 숫자 형식이 아닙니다: " + workdayStr;
                    return result;
                }
            }

            // 정상근무시간
            if (nomaltimeStr != null ) {
                try {
                    BigDecimal nomaltime = new BigDecimal(nomaltimeStr.toString());
                    tbpb203.setNomaltime(nomaltime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "근무시간 값이 숫자 형식이 아닙니다: " + nomaltimeStr;
                    return result;
                }
            }

            // 총근무시간
            if (worktimeStr != null ) {
                try {
                    BigDecimal worktime = new BigDecimal(worktimeStr.toString());
                    tbpb203.setWorktime(worktime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "총근무시간 값이 숫자 형식이 아닙니다: " + worktimeStr;
                    return result;
                }
            }

            // 지각
            if (jitimeStr != null ) {
                try {
                    BigDecimal jitime = new BigDecimal(jitimeStr.toString());
                    tbpb203.setJitime(jitime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                    return result;
                }
            }

            // 조퇴
            if (jotimeStr != null ) {
                try {
                    BigDecimal jotime = new BigDecimal(jitimeStr.toString());
                    tbpb203.setJitime(jotime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "지각 값이 숫자 형식이 아닙니다: " + jitimeStr;
                    return result;
                }
            }

            // 연장
            if (overtimeStr != null ) {
                try {
                    BigDecimal overtime = new BigDecimal(overtimeStr.toString());
                    tbpb203.setOvertime(overtime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "연장근무 값이 숫자 형식이 아닙니다: " + overtimeStr;
                    return result;
                }
            }

            // 야간
            if (nighttimeStr != null) {
                try {
                    BigDecimal nighttime = new BigDecimal(nighttimeStr.toString());
                    tbpb203.setNighttime(nighttime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "야간근무 값이 숫자 형식이 아닙니다: " + nighttimeStr;
                    return result;
                }
            }

            // 휴가
            if (yuntimeStr != null ) {
                try {
                    BigDecimal yuntime = new BigDecimal(yuntimeStr.toString());
                    tbpb203.setYuntime(yuntime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "휴가 값이 숫자 형식이 아닙니다: " + yuntimeStr;
                    return result;
                }
            }

            //결근
            if (abtimeStr != null ) {
                try {
                    BigDecimal abtime = new BigDecimal(abtimeStr.toString());
                    tbpb203.setAbtime(abtime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "결근 값이 숫자 형식이 아닙니다: " + abtimeStr;
                    return result;
                }
            }

            // 특근
            if (holitimeStr != null ) {
                try {
                    BigDecimal holitime = new BigDecimal(holitimeStr.toString());
                    tbpb203.setHolitime(holitime);
                } catch (NumberFormatException e) {
                    result.success = false;
                    result.message = "특근 값이 숫자 형식이 아닙니다: " + holitimeStr;
                    return result;
                }
            }

            tbpb203List.add(tbpb203);
            }
        }

        // 저장
        List<Tb_pb203> savedList = tb_pb203Repository.saveAll(tbpb203List);

        result.success = true;
        result.data = savedList;
        return result;
    }

}
