package mes.domain.entity.commute;

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
public class TB_PB201_PK implements Serializable {
    @Column(name = "spjangcd", length = 2)
    private String spjangcd;

    @Column(name = "workym", length = 6)
    private String workym;

    @Column(name = "workday", length = 2)
    private String workday;

    @Column(name = "personid")
    private Integer personid;
}
