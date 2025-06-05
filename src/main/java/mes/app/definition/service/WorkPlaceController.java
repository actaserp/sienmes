package mes.app.definition.service;

import mes.app.mobile.Service.AttendanceCurrentService;
import mes.domain.entity.Tb_xa012;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.Tb_xa012Repository;
import mes.domain.repository.mobile.TB_PB204Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workplace")
public class WorkPlaceController {
    @Autowired
    WorkPlaceService workPlaceService;
    @Autowired
    Tb_xa012Repository tbXa012Repository;
    
    // 사업장정보 리스트 조회
    @GetMapping("/read")
    public AjaxResult getSpjangInfo(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();

        result.data = tbXa012Repository.findAll();
        return result;
    }
    // 사업장 등록
    @PostMapping("/save")
    public AjaxResult saveSpjangInfo(
            @ModelAttribute  Tb_xa012 tbXa012,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();

        try {
            tbXa012Repository.save(tbXa012);
            result.success = true;
            result.message = "저장되었습니다.";
        } catch (Exception e) {
            e.printStackTrace();
            result.success = false;
            result.message = "저장 중 오류 발생";
        }

        return result;
    }
    // 사업장 삭제
    @PostMapping("/delete")
    public AjaxResult deleteSpjangInfo(
            @RequestParam String spjangcd,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        try {
            tbXa012Repository.deleteById(spjangcd);
            result.success = true;
            result.message = "삭제되었습니다.";
        } catch (Exception e) {
            e.printStackTrace();
            result.success = false;
            result.message = "삭제 중 오류 발생";
        }
        return result;
    }
}
