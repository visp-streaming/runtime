package at.tuwien.infosys.entities.operators;


public class ProcessingOperator extends Operator {
    private String scalingThreshold;

    private String expectedDuration;
    private String queueThreshold;


    public String getScalingThreshold() {
        return scalingThreshold;
    }

    public void setScalingThreshold(String scalingThreshold) {
        this.scalingThreshold = scalingThreshold;
    }

    public String getExpectedDuration() {
        return expectedDuration;
    }

    public void setExpectedDuration(String expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    public String getQueueThreshold() {
        return queueThreshold;
    }

    public void setQueueThreshold(String queueThreshold) {
        this.queueThreshold = queueThreshold;
    }
}
