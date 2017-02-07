package ac.at.tuwien.infosys.visp.runtime.entities;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder
public class GraphData {

    private String time;
    private Integer vmUp;
    private Integer vmDown;
    private Integer vmprolongLease;
    private Integer operatorUp;
    private Integer operatorDown;
    private Integer operatorMigrate;
    private Integer totalVMs;

    public GraphData(String time) {
        this.time = time;
        this.vmUp = 0;
        this.vmprolongLease = 0;
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

    public void vmProlongLease() {
        this.vmprolongLease++;
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

}
