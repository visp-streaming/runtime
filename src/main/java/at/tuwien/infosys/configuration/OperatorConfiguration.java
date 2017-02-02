package at.tuwien.infosys.configuration;


import at.tuwien.infosys.datasources.entities.DockerContainer;
import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service
public class OperatorConfiguration {

    public OperatorConfiguration(String name) {
        this.name = name;
    }


    public OperatorConfiguration() {
    }

    private String name;
    private Double cpuCores = 0.5;
    private Integer memory = 500;
    private Integer storage = 1;
    private Double incommingToOutgoingRatio = 0.5;

    public String getImage(String operator) {
        return "chochreiner/" + "vispprocessingnodes";
    }

    public DockerContainer createDockerContainerConfiguration(String operator) {

        return new DockerContainer(operator, cpuCores, memory, storage);
    }

}
