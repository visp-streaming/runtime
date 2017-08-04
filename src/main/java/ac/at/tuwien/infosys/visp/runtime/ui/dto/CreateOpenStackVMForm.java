package ac.at.tuwien.infosys.visp.runtime.ui.dto;

import lombok.Data;

@Data
public class CreateOpenStackVMForm {

    private String poolname;
    private String flavour;
    private Double cost;
    private String instanceName;
    private Integer frequency;
    private Integer instancecount;

}
