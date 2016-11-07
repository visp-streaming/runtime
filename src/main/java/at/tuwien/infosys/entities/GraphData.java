package at.tuwien.infosys.entities;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder
public class GraphData {

    private String time;
    private Integer vmUp;
    private Integer vmDown;
    private Integer operatorUp;
    private Integer operatorDown;
    private Integer operatorMigrate;
    private Integer totalVMs;

    public GraphData(String time) {
        this.time = time;
        this.vmUp = 0;
        this.vmDown = 0;
        this.operatorUp = 0;
        this.operatorDown = 0;
        this.operatorMigrate = 0;
        this.totalVMs = 0;
    }

    public void vmUpInc() {
        this.vmUp++;
    }

    public void vmDownInc() {
        this.vmDown++;
    }

    public void operatorUpInc() {
        this.operatorUp++;
    }

    public void operatorDownInc() {
        this.operatorDown++;
    }

    public void operatorMigrateInc() {
        this.operatorMigrate++;
    }


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getVmUp() {
        return vmUp;
    }

    public void setVmUp(Integer vmUp) {
        this.vmUp = vmUp;
    }

    public Integer getVmDown() {
        return vmDown;
    }

    public void setVmDown(Integer vmDown) {
        this.vmDown = vmDown;
    }

    public Integer getOperatorUp() {
        return operatorUp;
    }

    public void setOperatorUp(Integer operatorUp) {
        this.operatorUp = operatorUp;
    }

    public Integer getOperatorDown() {
        return operatorDown;
    }

    public void setOperatorDown(Integer operatorDown) {
        this.operatorDown = operatorDown;
    }

    public Integer getOperatorMigrate() {
        return operatorMigrate;
    }

    public void setOperatorMigrate(Integer operatorMigrate) {
        this.operatorMigrate = operatorMigrate;
    }

    public Integer getTotalVMs() {
        return totalVMs;
    }

    public void setTotalVMs(Integer totalVMs) {
        this.totalVMs = totalVMs;
    }

    @Override
    public String toString() {
        return "GraphData{" +
                "time=" + time +
                ", vmUp=" + vmUp +
                ", vmDown=" + vmDown +
                ", operatorUp=" + operatorUp +
                ", operatorDown=" + operatorDown +
                ", operatorMigrate=" + operatorMigrate +
                ", totalVMs=" + totalVMs +
                '}';
    }
}
