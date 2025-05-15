package mes.domain.entity.approval;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class TB_E080_PK implements Serializable {

    @Column(name = "spjangcd", length = 2)
    private String spjangcd;

    @Column(name = "appnum", length = 2)
    private String appnum;

    @Column(name = "personid")
    private Integer personid;

    @Column(name = "seq", length = 3)
    private String seq;
}
