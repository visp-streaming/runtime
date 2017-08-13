package ac.at.tuwien.infosys.visp.runtime.ui.dto;


import lombok.Data;

@Data
public class ConfigurationForm {

    private String runtimeip;
    private String infrastructureip;
    private String openstackhostimageid;
    private String processingimageid;
    private String reasoner;
    private String btu;

    public ConfigurationForm(String runtimeip, String infrastructureip, String openstackhostimageid, String processingimageid, String reasoner, String btu) {
        this.runtimeip = runtimeip;
        this.infrastructureip = infrastructureip;
        this.openstackhostimageid = openstackhostimageid;
        this.processingimageid = processingimageid;
        this.reasoner = reasoner;
        this.btu = btu;
    }

    public ConfigurationForm() {
    }

}
