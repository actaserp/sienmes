package mes.app.mobile;

import lombok.extern.slf4j.Slf4j;
import mes.app.mobile.Service.MobileMainService;
import mes.app.transaction.service.MonthlyPurchaseListService;
import mes.domain.entity.User;
import mes.domain.entity.commute.TB_PB201;
import mes.domain.entity.commute.TB_PB201_PK;
import mes.domain.model.AjaxResult;
import mes.domain.repository.commute.TB_PB201Repository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/mobile_main")
public class MobileMainController {

    @Autowired
    MobileMainService mobileMainService;

    @Autowired
    private TB_PB201Repository tbPb201Repository;

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // 사용자 정보 조회(부서 이름 출근여부)
    @GetMapping("/read_userInfo")
    public AjaxResult getUserInfo(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();
        String username = user.getUsername();

        Map<String, Object> userInfo = mobileMainService.getUserInfo(username);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("first_name", (String)userInfo.get("first_name"));
        if(userInfo != null) {
            resultData.put("inOfficeTime", (String) userInfo.get("starttime"));
        }

        result.data = resultData;

        return result;
    }

    // 출근 메서드
    @PostMapping("/submitCommute")
    public AjaxResult submitCommute(
            @RequestParam(value="weekNum") Integer weekNum,
            @RequestParam(value="office") String office,
            @RequestParam(value="workym", required=false) String workym,
            @RequestParam(value="workday", required=false) String workday,
            @RequestParam(value="isHoly", required=false) String isHoly,
            @RequestParam(value="workcd", required=false) String workcd,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        // 일근태 테이블 초기화
        TB_PB201 tbPb201 = new TB_PB201();
        TB_PB201_PK tbPb201Pk = new TB_PB201_PK();

        User user = (User)auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = user.getSpjangcd();
        // 테스트용 spjangcd / ZZ
        spjangcd = "ZZ";
        // 직원코드 조회 및 근무구분 조회
        Map<String, Object> personInfo = mobileMainService.getPersonId(username);
        String perId = personInfo.get("personid").toString();
        String workType = String.format("%02d", Integer.parseInt(personInfo.get("PersonGroup_id").toString()));
        // 지각여부 확인 (근태설정 비교(jitime(지각)값 설정)) sttime 00:00
        LocalDateTime inOfficeTime = LocalDateTime.now();
        // 테스트 시간(8:59)
//        LocalDateTime inOfficeTime = LocalDateTime.of(2025, 5, 15, 8, 59);  // 2025년 5월 15일 08:59
        String sttime = (String) mobileMainService.getWorkTime(workType).get("sttime");
        // inOfficeTime에서 시간만 추출 지각 비교
        LocalTime sttimeParsed = LocalTime.parse(sttime, timeFormatter);
        LocalTime currentTime = inOfficeTime.toLocalTime();
        String formattedCurrentTime = currentTime.format(timeFormatter); // "HH:mm" 형식으로 포맷
        String jitFlag = sttimeParsed.isAfter(currentTime) ? "0" : "1";
        // 사내 / 외부 출근 확인
        String inFlag = "";
        if(office.equals("inOfficeIn")){
            inFlag = "0";
        }else{
            inFlag = "1";
            tbPb201.setWorkcd(workcd);
        }

        tbPb201Pk.setPersonid(Integer.valueOf(perId));
        tbPb201Pk.setSpjangcd(spjangcd);
        tbPb201Pk.setWorkday(workday);
        tbPb201Pk.setWorkym(workym);

        tbPb201.setWorknum(weekNum);
        tbPb201.setId(tbPb201Pk);
        tbPb201.setHoliyn(isHoly);
        tbPb201.setWorkcd(workday);

        tbPb201.setStarttime(formattedCurrentTime);
        tbPb201.setJitime(Integer.parseInt(jitFlag));
        tbPb201.setInflag(inFlag);



        result.message = "출근등록이 완료되었습니다.";
        try {
            result.data = tbPb201Repository.save(tbPb201);
        }catch (Exception e){
            e.printStackTrace();
            result.message = "오류가 발생하였습니다.";
        }
        return result;
    }
    // 퇴근메서드
    @PostMapping("/modifyCommute")
    public AjaxResult submitCommute(
            @RequestParam(value="office") String office,
            @RequestParam(value="workym", required=false) String workym,
            @RequestParam(value="workday", required=false) String workday,
            @RequestParam(value="remark", required=false) String remark,
            @RequestParam(value="workcd", required=false) String workcd,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = user.getSpjangcd();
        // 테스트용 spjangcd / ZZ
        spjangcd = "ZZ";
        String workyn;
        // 직원코드 조회 및 근무구분 조회
        Map<String, Object> personInfo = mobileMainService.getPersonId(username);
        String perId = personInfo.get("personid").toString();
        String workType = String.format("%02d", Integer.parseInt(personInfo.get("PersonGroup_id").toString()));

        // 퇴근시간 조회(조퇴 확인) / 근무구분에 따른 정상퇴근시간 조회
        LocalDateTime outOfficeTime = LocalDateTime.now();
        Map<String, Object> WorkTimeInfo = mobileMainService.getWorkTime(workType);
        String endtime = (String) WorkTimeInfo.get("endtime");
        // outOfficeTime 시간만 추출 정상퇴근 비교
        LocalTime endtimeParsed = LocalTime.parse(endtime, timeFormatter);
        LocalTime currentTime = outOfficeTime.toLocalTime();
        String formattedCurrentTime = currentTime.format(timeFormatter); // "HH:mm" 형식으로 포맷
        int jotFlag =  currentTime.isAfter(endtimeParsed) ? 0 : 1;

        // 일근태 테이블 초기화
        TB_PB201_PK tbPb201Pk = new TB_PB201_PK();
        tbPb201Pk.setPersonid(Integer.valueOf(perId));
        tbPb201Pk.setSpjangcd(spjangcd);
        tbPb201Pk.setWorkday(workday);
        tbPb201Pk.setWorkym(workym);
        // 정상퇴근(workyn값 지정 / 지각,조퇴 등 해당사항 유무 확인하여 이상없을시 1)
        Optional<TB_PB201> savedTbPb201 = tbPb201Repository.findById(tbPb201Pk);
        TB_PB201 entity = savedTbPb201.get();  // 값이 존재하면 꺼냄
        if(entity.getEndtime() != null){
            result.message = "이미 퇴근처리 되었습니다.";
            return result;
        }
        log.info("saved 201 data : {}", entity);
        if(entity.getJitime() == 1 ||
                jotFlag == 1 ||
                entity.getBantime() == 1 ){
            workyn = "0";
        }else{
            workyn = "1";
        }
        // 사내 / 외부 퇴근 확인
        String inFlag = "";
        if(office.equals("inOfficeOut")){
            inFlag = "0";
        }else{
            inFlag = "1";
            // 외부퇴근일경우 사유 바인드
            entity.setWorkcd(workcd);
        }
        // 출근시간 ~ 퇴근시간 비교하여 정상, 연장, 야간 근무시간 계산 후 바인드
        String sttime = (String) WorkTimeInfo.get("sttime"); // 출근시간
        //휴식(점심)시간 설정값으로 할지 몰라 하드코딩
        String startRestTime = "12:00";
        String endRestTime = "13:00";
        String ovsttime = (String) WorkTimeInfo.get("ovsttime"); // 연장근무 시작시간
        String ovedtime = (String) WorkTimeInfo.get("ovedtime"); // 연장근무 종료시간
        String ngsttime = (String) WorkTimeInfo.get("ngsttime"); // 야간근무 시작시간
        String ngedtime = (String) WorkTimeInfo.get("ngedtime"); // 야간근무 종료시간

        // 시간 파싱
        LocalTime startTime = LocalTime.parse(entity.getStarttime(), timeFormatter); // 사용자 출근시간
        LocalTime endTime = currentTime; // 사용자 퇴근시간
        LocalTime normalStart = LocalTime.parse(sttime, timeFormatter);
        LocalTime normalEnd = LocalTime.parse(endtime, timeFormatter); // 정상근무 퇴근시간
        LocalTime overStart = LocalTime.parse(ovsttime, timeFormatter);
        LocalTime overEnd = LocalTime.parse(ovedtime, timeFormatter);
        LocalTime nightStart = LocalTime.parse(ngsttime, timeFormatter);
        LocalTime nightEnd = LocalTime.parse(ngedtime, timeFormatter);
        // 휴식(점심)시간
        LocalTime restStart = LocalTime.parse(startRestTime, timeFormatter);
        LocalTime restEnd = LocalTime.parse(endRestTime, timeFormatter);

        //정상근무 계산
        BigDecimal normalTime = calculateTimeOverlap(startTime, endTime, normalStart, normalEnd, restStart, restEnd);

        // 연장 근무 시간 계산
        BigDecimal overTime = calculateTimeOverlap(startTime, endTime, overStart, overEnd, restStart, restEnd);

        // 야간 근무 시간 계산 (00:00을 기준으로 넘어갈 경우 처리)
        BigDecimal nightTime;
        if (nightEnd.isBefore(nightStart)) {
            // 다음날로 넘어갈 때
            BigDecimal nightPart1 = calculateTimeOverlap(startTime, endTime, nightStart, LocalTime.MAX, restStart, restEnd);
            BigDecimal nightPart2 = calculateTimeOverlap(startTime, endTime, LocalTime.MIN, nightEnd, restStart, restEnd);
            nightTime = nightPart1.add(nightPart2);
        } else {
            nightTime = calculateTimeOverlap(startTime, endTime, nightStart, nightEnd, restStart, restEnd);
        }
        // 총 근무 시간
        BigDecimal totalTime = normalTime.add(overTime).add(nightTime);

        entity.setId(tbPb201Pk);
        entity.setWorkyn(workyn);
        entity.setWorkcd(workday);
        entity.setEndtime(formattedCurrentTime);
        entity.setRemark(remark);
        entity.setInflag(inFlag);
        if(entity.getHoliyn().equals("0")){
            entity.setWorktime(totalTime);
            entity.setNomaltime(normalTime);
            entity.setOvertime(overTime);
            entity.setNighttime(nightTime);
        }else{
            entity.setWorktime(totalTime);
            entity.setHolitime(totalTime);
        }
        entity.setJotime(jotFlag);
        result.message = "퇴근처리가 마무리되었습니다.";
        try {
            result.data = tbPb201Repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        return result;
    }

    // 시간대별 근무시간 계산 메서드 (휴식 시간 제외)
    public static BigDecimal calculateTimeOverlap(LocalTime start, LocalTime end, LocalTime rangeStart, LocalTime rangeEnd, LocalTime restStart, LocalTime restEnd) {
        // 시간대 겹침 계산
        LocalTime actualStart = start.isBefore(rangeStart) ? rangeStart : start;
        LocalTime actualEnd = end.isAfter(rangeEnd) ? rangeEnd : end;

        // 정상 근무 시간이 있을 경우에만 처리
        if (actualStart.isBefore(actualEnd)) {
            // 휴식 시간 체크
            Duration workDuration = Duration.between(actualStart, actualEnd);

            // 근무 시간이 휴식 시간과 겹치는 경우
            if (!(restEnd.isBefore(actualStart) || restStart.isAfter(actualEnd))) {
                // 겹치는 시간 계산
                LocalTime restOverlapStart = actualStart.isBefore(restStart) ? restStart : actualStart;
                LocalTime restOverlapEnd = actualEnd.isAfter(restEnd) ? restEnd : actualEnd;

                if (restOverlapStart.isBefore(restOverlapEnd)) {
                    Duration restDuration = Duration.between(restOverlapStart, restOverlapEnd);
                    workDuration = workDuration.minus(restDuration);
                }
            }

            // 소수점 2자리까지 반올림
            double hours = workDuration.toMinutes() / 60.0;
            return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
