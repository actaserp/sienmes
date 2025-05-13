package mes.app.transaction;


import lombok.extern.slf4j.Slf4j;
import mes.app.aop.DecryptField;
import mes.app.transaction.service.TransactionInputService;
import mes.app.util.UtilClass;
import mes.domain.dto.BankTransitDto;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction/input")
@Slf4j
public class TransactionInputController {

    @Autowired
    TransactionInputService transactionInputService;
    

    @GetMapping("/registerAccount")
    public AjaxResult registerAccount(@RequestParam String spjangcd){

        AjaxResult result = new AjaxResult();

 /*       Object spjangList = session.getAttribute("spjangList");
        Map<String, Object> spjang = UtilClass.getSpjanInfoFromSession(spjancd, session);

        String spjangcd = null;
        if(spjang != null){
            spjangcd = String.valueOf(spjang.get("spjangcd"));
        }*/

        List<Map<String, Object>> accountList = transactionInputService.getAccountList(spjangcd);

        result.data = accountList;

        return  result;

    }

    @DecryptField(column = "account", mask = 4)
    @GetMapping("/history")
    public AjaxResult TransactionHistory(@RequestParam String searchfrdate,
                                         @RequestParam String searchtodate,
                                         @RequestParam String tradetype,
                                         @RequestParam(required = false) String accountNameHidden,
                                         @RequestParam(required = false) String cboCompanyHidden){
        AjaxResult result = new AjaxResult();
        searchfrdate = searchfrdate.replaceAll("-", "");
        searchtodate = searchtodate.replaceAll("-", "");

        Integer parsedAccountId = null;
        if(accountNameHidden != null && !accountNameHidden.isEmpty()){
            parsedAccountId = UtilClass.parseInteger(accountNameHidden);
        }

        Integer parsedCompanyId = null;
        if(cboCompanyHidden != null && !cboCompanyHidden.isEmpty()){
            parsedCompanyId = UtilClass.parseInteger(cboCompanyHidden);
        }

        List<Map<String, Object>> transaction_history = transactionInputService.getTransactionHistory(searchfrdate, searchtodate, tradetype, parsedAccountId, parsedCompanyId);

        result.data = transaction_history;
        return result;
    }

    @PostMapping("/transactionForm")
    public AjaxResult transactionForm(@Valid @RequestBody BankTransitDto data,
                                      BindingResult bindingResult){

        AjaxResult result = new AjaxResult();

        if(bindingResult.hasErrors()){
            result.success = false;
            result.message = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return result;
        }
        try{
            transactionInputService.saveBankTransit(data);
            result.success = true;
            result.message = "저장하였습니다.";
        }catch(Exception e){
            result.success = false;
            result.message = "오류가 발생하였습니다.";

            return result;
        }

        return  result;
    }

    @PostMapping("/edit")
    public AjaxResult transactionEdit(@RequestBody Object list){

        AjaxResult result = new AjaxResult();

        transactionInputService.editBankTransit(list);

        result.message = "수정되었습니다.";
        return  result;

    }

    @GetMapping("/searchDetail")
    public AjaxResult searchTransactionDetail(@RequestParam String companyId,
                                              @RequestParam String searchfrdate,
                                              @RequestParam String searchtodate){
        AjaxResult result = new AjaxResult();

        searchfrdate = searchfrdate.replaceAll("-", "");
        searchtodate = searchtodate.replaceAll("-", "");

        result.data = transactionInputService.searchDetail(UtilClass.parseInteger(companyId), searchfrdate, searchtodate);

        return  result;
    }

    @PostMapping("/delete")
    public AjaxResult transactionDelete(@RequestParam String idList){

        AjaxResult result = new AjaxResult();

        List<Integer> parsedidList = Arrays.stream(idList.split(","))
                        .map(Integer::parseInt)
                                .toList();

        transactionInputService.deleteBanktransit(parsedidList);

        result.message = "삭제되었습니다.";
        return  result;

    }
}
