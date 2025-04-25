package mes.app.PopBill.service;


import com.popbill.api.PopbillException;
import com.popbill.api.Response;
import com.popbill.api.easyfin.EasyFinBankAccountForm;
import lombok.extern.slf4j.Slf4j;
import mes.app.PopBill.dto.EasyFinBankAccountFormDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EasyFinBankCustomService {

    @Autowired
    private com.popbill.api.EasyFinBankService easyFinBankService;

    public void Insert_Tb_Account(EasyFinBankAccountFormDto form){


    }
}
