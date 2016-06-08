package at.tuwien.infosys.entities.operators;

public class Source extends Operator {
    private String source;
    private String mechanism;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMechanism() {
        return mechanism;
    }

    public void setMechanism(String mechanism) {
        this.mechanism = mechanism;
    }

}
