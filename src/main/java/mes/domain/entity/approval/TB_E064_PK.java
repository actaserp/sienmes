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
public class TB_E064_PK implements Serializable {

    @Column(name = "spjangcd", length = 2)
    private String spjangcd;

    @Column(name = "papercd", length = 2)
    private String papercd;

    @Column(name = "personid")
    private Integer personid;

    @Column(name = "no", length = 3)
    private String no;
}
