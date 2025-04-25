package mes.app.PopBill.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EasyFinBankAccountFormDto {

    private String PopBillId;
    private String PopBillPw;

    private String BankName;
    private String AccountNumber;

    private String BankId;
    private String BankPw;

    private String PaymentPw;
    private String accountType;

    private String identityNumber;




}
