package mes.app.transaction;


import mes.app.transaction.service.TransactionInputService;
import mes.app.util.UtilClass;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction/input")
public class TransactionInputController {

    @Autowired
    TransactionInputService transactionInputService;
    

    @GetMapping("/registerAccount")
    public AjaxResult registerAccount(){

        AjaxResult result = new AjaxResult();

        List<Map<String, Object>> accountList = transactionInputService.getAccountList();

        result.data = accountList;

        return  result;

    }

    @GetMapping("/history")
    public AjaxResult TransactionHistory(@RequestParam String searchfrdate,
                                         @RequestParam String searchtodate,
                                         @RequestParam String tradetype,
                                         @RequestParam(required = false) String accountNameHidden){
        AjaxResult result = new AjaxResult();
        searchfrdate = searchfrdate.replaceAll("-", "");
        searchtodate = searchtodate.replaceAll("-", "");

        Integer parsedAccountId = null;
        if(accountNameHidden != null && !accountNameHidden.isEmpty()){
            parsedAccountId = UtilClass.parseInteger(accountNameHidden);
        }

        List<Map<String, Object>> transaction_history = transactionInputService.getTransactionHistory(searchfrdate, searchtodate, tradetype, parsedAccountId);

        result.data = transaction_history;
        return result;
    }
}
