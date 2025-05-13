package mes.app.definition;

import lombok.extern.slf4j.Slf4j;
import mes.app.aop.DecryptField;
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

    @DecryptField(columns = {"accnum"}, masks = 0)
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

        accnum = accnum.replaceAll("[^0-9]", ""); // 하이픈 등 제거
        Integer pk = null;
        Integer bankid = null;

        if(id != null){
            pk = UtilClass.parseInteger(id);
        }
        bankid = UtilClass.parseInteger(bankname);

        if (!isValidAccountNumber(accnum)) {
            result.success = false;
            result.message = "계좌번호 형식이 유효하지 않습니다.";
            return result;
        }

        accountService.saveAccount(pk, bankid, accnum, accname);

        result.success = true;
        result.message = "저장되었습니다.";

        return result;
    }


    @PostMapping("/delete")
    public AjaxResult deleteAccount(@RequestParam Integer id
    ){

        AjaxResult result = new AjaxResult();

        accountService.deleteAccount(id);

        result.success = true;
        result.message = "삭제되었습니다.";

        return result;
    }

    public static boolean isValidAccountNumber(String accnum) {
        if (accnum == null) return false;

        // 숫자만 포함 & 10~14자리 사이
        return accnum.matches("^\\d{10,14}$");
    }
}
