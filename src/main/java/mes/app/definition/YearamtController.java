package mes.app.definition;

import mes.app.definition.service.YearamtService;
import mes.domain.entity.Area;
import mes.domain.entity.User;
import mes.domain.entity.Yearamt;
import mes.domain.model.AjaxResult;
import mes.domain.repository.YearamtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/definition/yearamt")
public class YearamtController {

    @Autowired
    YearamtService yearamtService;

    @Autowired
    YearamtRepository yearamtRepository;

    @GetMapping("/read")
    public AjaxResult getYearamtList(
            @RequestParam(value="cboYear", required=false) String year,
            @RequestParam(value="ioflag", required=false) String ioflag,
            @RequestParam(value="searchId", required=false) String cltid,
            @RequestParam(value="searchname", required=false) String name
    ) {
        AjaxResult result = new AjaxResult();

        result.data = this.yearamtService.getYearamtList(year,ioflag,cltid,name);
        return result;
    }


    @PostMapping("/save")
    @Transactional
    public AjaxResult saveYearamt(
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

        List<Yearamt> yearamtList = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            Yearamt yearamt = new Yearamt();

            yearamt.setCltcd((Integer) item.get("cltcd"));
            yearamt.setIoflag((String) item.get("ioflag"));
            yearamt.setYyyymm((String) item.get("yyyymm"));

            // balance 값이 null이 아닌 경우 Integer로 변환하여 yearamt에 저장
            if (item.get("balance") != null) {
                yearamt.setYearamt(((Number) item.get("balance")).intValue());
            }

           /* // endyn 값이 null이면 "N"으로 저장
            Object endynVal = item.get("endyn");
            yearamt.setEndyn(endynVal == null ? "N" : endynVal.toString());*/

            Object endynVal = item.get("endyn");
            if (endynVal instanceof Boolean) {
                yearamt.setEndyn((Boolean) endynVal ? "N" : "Y");
            } else {
                yearamt.setEndyn("Y");
            }




            yearamtList.add(yearamt);
        }

        // 저장
        List<Yearamt> savedList = yearamtRepository.saveAll(yearamtList);

        result.success = true;
        result.data = savedList;
        return result;
    }


    @PostMapping("/magam")
    @Transactional
    public AjaxResult saveYearamtMagam(
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

        List<Yearamt> yearamtList = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            Yearamt yearamt = new Yearamt();

            yearamt.setCltcd((Integer) item.get("cltcd"));
            yearamt.setIoflag((String) item.get("ioflag"));
            yearamt.setYyyymm((String) item.get("yyyymm"));

            // balance 값이 null이 아닌 경우 Integer로 변환하여 yearamt에 저장
            if (item.get("balance") != null) {
                yearamt.setYearamt(((Number) item.get("balance")).intValue());
            }

            Object endynVal = item.get("endyn");
            if (endynVal instanceof Boolean) {
                yearamt.setEndyn((Boolean) endynVal ? "Y" : "N");
            } else {
                yearamt.setEndyn("N");
            }

            yearamtList.add(yearamt);
        }

        // 저장
        List<Yearamt> savedList = yearamtRepository.saveAll(yearamtList);

        result.success = true;
        result.data = savedList;
        return result;
    }


    @PostMapping("/magamCancel")
    @Transactional
    public AjaxResult saveYearamtMagamCancel(
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

        List<Yearamt> yearamtList = new ArrayList<>();

        for (Map<String, Object> item : dataList) {
            Yearamt yearamt = new Yearamt();

            yearamt.setCltcd((Integer) item.get("cltcd"));
            yearamt.setIoflag((String) item.get("ioflag"));
            yearamt.setYyyymm((String) item.get("yyyymm"));

            // balance 값이 null이 아닌 경우 Integer로 변환하여 yearamt에 저장
            if (item.get("balance") != null) {
                yearamt.setYearamt(((Number) item.get("balance")).intValue());
            }

            Object endynVal = item.get("endyn");
            if (endynVal instanceof Boolean) {
                yearamt.setEndyn((Boolean) endynVal ? "N" : "Y");
            } else {
                yearamt.setEndyn("N");
            }

            yearamtList.add(yearamt);
        }

        // 저장
        List<Yearamt> savedList = yearamtRepository.saveAll(yearamtList);

        result.success = true;
        result.data = savedList;
        return result;
    }


    @PostMapping("/delete")
    public AjaxResult deleteYearamt(@RequestParam("id") Integer id) {

        AjaxResult result = new AjaxResult();

        return result ;
    }


}
