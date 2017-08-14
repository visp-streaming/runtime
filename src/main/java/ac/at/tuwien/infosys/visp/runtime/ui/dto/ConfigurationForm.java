package ac.at.tuwien.infosys.visp.runtime.ui.dto;


import lombok.Data;

@Data
public class ConfigurationForm {

    private String runtimeip;
    private String infrastructureip;
    private String openstackhostimageid;
    private String processingimageid;
    private String reasoner;
    private Integer btu;
    private Integer monitoringperiod;
    private Integer availabilitycheck;
    private Integer simulatestartup;
    private Integer shutdowngrace;
    private Integer reasoninginterval;
    private Integer upscalingthreshold;

    public ConfigurationForm() {
    }

}
