package at.tuwien.infosys.configuration;


import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.entities.ResourceTriple;
import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service
public class OperatorConfigurationBootstrap {

    public OperatorConfigurationBootstrap(String name) {
        this.name = name;
    }

    public OperatorConfigurationBootstrap() {
    }

    private String name;
    private ResourceTriple expected = new ResourceTriple(0.5, 500,1F);

    private Double incommingToOutgoingRatio = 0.5;

    public String getImage(String operator) {
        return "chochreiner/" + "vispprocessingnodes";
    }

    public DockerContainer createDockerContainerConfiguration(String operator) {
        return new DockerContainer(operator, expected.getCores(), expected.getMemory(), Math.round(expected.getStorage()));
    }

}
