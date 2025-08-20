package mes.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "process_test")
public class ProcessTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_test_id")
    private Integer processTestId;

    @Column(name = "job_res_id", nullable = false)
    private Integer jobResId;

    @Column(name = "mat_incoming", length = 1)
    private String matIncoming = "0";

    @Column(name = "mat_weighing", length = 1)
    private String matWeighing = "0";

    @Column(name = "tank_cleaning", length = 1)
    private String tankCleaning = "0";

    @Column(name = "input_mixing", length = 1)
    private String inputMixing = "0";

    @Column(name = "heat_sterilization", length = 1)
    private String heatSterilization = "0";

    @Column(name = "filtration", length = 1)
    private String filtration = "0";

    @Column(name = "quality_check", length = 1)
    private String qualityCheck = "0";

    @Column(name = "metal_detect", length = 1)
    private String metalDetect = "0";

    @Column(name = "packaging", length = 1)
    private String packaging = "0";

    @Column(name = "product_inspect", length = 1)
    private String productInspect = "0";

    @Column(name = "storage_shipping", length = 1)
    private String storageShipping = "0";

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "qc_first_brix", length = 50)
    private String qcFirstBrix;

    @Column(name = "qc_first_salt", length = 50)
    private String qcFirstSalt;

    @Column(name = "qc_second_brix", length = 50)
    private String qcSecondBrix;

    @Column(name = "qc_second_salt", length = 50)
    private String qcSecondSalt;

    @Column(name = "qc_final_brix", length = 50)
    private String qcFinalBrix;

    @Column(name = "qc_final_salt", length = 50)
    private String qcFinalSalt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "mat_name", length = 200)
    private String matName;

    @Column(name = "workcenter_name", length = 100)
    private String workcenterName;

    @Column(name = "food_type", length = 100)
    private String foodType;

    @Column(name = "production_date", length = 8)
    private String productionDate;

    @Column(name = "validate", length = 5)
    private String validate;

    @Column(name = "storage_method", length = 100)
    private String storageMethod;

    @Column(name = "packaging_spec", length = 100)
    private String packagingSpec;

    @Column(name = "mixing_amount", length = 100)
    private String mixingAmount;

    @Column(name = "packaging_mat", length = 100)
    private String packagingMat;

    @Column(name = "lodcell", length = 10)
    private String lodcell;
}
