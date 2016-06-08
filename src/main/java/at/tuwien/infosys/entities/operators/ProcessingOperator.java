package at.tuwien.infosys.entities.operators;


public class ProcessingOperator extends Operator {
    private String scalingThreshold;

    public String getScalingThreshold() {
        return scalingThreshold;
    }

    public void setScalingThreshold(String scalingThreshold) {
        this.scalingThreshold = scalingThreshold;
    }

}
