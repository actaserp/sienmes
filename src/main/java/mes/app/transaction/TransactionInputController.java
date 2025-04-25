package mes.app.transaction;


import mes.app.transaction.service.TransactionInputService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
