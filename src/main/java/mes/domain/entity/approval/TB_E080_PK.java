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

    @Column(length = 8, nullable = false)
    private String custcd;

    @Column(length = 2, nullable = false)
    private String spjangcd;

    @Column(length = 20, nullable = false)
    private String appnum;

    @Column(length = 10, nullable = false)
    private String appperid;
}
