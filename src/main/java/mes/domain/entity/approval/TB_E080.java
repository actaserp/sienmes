package mes.domain.entity.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "TB_E080")
@Setter
@Getter
@NoArgsConstructor
public class TB_E080 {

    @EmbeddedId
    private TB_E080_PK id;

    @Column(length = 3)
    private String seq;

    @Column(length = 1)
    private String flag;

    @Column(length = 8)
    private String repodate;

    @Column(length = 3)
    private String papercd;

    @Column(length = 10)
    private String repoperid;

    @Column(length = 100)
    private String title;

    @Column(length = 3)
    private String appgubun;

    @Column(length = 8)
    private String appdate;

    @Column(length = 100)
    private String remark;

    @Column(length = 10)
    private String inperid;

    @Column(length = 8)
    private String indate;

    @Column(length = 1)
    private String adflag;
}
