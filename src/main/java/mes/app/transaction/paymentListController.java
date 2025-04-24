package mes.app.transaction;

import mes.app.production.service.EquipmentRunChartService;
import mes.domain.model.AjaxResult;
import mes.domain.repository.EquRunRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transaction/payment_list")
public class paymentListController {
    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    EquipmentRunChartService equipmentRunChartService;

    @Autowired
    EquRunRepository equRunRepository;

    // 차트 searchMainData
    @GetMapping("/read")
    public AjaxResult getEquipmentRunChart(
            @RequestParam(value="date_from", required=false) String date_from,
            @RequestParam(value="date_to", required=false) String date_to,
            @RequestParam(value="id", required=false) Integer id,
            @RequestParam(value="runType", required=false) String runType,
            HttpServletRequest request) {


        AjaxResult result2 = new AjaxResult();
        result2.data = "";
        return result2;
    }
}
