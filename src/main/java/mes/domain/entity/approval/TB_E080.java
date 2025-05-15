package mes.domain.entity.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "tb_e080")
public class TB_E080 {

    @EmbeddedId
    private TB_E080_PK id;

    @Column(name = "flag", length = 1)
    private String flag;

    @Column(name = "repodate", length = 8)
    private String repodate;

    @Column(name = "papercd", length = 3)
    private String papercd;

    @Column(name = "repoperid")
    private Integer repoperid;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "appgubun", length = 3)
    private String appgubun;

    @Column(name = "inperid")
    private Integer inperid;

    @Column(name = "indate", length = 8)
    private String indate;

    @Column(name = "adflag", length = 1)
    private String adflag;

    @Column(name = "prtflag", length = 30)
    private String prtflag;

    @Column(name = "gubun", length = 2)
    private String gubun;
}
