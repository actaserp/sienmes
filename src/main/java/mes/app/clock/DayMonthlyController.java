package mes.app.clock;

import mes.app.clock.service.DayMonthlyService;
import mes.domain.model.AjaxResult;
import mes.domain.repository.commute.TB_PB201Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clock/DayMonthly")
public class DayMonthlyController {

    @Autowired
    private DayMonthlyService dayMonthlyService;
    @Autowired
    private TB_PB201Repository tbPb201Repository;



    @GetMapping("/read")
    public AjaxResult getDayList(
            @RequestParam(value="work_division", required=false) String work_division,
            @RequestParam(value="serchday", required=false) String serchday,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        if (serchday != null && serchday.contains("-")) {
            serchday = serchday.replaceAll("-", "");
        }

        List<Map<String, Object>> items = this.dayMonthlyService.getDayList(work_division, serchday);
        result.data = items;
        return result;
    }


}
