package mes.domain.entity.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "tb_e064")
public class TB_E064 {

    @EmbeddedId
    private TB_E064_PK id;

    @Column(name = "seq", length = 3)
    private String seq;

    @Column(name = "kcpersonid")
    private Integer kcpersonid;

    @Column(name = "gubun", length = 2)
    private String gubun;

    @Column(name = "remark", length = 30)
    private String remark;

    @Column(name = "kcchk", length = 1)
    private String kcchk;

    @Column(name = "inperid")
    private Integer inperid;

    @Column(name = "indate", length = 8)
    private String indate;
}
