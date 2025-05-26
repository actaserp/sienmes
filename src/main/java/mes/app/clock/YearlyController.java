package mes.app.clock;

import mes.app.clock.service.ClockYearlyService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clock/Yearly")
public class YearlyController {

    @Autowired
    private ClockYearlyService clockYearlyService;

    @GetMapping("/read")
    public AjaxResult getYearlyList(
            @RequestParam(value="year") String year,
            @RequestParam(value="name",required=false) String name,
            @RequestParam(value ="spjangcd") String spjangcd,
            @RequestParam(value="startdate2",required=false) String startdate,
            @RequestParam(value="rtflag",required=false) String rtflag,
            HttpServletRequest request) {


        if (startdate != null && startdate.contains("-")) {
            startdate = startdate.replaceAll("-", "");
        }


        List<Map<String, Object>> items = this.clockYearlyService.getYearlyList(year,name,spjangcd,startdate,rtflag);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/YearlyCreat")
    public AjaxResult getYearlyCreat(
            @RequestParam(value="year") String year,
            @RequestParam(value ="spjangcd") String spjangcd,
            @RequestParam(value="startdate",required=false) String startdate,
            @RequestParam(value="name",required=false) String name,
            HttpServletRequest request) {


        if (startdate != null && startdate.contains("-")) {
            startdate = startdate.replaceAll("-", "");
        }


        List<Map<String, Object>> items = this.clockYearlyService.YearlyCreate(year,spjangcd,startdate,name);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/MonthlyCreate")
    public AjaxResult getMonthlyCreate(
            @RequestParam(value="year") String year,
            @RequestParam(value ="spjangcd") String spjangcd,
            @RequestParam(value="startdate",required=false) String startdate,
            @RequestParam(value="name",required=false) String name,
            HttpServletRequest request) {


        if (startdate != null && startdate.contains("-")) {
            startdate = startdate.replaceAll("-", "");
        }


        List<Map<String, Object>> items = this.clockYearlyService.MonthlyCreate(year,spjangcd,startdate,name);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }



}
