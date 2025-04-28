package mes.app.definition;


import mes.app.definition.service.AccSubjectService;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.AccSubjectRepository;
import mes.domain.repository.AccmanageRepository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/definition/Accsubject")
public class AccsubjectController {

    @Autowired
    AccSubjectRepository accSubjectRepository;

    @Autowired
    AccSubjectService accSubjectService;

    @Autowired
    AccmanageRepository accmanageRepository;

    @GetMapping("/read")
    public AjaxResult getAccList() {
        AjaxResult result = new AjaxResult();
        result.data = this.accSubjectService.getAccList();
        return result;
    }

    @PostMapping("/save")
    public AjaxResult saveAcc(
            @RequestParam(value="acccd") String acccd, // 계정코드
            @RequestParam(value="uacccd" , required=false) String uacccd, // 상위계정
            @RequestParam(value="drcr") String drcr, //대손
            @RequestParam(value="accnm") String accnm, //계정명
            @RequestParam(value="dcpl") String dcpl, //대손
            @RequestParam(value="spyn" , required=false) String spyn, // 전표사용
            @RequestParam(value="accprtnm") String accprtnm, // 양식명 
            @RequestParam(value="etccode" , required=false) String etccode, // 연결코드 accnm
            @RequestParam(value="cacccd" , required=false) String cacccd, // 차감계정 acccd
            @RequestParam(value="acclv", required=false) String acclvStr,
            @RequestParam(value="useyn", required=false) String useyn,
            Authentication auth
    ) {

        User user = (User)auth.getPrincipal();
        AjaxResult result = new AjaxResult();
        Accsubject acc = new Accsubject();

        int lv = 0;
        if (acclvStr == null || acclvStr.trim().isEmpty()) {
            acc.setAcclv(lv);
        } else {
            try {
                acc.setAcclv(Integer.parseInt(acclvStr) + 1);
            } catch (NumberFormatException e) {
                acc.setAcclv(lv); // 또는 예외 처리
            }
        }


        acc.setUseyn(useyn != null ? useyn : "N");
        acc.setSpyn(spyn != null ? spyn : "0");

        acc.setAcccd(acccd);
        acc.setUacccd(uacccd); //상위계정
        acc.setDrcr(drcr);  // 차대
        acc.setAccnm(accnm); // 계정명
        acc.setDcpl(dcpl); // 대손
        acc.setAccprtnm(accprtnm); // 양식명
        acc.setEtccode(etccode); // 연결코드
        acc.setCacccd(cacccd); //차감계정

        this.accSubjectRepository.save(acc);
        result.data = acc;

        return result;

    }

    @PostMapping("/delete")
    public AjaxResult deleteAcc(@RequestParam(value="id") String id ) {
        this.accSubjectRepository.deleteById(id);
        AjaxResult result = new AjaxResult();
        return result;
    }


    @PostMapping("/add")
    @Transactional
    public AjaxResult saveTestMaster(
            @RequestParam("Q") String qJson,  // 문자열로 넘어온 JSON
            @RequestParam("id") String id,    // 'id'는 acccd로 사용
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        List<Accmanage> savedData = new ArrayList<>();

       /* // 기존 데이터 삭제
        if (id != null) {
            accmanageRepository.deleteByAcccd(id);
        }*/

        // JSON 문자열 파싱
        List<Map<String, Object>> data = CommonUtil.loadJsonListMap(qJson);
        for (Map<String, Object> item : data) {
            Accmanage Ag = new Accmanage();
            Ag.setAcccd(id); // acccd는 id 값

            // 각 필드 추출 및 매핑
            Ag.setItemcd((String) item.get("code"));
            Ag.setItemnm((String) item.get("name"));


            Boolean required = (Boolean) item.get("required");
            Ag.setEssyn(required != null && required ? "1" : "0");

            Boolean used = (Boolean) item.get("used");
            Ag.setUseyn(used != null && used ? "Y" : "N");

            savedData.add(accmanageRepository.save(Ag));
        }

        // 저장된 데이터 다시 조회
        Optional<Accmanage> finalData = accmanageRepository.findByAcccd(id);
        result.data = finalData;

        return result;
    }



    /*
* 계정과목관리 차감계정 검색
* */
    @GetMapping("/search_acc")
    public AjaxResult getAccSearchList(
            @RequestParam(value="searchCode", required=false) String code,
            @RequestParam(value="searchName", required=false) String name
    ) {

        AjaxResult result = new AjaxResult();
        result.data = this.accSubjectService.getAccSearchitem(code,name);
        return result;
    }


    @GetMapping("/list")
    public List<Map<String, String>> getAcccdList() {
        return accSubjectService.getAccCodeAndAccnmAndAcclvList();
    }


}
