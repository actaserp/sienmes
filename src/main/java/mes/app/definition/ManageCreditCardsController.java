package mes.app.definition;

import lombok.extern.slf4j.Slf4j;
import mes.Encryption.EncryptionUtil;
import mes.app.definition.service.ManageCreditCardsSecvice;
import mes.domain.entity.TB_IZ010;
import mes.domain.entity.TB_IZ010Id;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.TB_IZ010Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/api/transaction/manageCreditCard")
public class ManageCreditCardsController {
  @Autowired
  ManageCreditCardsSecvice cardsSecvice;

  @Autowired
  TB_IZ010Repository tb_iz010Repository;

  @GetMapping("/read")
  public AjaxResult getCreditCardsList(@RequestParam(value ="spjangcd") String spjangcd,
                                   @RequestParam(value = "txtDescription", required = false) String txtcardnm,
                                   @RequestParam(value = "txtcardnum", required = false) String txtcardnum,
                                   HttpServletRequest request) {
    //log.info("신용카드 등록 read - spjangcd:{}, txtcardnm: {}, txtcardnum:{}",spjangcd,  txtcardnm, txtcardnum);

    List<Map<String, Object>> items = this.cardsSecvice.getCreditCardsList(spjangcd,txtcardnm, txtcardnum);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  //저장
  @PostMapping("/save")
  public AjaxResult SaveCreditCardsList(
      @RequestParam(value ="spjangcd") String spjangcd,
      @RequestParam(value = "cardType") String cardType,
      @RequestParam(value = "cardnum") String cardnum,
      @RequestParam(value = "cardco") String cardco,
      @RequestParam(value = "cardnm") String cardnm,
      @RequestParam(value = "regAsName") String regAsName,
      @RequestParam(value = "expdate") String expdate,
      @RequestParam(value = "ACCID") Integer accid,
      @RequestParam(value = "ACCNUM") String accnum,  // 마스킹되었더라도 무시
      @RequestParam(value = "bankid") Integer bankid,
      @RequestParam(value = "BANKNM") String banknm,
      @RequestParam(value = "useYn") String useYn,
      @RequestParam(value = "baroflag") String baroflag,
      @RequestParam(value = "stldate") String stldate,
      @RequestParam(value = "cardwebid", required = false) String cardwebid,
      @RequestParam(value = "cardwebpw", required = false) String cardwebpw,
      @RequestParam(value = "barocd", required = false) String barocd,
      @RequestParam(value = "baroid", required = false) String baroid,
      @RequestParam(value = "cdpernm") String cdpernm,
      @RequestParam(value = "cdperid") Integer cdperid,
      @RequestParam(value = "relation", required = false) String relation,
      Authentication auth,
      HttpServletRequest request) throws Exception {

    AjaxResult result = new AjaxResult();
    //log.info("신용카드 등록 요청: cardnum={}, accid={}, accnum(입력)={}", cardnum, accid, accnum);

    String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    User user = (User) auth.getPrincipal();
    String username = user.getUserProfile().getName();

    // ✅ 계좌번호 복호화 후 재암호화
    String decryptedAccnum = cardsSecvice.findDecryptedAccountNumberByAccid(accid);
    String reEncryptedAccnum = EncryptionUtil.encrypt(decryptedAccnum);


    // ✅ 카드 PK 구성
    TB_IZ010Id id = new TB_IZ010Id();
    id.setSpjangcd(spjangcd);
    id.setCardnum(EncryptionUtil.encrypt(cardnum));

    Optional<TB_IZ010> optional = tb_iz010Repository.findById(id);

    TB_IZ010 card = optional.orElseGet(TB_IZ010::new);
    card.setId(id);
    card.setCardco(cardco);
    card.setCardnm(cardnm);
    card.setCdperid(cdperid);
    card.setCdpernm(cdpernm);
    card.setIssudate(regAsName);
    card.setExpdate(expdate);
    card.setStldate(stldate);
    card.setBankid(bankid);
    card.setBanknm(banknm);
    card.setAccid(accid);
    card.setAccnum(reEncryptedAccnum);
    card.setBaroflag(baroflag);
    card.setCardwebid(cardwebid);
    card.setCardwebpw((cardwebpw != null && !cardwebpw.isBlank())
        ? EncryptionUtil.encrypt(cardwebpw)
        : null);
    card.setBarocd(barocd);
    card.setBaroid(baroid);
    card.setUseyn(useYn);
    card.setIndate(today);
    card.setInuserid(username);
    card.setCardclafi(cardType);
//  card.setPaymentday(paymentDay); // 사용 중이면 주석 해제

    tb_iz010Repository.save(card);

    result.message = optional.isPresent() ? "수정 완료" : "신규 등록 완료";
    result.success = true;
    return result;
  }

}
