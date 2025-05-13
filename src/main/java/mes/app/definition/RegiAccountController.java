package mes.app.definition;

import lombok.extern.slf4j.Slf4j;
import mes.app.definition.service.RegiAccountService;
import mes.app.util.UtilClass;
import mes.domain.model.AjaxResult;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/definition/account")
public class RegiAccountController {

    private final RegiAccountService accountService;

    public RegiAccountController(RegiAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/read")
    public AjaxResult getRegiAccountList(@RequestParam String bankid,
                                         @RequestParam String accountnum,
                                         @RequestParam String spjangcd
                                         ){

        AjaxResult result = new AjaxResult();

        Integer bankId = UtilClass.parseInteger(bankid);

        result.data = accountService.getAccountList(bankId, accountnum, spjangcd);

        return result;
    }

    @PostMapping("/save")
    public AjaxResult saveAccount(@RequestParam(required = false) String id,
                                         @RequestParam String bankname,
                                         @RequestParam String accnum,
                                         @RequestParam String accname
    ){

        AjaxResult result = new AjaxResult();

        result.success = true;
        result.message = "저장되었습니다.";

        return result;
    }
}
