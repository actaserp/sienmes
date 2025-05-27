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
                                   @RequestParam(value = "txtcardnm", required = false) String txtcardnm,
                                   @RequestParam(value = "txtcardnum", required = false) String txtcardnum,
                                   HttpServletRequest request) {
    log.info("신용카드 등록 read - spjangcd:{}, txtcardnm: {}, txtcardnum:{}",spjangcd,  txtcardnm, txtcardnum);

    List<Map<String, Object>> items = this.cardsSecvice.getCreditCardsList(spjangcd,txtcardnm, txtcardnum);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  //저장
  @PostMapping("/save")
  public AjaxResult SaveCreditCardsList(
      @RequestParam("spjangcd") String spjangcd,
      @RequestParam("cardType") String cardType,
      @RequestParam("cardnum") String cardnum, // 마스킹된 값 (표시용)
      @RequestParam("cardnum_real") String cardnum_real, // 원본 카드번호 (실제 저장용)
      @RequestParam("cardco") String cardco,
      @RequestParam("cardnm") String cardnm,
      @RequestParam("regAsName") String regAsName,
      @RequestParam("expdate") String expdate,
      @RequestParam("ACCID") Integer accid,
      @RequestParam("ACCNUM") String accnum,
      @RequestParam("bankid") Integer bankid,
      @RequestParam("BANKNM") String banknm,
      @RequestParam("useYn") String useYn,
      @RequestParam("baroflag") String baroflag,
      @RequestParam("stldate") String stldate,
      @RequestParam(value = "cardwebid", required = false) String cardwebid,
      @RequestParam(value = "cardwebpw", required = false) String cardwebpw,
      @RequestParam(value = "barocd", required = false) String barocd,
      @RequestParam(value = "baroid", required = false) String baroid,
      @RequestParam("cdpernm") String cdpernm,
      @RequestParam("cdperid") Integer cdperid,
      @RequestParam(value = "relation", required = false) String relation,
      Authentication auth,
      HttpServletRequest request
  ) throws Exception {

    AjaxResult result = new AjaxResult();

    // 사용자 정보 및 날짜
    String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    User user = (User) auth.getPrincipal();
    String username = user.getUserProfile().getName();

    // ✅ 카드번호 암호화 (원본 사용)
    String encryptedCardnum = EncryptionUtil.encrypt(cardnum_real);

    // ✅ 계좌번호 복호화 후 재암호화
    String decryptedAccnum = cardsSecvice.findDecryptedAccountNumberByAccid(accid);
    String reEncryptedAccnum = EncryptionUtil.encrypt(decryptedAccnum);

    // ✅ 카드 존재 여부 확인
    Optional<TB_IZ010> optional = tb_iz010Repository.findBySpjangcdAndCardnum(spjangcd, encryptedCardnum);

    // ✅ 카드 ID 구성
    TB_IZ010Id id = new TB_IZ010Id();
    id.setSpjangcd(spjangcd);
    id.setCardnum(encryptedCardnum);

    // ✅ 신규 or 기존 카드 객체 준비
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

    // ✅ 저장
    tb_iz010Repository.save(card);

    // ✅ 결과 반환
    result.message = optional.isPresent() ? "수정 완료" : "신규 등록 완료";
    result.success = true;
    return result;
  }


}
