package at.tuwien.infosys.entities.operators;


import java.util.ArrayList;
import java.util.List;

public abstract class Operator {
    protected String name;
    protected String type;
    protected List<Operator> sources = new ArrayList<>();
    protected List<Operator> affectedInstances = new ArrayList<>();
    protected List<String> sourcesText = new ArrayList<>();
    protected String allowedLocations;
    protected String inputFormat;
    protected String outputFormat;

    private String messageBrokerHost;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Operator> getSources() {
        return sources;
    }

    public void setSources(List<Operator> sources) {
        this.sources = sources;
    }

    public String getAllowedLocations() {
        return allowedLocations;
    }

    public void setAllowedLocations(String allowedLocations) {
        this.allowedLocations = allowedLocations;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getMessageBrokerHost() {
        return messageBrokerHost;
    }

    public void setMessageBrokerHost(String messageBrokerHost) {
        this.messageBrokerHost = messageBrokerHost;
    }

    public List<String> getSourcesText() {
        return sourcesText;
    }

    public void setSourcesText(List<String> sourcesText) {
        this.sourcesText = sourcesText;
    }

    public List<Operator> getAffectedInstances() {
        return affectedInstances;
    }

    public void setAffectedInstances(List<Operator> affectedInstances) {
        this.affectedInstances = affectedInstances;
    }

    @Override
    public String toString() {
        return "Operator{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", sources=" + sources +
                ", affectedInstances=" + affectedInstances +
                ", sourcesText=" + sourcesText +
                ", allowedLocations='" + allowedLocations + '\'' +
                ", inputFormat='" + inputFormat + '\'' +
                ", outputFormat='" + outputFormat + '\'' +
                ", messageBrokerHost='" + messageBrokerHost + '\'' +
                '}';
    }
}
