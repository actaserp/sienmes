package mes.app.definition;

import mes.app.definition.service.YearamtService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/definition/yearamt")
public class YearamtController {

    @Autowired
    YearamtService yearamtService;

    @GetMapping("/read")
    public AjaxResult getYearamtList(
            @RequestParam(value="cboYear", required=false) String year,
            @RequestParam(value="ioflag", required=false) String ioflag,
            @RequestParam(value="searchtradenm", required=false) String name
    ) {
        AjaxResult result = new AjaxResult();
        result.data = this.yearamtService.getYearamtList(year,ioflag,name);
        return result;
    }

}
