package mes.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="tb_accmanage")
@NoArgsConstructor
@Data
@EqualsAndHashCode( callSuper=false)
public class Accmanage {

    @Id
    @Column(name = "acccd")
    String acccd;

    @Column(name = "itemcd")
    String itemcd; // 관리항목코드

    @Column(name = "itemnm")
    String itemnm; //관리항목명

    @Column(name = "essyn")
    String essyn; //필수여부

    @Column(name = "useyn")
    String useyn; //사용여부


}
