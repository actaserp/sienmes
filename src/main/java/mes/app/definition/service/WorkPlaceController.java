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
    // 사업장 세부조회
    @GetMapping("/detail")
    public AjaxResult getSpjangDetail(
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        int personId = user.getPersonid();

        return result;
    }
    // 사업장 등록
    @PostMapping("/save")
    public AjaxResult saveSpjangInfo(
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();

        Tb_xa012 tbXa012 = new Tb_xa012();
        tbXa012 = (Tb_xa012) params;
        try{
            tbXa012Repository.save(tbXa012);
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }
}
