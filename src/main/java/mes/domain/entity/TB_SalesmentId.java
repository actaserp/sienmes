package mes.domain.entity;


import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TB_SalesmentId implements Serializable {

    private String misdate;
    private String misnum;
}
