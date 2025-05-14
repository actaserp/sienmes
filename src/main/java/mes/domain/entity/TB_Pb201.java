package mes.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_pb201")
public class TB_Pb201 {

    @EmbeddedId
    private TB_Pb201Id id; // 복합키

    @Column(name="holiyn")
    String holiyn;

    @Column(name="workyn")
    String workyn;

    @Column(name="workcd")
    String workcd;

    @Column(name="starttime")
    String starttime;

    @Column(name="endtime")
    String endtime;

    @Column(name="remark")
    String remark;

    @Column(name="fixflag")
    String fixflag;


    @Column(name="worktime")
    BigDecimal worktime;

    @Column(name="nomaltime")
    BigDecimal nomaltime;

    @Column(name="overtime")
    BigDecimal overtime;

    @Column(name="nighttime")
    BigDecimal nighttime;

    @Column(name="holitime")
    BigDecimal holitime;

    @Column(name="jitime")
    BigDecimal jitime;

    @Column(name="jotime")
    BigDecimal jotime;

    @Column(name="yuntime")
    BigDecimal yuntime;

    @Column(name="abtime")
    BigDecimal abtime;

    @Column(name="bantime")
    BigDecimal bantime;

    @Column(name="adttime01")
    BigDecimal adttime01;

    @Column(name="adttime02")
    BigDecimal adttime02;

    @Column(name="adttime03")
    BigDecimal adttime03;

    @Column(name="adttime04")
    BigDecimal adttime04;

    @Column(name="adttime05")
    BigDecimal adttime05;

    @Column(name="adttime06")
    BigDecimal adttime06;

    @Column(name="adttime07")
    BigDecimal adttime07;

    @Column(name="worknum")
    Integer worknum;




}
