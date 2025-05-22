package mes.app.definition;

import lombok.extern.slf4j.Slf4j;
import mes.app.definition.service.ManageCreditCardsSecvice;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/transaction/manageCreditCard")
public class ManageCreditCardsController {
  @Autowired
  ManageCreditCardsSecvice cardsSecvice;

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

}
