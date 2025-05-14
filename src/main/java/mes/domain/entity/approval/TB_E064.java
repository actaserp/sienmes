package mes.domain.entity.approval;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "TB_E064")
@Setter
@Getter
@NoArgsConstructor
public class TB_E064 {

    @EmbeddedId
    private TB_E064_PK id;

    @Column(length = 3)
    private String seq;

    @Column(length = 10)
    private String kcperid;

    @Column(length = 3)
    private String gubun;

    @Column(length = 100)
    private String remark;

    @Column(length = 10)
    private String inperid;

    @Column(length = 8)
    private String indate;

    @Column(length = 1)
    private String kcchk;
}
