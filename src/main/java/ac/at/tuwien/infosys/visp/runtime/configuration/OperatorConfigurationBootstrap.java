package ac.at.tuwien.infosys.visp.runtime.configuration;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Data
@Service
@DependsOn("configurationprovider")
public class OperatorConfigurationBootstrap {

    public OperatorConfigurationBootstrap(String name) {
        this.name = name;
    }

    public OperatorConfigurationBootstrap() {
    }

    @Autowired
    private Configurationprovider config;

    private String name;

    //TODO replace this with actual resource usage and this should be only a fallback
    private ResourceTriple expected = new ResourceTriple(0.5, 500,300F);

    private Double incommingToOutgoingRatio = 0.5;

    public String getImage(String operator) {
        return config.getProcessingNodeImage();
    }

    public DockerContainer createDockerContainerConfiguration(String operator) {
        return new DockerContainer(operator, operator, expected.getCores(), expected.getMemory(), Math.round(expected.getStorage()));
    }

    public DockerContainer createDockerContainerConfiguration(Operator operator) {
        return new DockerContainer(operator.getType(), operator.getName(), expected.getCores(), expected.getMemory(), Math.round(expected.getStorage()));
    }

}
