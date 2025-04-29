package mes.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_banktransit")
public class TB_BANKTRANSIT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ioid;

    @Column(length = 1)
    private String ioflag;

    @Column(length = 50)
    private String tid;

    @Column(length = 8)
    private String trdate;

    private Integer trserial;

    @Column(length = 20)
    private String trdt;

    private Integer accin;
    private Integer accout;
    private Integer balance;

    @Column(length = 500)
    private String remark1;

    @Column(length = 500)
    private String remark2;

    @Column(length = 500)
    private String remark3;

    @Column(length = 500)
    private String remark4;

    @Column(length = 20)
    private String regdt;

    @Column(length = 20)
    private String regpernm;

    @Column(length = 100)
    private String memo;

    @Column(length = 30)
    private String jobid;

    private Integer cltcd;
    private Integer trid;

    @Column(length = 1)
    private String iotype;

    @Column(length = 50)
    private String banknm;

    @Column(length = 50)
    private String accnum;

    private Integer accid;

    @Column(length = 50)
    private String etcremark;

    @Column(length = 50)
    private String eumnum;

    @Column(length = 8)
    private String eumfrdt;

    @Column(length = 8)
    private String eumtodt;

    private Integer feeamt;

    @Column(length = 1)
    private String feeflag;

    @Column(length = 8)
    private String acccd;
}
