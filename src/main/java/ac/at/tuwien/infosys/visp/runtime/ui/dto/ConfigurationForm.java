package ac.at.tuwien.infosys.visp.runtime.ui.dto;


public class ConfigurationForm {

    private String runtimeip;
    private String infrastructureip;
    private String openstackhostimageid;
    private String processingimageid;
    private String reasoner;

    public ConfigurationForm(String runtimeip, String infrastructureip, String openstackhostimageid, String processingimageid, String reasoner) {
        this.runtimeip = runtimeip;
        this.infrastructureip = infrastructureip;
        this.openstackhostimageid = openstackhostimageid;
        this.processingimageid = processingimageid;
        this.reasoner = reasoner;
    }

    public ConfigurationForm() {
    }

    public String getRuntimeip() {
        return runtimeip;
    }

    public void setRuntimeip(String runtimeip) {
        this.runtimeip = runtimeip;
    }

    public String getInfrastructureip() {
        return infrastructureip;
    }

    public void setInfrastructureip(String infrastructureip) {
        this.infrastructureip = infrastructureip;
    }

    public String getOpenstackhostimageid() {
        return openstackhostimageid;
    }

    public void setOpenstackhostimageid(String openstackhostimageid) {
        this.openstackhostimageid = openstackhostimageid;
    }

    public String getProcessingimageid() {
        return processingimageid;
    }

    public void setProcessingimageid(String processingimageid) {
        this.processingimageid = processingimageid;
    }

    public String getReasoner() {
        return reasoner;
    }

    public void setReasoner(String reasoner) {
        this.reasoner = reasoner;
    }
}
