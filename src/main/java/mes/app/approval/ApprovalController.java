package mes.app.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.approval.service.ApprovalService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.approval.TB_E063;
import mes.domain.entity.approval.TB_E063_PK;
import mes.domain.entity.approval.TB_E064;
import mes.domain.entity.approval.TB_E064_PK;
import mes.domain.model.AjaxResult;
import mes.domain.repository.approval.E063Repository;
import mes.domain.repository.approval.E064Repository;
import mes.domain.repository.approval.E080Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {
    @Autowired
    private ApprovalService approvalService;

//    @Autowired
//    private E063Repository e063Repository;
//
//    @Autowired
//    private E064Repository e064Repository;
//
//    @Autowired
//    private E080Repository e080Repository;

    @Autowired
    Settings settings;
    // 결재라인등록 그리드 read
    @GetMapping("/read")
    public AjaxResult getList(@RequestParam Map<String, String> params
            , Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = approvalService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
//        Map<String, Object> userInfo = requestService.getUserInfo(username);
        String comcd = params.get("comcd");

        List<Map<String, Object>> items = this.approvalService.getCheckPaymentList(splitPerid, comcd);
        for (Map<String, Object> paperInfo : items){
            String kcperid = "p" + paperInfo.get("kcperid");
            Map<String, Object> kcInfo = approvalService.getuserInfoPerid(kcperid);
            paperInfo.put("kcpernm", kcInfo.get("pernm"));

            String gubunnm = approvalService.getGubuncd((String)paperInfo.get("gubun"));
            paperInfo.put("gubunnm", gubunnm);
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    // 문서코드 옵션 불러오기
    @GetMapping("/getComcd")
    public AjaxResult getListHgrb(){
        List<Map<String, Object>> items = this.approvalService.getComcd();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    // 결재자 옵션 불러오기
    @GetMapping("/getKcperid")
    public AjaxResult getKcperid(){
        List<Map<String, Object>> items = this.approvalService.getKcperid();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    // 공통코드 리스트 가져오기
    @GetMapping("/find_parent_id")
    public List<Map<String, Object>> getCommonCodeList(@RequestParam("id") Integer id) {
        return approvalService.findByParentId(id);
    }

    // 유저정보 불러와 input태그 value
    @GetMapping("/getUserInfo")
    public AjaxResult getUserInfo(Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String perid = approvalService.getPerid(username);
        String splitPerid = perid.replaceFirst("p", ""); // ✅ 첫 번째 "p"만 제거
        Map<String, Object> userInfo = approvalService.getMyInfo(username);
        userInfo.put("perid", splitPerid);

        AjaxResult result = new AjaxResult();
        result.data = userInfo;
        return result;
    }

    // 삭제 메서드
    @PostMapping("/delete")
    public AjaxResult deleteHead(@RequestParam Map<String, String> params,
                                 Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = approvalService.getMyInfo(username);
        // 064table PK - custcd,spjangcd,perid,papercd,no
        TB_E064_PK e064PK = new TB_E064_PK();
        e064PK.setPapercd(params.get("papercd"));
//        e064PK.setCustcd((String) userInfo.get("custcd"));
//        e064PK.setPerid(params.get("perid"));
        e064PK.setNo(params.get("no"));
        e064PK.setSpjangcd((String) userInfo.get("spjangcd"));

        try {
//            e064Repository.deleteById(e064PK);
            result.success = true;
            result.message = "삭제하였습니다.";
        }
        catch (Exception e){
            result.success = false;
            result.message = "삭제에 실패하였습니다.";
        }
        return result;
    }
    // 결재라인 등록
    @PostMapping("/save")
    public AjaxResult saveOrder(@RequestParam Map<String, String> params,
                                Authentication auth) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AjaxResult result = new AjaxResult();
        TB_E063_PK headpk = new TB_E063_PK();
        TB_E063 head = new TB_E063();
        TB_E064_PK bodypk = new TB_E064_PK();
        TB_E064 body = new TB_E064();
        LocalDateTime createdAt = LocalDateTime.now();
        String indate = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        User user = (User)auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = approvalService.getMyInfo(username);

        // 063 테이블 선언
        headpk.setCustcd((String) userInfo.get("custcd"));
        headpk.setPerid(params.get("perid"));
        headpk.setPapercd(params.get("papercd"));
        headpk.setSpjangcd((String) userInfo.get("spjangcd"));

        head.setId(headpk);
        head.setInperid(username);
        head.setIndate(indate);



        // 064테이블 선언
//        bodypk.setCustcd((String) userInfo.get("custcd"));
//        bodypk.setPerid(params.get("perid"));
        bodypk.setSpjangcd((String) userInfo.get("spjangcd"));
        if (params.get("no") == null || Objects.equals(params.get("no"), "")) {
            bodypk.setNo(getNextNoForKey((String) userInfo.get("custcd"),
                    (String) userInfo.get("spjangcd"),
                    params.get("perid"),
                    params.get("papercd")));
        }else{
            bodypk.setNo(params.get("no"));
        }
        bodypk.setPapercd(params.get("papercd"));

        body.setId(bodypk);
//        body.setInperid(username);
//        body.setGubun(params.get("gubun"));
//        body.setKcchk("1");
//        body.setKcperid(params.get("kcperid"));
        body.setSeq(params.get("seq"));
        body.setIndate(indate);
        // 데이터 insert
        try {
//            e063Repository.save(head);
//            e064Repository.save(body);

            result.success = true;
            result.message = "저장을 성공했습니다.";
        }catch (Exception e) {
            result.success = false;
            result.message = "저장 실패(" + e.getMessage() + ")";
        }
        return result;
    }
    // 064 테이블 no 컬럼 Max값 +1
    public String getNextNoForKey(String custcd, String spjangcd, String perid, String papercd) {
        // 현재 max(no) 조회
//        String maxNo = e064Repository.findMaxNo(custcd, spjangcd, perid, papercd);
//        int next = maxNo != null ? Integer.parseInt(maxNo) + 1 : 1;
//        return String.valueOf(next); // 예: 001, 002
        return "";
    }

}

