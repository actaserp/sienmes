/**
  * 팝빌 계좌조회 API Java SDK SpringBoot Example
  *
  * SpringBoot 연동 튜토리얼 안내 : https://developers.popbill.com/guide/easyfinbank/java/getting-started/tutorial?fwn=springboot
  * 연동 기술지원 연락처 : 1600-9854
  * 연동 기술지원 이메일 : code@linkhubcorp.com
  *
  */
package mes.app.PopBill;

import com.popbill.api.*;
import com.popbill.api.easyfin.*;
import lombok.extern.slf4j.Slf4j;
import mes.app.PopBill.dto.EasyFinBankAccountFormDto;
import mes.app.PopBill.service.EasyFinBankCustomService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("EasyFinBankService")
public class EasyFinBankServiceController {

    @Autowired
    private EasyFinBankService easyFinBankService;

    @Autowired
    private EasyFinBankCustomService easyFinBankCustomService;


    //TODO : 조회전용 계정이 경우에 따라 필요하다. 신한은행이나 신협중앙회면 필요함.
    /**
     * BankID -> 국민은행일 경우 인터넷뱅킹 아이디가 필수임.
     * FastID, FastPWD -> 아이엠뱅크, 신한은행, 신협중앙회 일 경우 조회전용계정필수임.
     * **/
    @RequestMapping(value = "registBankAccount", method = RequestMethod.POST)
    public AjaxResult registBankAccount(EasyFinBankAccountFormDto form) {

        AjaxResult result = new AjaxResult();

        String bankName = form.getBankName();

        if(bankName.equals("0004")){
            String bankId = form.getBankId();
            if(bankId == null || bankId.isEmpty()){
                result.success = false;
                result.message = "국민은행은 인터넷뱅킹 아이디가 필수입니다.";
                return result;
            }
        }

        log.info("도달");

        EasyFinBankAccountForm bankInfo = new EasyFinBankAccountForm();

        bankInfo.setBankCode(form.getBankName());
        bankInfo.setAccountNumber(form.getAccountNumber());
        bankInfo.setAccountPWD(form.getPaymentPw());
        bankInfo.setAccountType(form.getAccountType());
        bankInfo.setIdentityNumber(form.getIdentityNumber());
        bankInfo.setBankID(form.getBankId());

        //TODO: 임시로 하드코딩함. 사용자의 사업자번호를 들고와야됨.
        String CorpNum = "1778602466";

        try{
            Response response = easyFinBankService.registBankAccount(CorpNum, bankInfo);



            result.success = true;
            result.message = "계좌등록이 완료되었습니다.";
        } catch (PopbillException e){
            log.error(e.getMessage());
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }
}