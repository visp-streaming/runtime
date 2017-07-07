package ac.at.tuwien.infosys.visp.runtime.ui.dto;


import lombok.Data;

@Data
public class AddCustomHostForm {

    private String instanceName;
    private String url;
    private String poolname;
    private Double cost;
    private Integer frequency;
    private Float storage;
    private Integer memory;
    private Double cores;

}
