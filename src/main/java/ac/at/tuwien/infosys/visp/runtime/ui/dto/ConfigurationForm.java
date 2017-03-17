package ac.at.tuwien.infosys.visp.runtime.ui.dto;


public class ConfigurationForm {

    private String runtimeip;
    private String infrastructureip;
    private String openstackhostimageid;
    private String processingimageid;


    public ConfigurationForm(String runtimeip, String infrastructureip, String openstackhostimageid, String processingimageid) {
        this.runtimeip = runtimeip;
        this.infrastructureip = infrastructureip;
        this.openstackhostimageid = openstackhostimageid;
        this.processingimageid = processingimageid;
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
}
