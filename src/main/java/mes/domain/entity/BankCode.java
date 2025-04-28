package mes.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_xbank")  //  테이블 이름 주의
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class BankCode extends AbstractAuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BANKID") // 은행 ID
    private Integer id;

    @Column(name = "BANKNM")
    private String name;  // 은행명

    @Column(name = "BANKPOPCD")
    private String bankPopCd;  // 팝빌관리코드

    @Column(name = "BANKPOPNM")
    private String bankPopNm;  // 팝빌관리코드명

    @Column(name = "BANKSUBCD")
    private String bankSubCd;  // 기관관리코드

    @Column(name = "BANKSUBNM")
    private String bankSubNm;  // 기관관리코드명

    @Column(name = "REMARK")
    private String remark;  // 비고

    @Column(name = "USEYN")
    private String useYn;  // 사용여부
}
