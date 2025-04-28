package mes.domain.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="tb_yearamt")
@NoArgsConstructor
@Data
@EqualsAndHashCode( callSuper=false)
public class Yearamt {

    @Column(name = "ioflag")
    String ioflag; //입출금구분

    @Column(name = "yyyymm")
    String yyyymm; //마감년월

    @Id
    @Column(name = "cltcd")
    Integer cltcd; //거래처ID

    @Column(name = "yearamt")
    Integer yearamt; //마감금액

    @Column(name = "endyn")
    String endyn;  // 마감유무

}
