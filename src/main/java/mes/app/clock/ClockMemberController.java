package mes.app.clock;

import mes.app.clock.service.ClockMemberService;
import mes.domain.entity.Tb_pb203;
import mes.domain.entity.Tb_pb203Id;
import mes.domain.entity.User;
import mes.domain.entity.commute.TB_PB201;
import mes.domain.entity.mobile.TB_PB204;
import mes.domain.model.AjaxResult;
import mes.domain.repository.mobile.TB_PB204Repository;
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
@RequestMapping("/api/clock/member")
public class ClockMemberController {

    @Autowired
    private ClockMemberService clockMemberService;

    @Autowired
    TB_PB204Repository tbPb204Repository;

    @GetMapping("/read")
    public AjaxResult getMemberList(
            @RequestParam(value="start_date", required=false) String start_date,
            @RequestParam(value="end_date", required=false) String end_date,
            @RequestParam(value="person_name", required=false) String person_name,
            @RequestParam(value ="spjangcd") String spjangcd,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        if (start_date != null && start_date.contains("-")) {
            start_date = start_date.replaceAll("-", "");
        }
        if (end_date != null && end_date.contains("-")) {
            end_date = end_date.replaceAll("-", "");
        }

        List<Map<String, Object>> items = this.clockMemberService.getMemberList(start_date,end_date,person_name,spjangcd);
        result.data = items;
        return result;
    }

    @PostMapping("/save")
    @Transactional
    public AjaxResult saveMemberList(
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

        List<TB_PB204> tbpb204List = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            Integer id = ((Number) item.get("id")).intValue();

            Optional<TB_PB204> optional = tbPb204Repository.findById(id);

            if (optional.isPresent()) {
                TB_PB204 tbpb204 = optional.get();
                tbpb204.setFixflag("1");


                tbpb204List.add(tbpb204);
            }
        }

        // 저장
        List<TB_PB204> savedList = tbPb204Repository.saveAll(tbpb204List);

        result.success = true;
        result.data = savedList;
        return result;
    }

    @PostMapping("/Cancel")
    @Transactional
    public AjaxResult CancelMemberList(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get("list");

        for (Map<String, Object> item : dataList) {
            String spjangcd = (String) item.get("spjangcd"); // 사업장코드
            Integer id = ((Number) item.get("id")).intValue(); // 사번

            Optional<TB_PB204> optional = tbPb204Repository.findById(id);
            if (optional.isPresent()) {
                TB_PB204 entity = optional.get();
                entity.setFixflag("0"); // fixflag를 "0"으로 설정
                tbPb204Repository.save(entity); // 변경사항 저장
            }
        }

        result.success = true;
        return result;
    }



}
