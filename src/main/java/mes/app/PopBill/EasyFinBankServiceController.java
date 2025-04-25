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
import mes.app.PopBill.enums.BankJobState;
import mes.app.PopBill.service.EasyFinBankCustomService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("EasyFinBankService")
public class EasyFinBankServiceController {

    @Autowired
    private EasyFinBankService easyFinBankService;

    @Autowired
    private EasyFinBankCustomService easyFinBankCustomService;


    @RequestMapping(value = "listBankAccount", method = RequestMethod.GET)
    public String listBankAccount(Model m) {
        /**
         * 팝빌에 등록된 계좌정보 목록을 반환합니다.
         * - https://developers.popbill.com/reference/easyfinbank/java/api/manage#ListBankAccount
         */
        try {
            String CorpNum = "1778602466";

            EasyFinBankAccount[] bankList = easyFinBankService.listBankAccount(CorpNum);
            m.addAttribute("BankAccountList", bankList);

        } catch (PopbillException e) {
            m.addAttribute("Exception", e);
            return "exception";
        }

        return "EasyFinBank/ListBankAccount";
    }



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

        /*try{
            Response response = easyFinBankService.registBankAccount(CorpNum, bankInfo);

            result.success = true;
            result.message = "계좌등록이 완료되었습니다.";
        } catch (PopbillException e){
            log.error(e.getMessage());
            result.success = false;
            result.message = e.getMessage();
        }*/
        return result;
    }


    @RequestMapping(value = "requestJob", method = RequestMethod.POST)
    public AjaxResult requestJob(@RequestParam String frdate,
                             @RequestParam String todate,
                             @RequestParam String accountnumber,
                             @RequestParam String managementnum) {
        /**
         * 계좌 거래내역을 확인하기 위해 팝빌에 수집요청을 합니다. (조회기간 단위 : 최대 1개월)
         * - 조회일로부터 최대 3개월 이전 내역까지 조회할 수 있습니다.
         * - https://developers.popbill.com/reference/easyfinbank/java/api/job#RequestJob
         */
        AjaxResult result  = new AjaxResult();
        result.success = false;

        if(accountnumber.isEmpty() || managementnum.isEmpty()){
            result.message = "관리코드 및 계좌번호가 누락되었습니다.";
            return result;
        }else{
            frdate = frdate.replace("-", "");
            todate = todate.replaceAll("-", "");
            accountnumber = accountnumber.replaceAll("-", "");
        }

        try {
            String CorpNum = "1778602466";
            String jobID = easyFinBankService.requestJob(CorpNum, managementnum, accountnumber, frdate, todate);

            String returnResult = waitForJobComplete(CorpNum, jobID);

            if(!returnResult.equals(BankJobState.COMPLETE.getCode())){
                result.message = returnResult;
                return result;
            }

            EasyFinBankSearchResult searchInfo = easyFinBankService.search(CorpNum, jobID, null, null, null, null, null);

            if(searchInfo.getCode() != 1){
                result.message = searchInfo.getMessage();
                return result;
            }

            List<EasyFinBankSearchDetail> list = searchInfo.getList();
            List<Map<String, Object>> mapList = convertToMapList(list);
            System.out.println(mapList);

            result.success = true;
            result.data = mapList;


        } catch (PopbillException e) {
            result.message = e.getMessage();
        } catch (InterruptedException e) {
            log.error("에러발생 : {}", e.getMessage());
            return result;
        }

        return result;
    }

    public String waitForJobComplete(String CorpNum, String jobId) throws InterruptedException, PopbillException {
        int maxRetry = 10;
        int inteval = 400;

        for(int i=0; i < maxRetry; i++){
            EasyFinBankJobState jobState = easyFinBankService.getJobState(CorpNum, jobId);
            String jobStateCode = jobState.getJobState(); // 1=대기, 2=진행중, 3=완료
            long errorCode = jobState.getErrorCode();

            if(errorCode != 1){
                log.info("에러코드 발생 {}" ,errorCode);
                return "에러발생";
            }

            BankJobState state = BankJobState.fromCode(jobStateCode);

            if(state == BankJobState.COMPLETE){
                log.info("수집완료");
                return BankJobState.COMPLETE.getCode();
            }

            Thread.sleep(inteval);
        }
        return BankJobState.TIMEOUT.getCode();
    }


    public List<Map<String, Object>> convertToMapList(List<EasyFinBankSearchDetail> detailList) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (EasyFinBankSearchDetail detail : detailList) {
            Map<String, Object> map = new LinkedHashMap<>(); // 순서 유지

            map.put("tid", detail.getTid());
            map.put("accountID", detail.getAccountID());
            map.put("trdate", detail.getTrdate());
            map.put("trserial", detail.getTrserial());
            map.put("trdt", detail.getTrdt());
            map.put("accIn", detail.getAccIn());
            map.put("accOut", detail.getAccOut());
            map.put("balance", detail.getBalance());
            map.put("remark1", detail.getRemark1());
            map.put("remark2", detail.getRemark2());
            map.put("remark3", detail.getRemark3());
            map.put("remark4", detail.getRemark4());
            map.put("regDT", detail.getRegDT());
            map.put("memo", detail.getMemo());

            result.add(map);
        }

        return result;
    }
}