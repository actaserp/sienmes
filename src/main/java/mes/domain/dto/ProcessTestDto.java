package mes.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessTestDto {
    private Integer process_test_id;
    private Integer job_res_id;

    private String mat_incoming;
    private String mat_weighing;
    private String tank_cleaning;
    private String input_mixing;
    private String heat_sterilization;
    private String filtration;
    private String quality_check;
    private String metal_detect;
    private String packaging;
    private String product_inspect;
    private String storage_shipping;

    private String remark;

    private String qc_first_brix;
    private String qc_first_salt;
    private String qc_second_brix;
    private String qc_second_salt;
    private String qc_final_brix;
    private String qc_final_salt;

    private String mat_name;
    private String workcenter_name;
    private String food_type;
    private String production_date;
    private String validate;
    private String storage_method;
    private String packaging_spec;
    private String mixing_amount;
    private String packaging_mat;
    private String lodcell;
}
