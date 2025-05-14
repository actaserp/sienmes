package mes.domain.entity.approval;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "TB_PB204")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TB_PB204 {

  @EmbeddedId
  private TB_PB204_PK id;

  @Column(name = "reqdate")
  private String reqdate;

  @Column(name = "vafrdate")
  private String vafrdate;

  @Column(name = "vatodate")
  private String vatodate;

  @Column(name = "vafrtime")
  private String vafrtime;

  @Column(name = "vatotime")
  private String vatotime;

  @Column(name = "ATDCD")
  private String atdcd;

  @Column(name = "daynum")
  private BigDecimal daynum;

  @Column(name = "yearnum")
  private BigDecimal yearnum;

  @Column(name = "reasontxt")
  private String reasontxt;

  @Column(name = "remark")
  private String remark;

  @Column(name = "flag")
  private String flag;

  @Column(name = "flagdate")
  private String flagdate;

  @Column(name = "response")
  private String response;

  @Column(name = "telnum")
  private String telnum;

  @Column(name = "gowhere")
  private String gowhere;

  @Column(name = "yearflag")
  private String yearflag;

  @Column(name = "appdate")
  private String appdate;

  @Column(name = "appgubun")
  private String appgubun;

  @Column(name = "appperid")
  private String appperid;

  @Column(name = "appremark")
  private String appremark;

  @Column(name = "appnum")
  private String appnum;
}
