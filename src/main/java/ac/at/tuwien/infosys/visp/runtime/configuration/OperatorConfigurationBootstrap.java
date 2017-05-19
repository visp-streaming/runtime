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

    public ResourceTriple getExpected(String operatorType) {

        switch (operatorType) {
            case "calculateperformance" : return new ResourceTriple(0.1, 480, 300F);
            case "calculateavailability" : return new ResourceTriple(0.08, 502, 300F);
            case "calculatequality" : return new ResourceTriple(0.07, 527, 300F);
            case "distributedata" : return new ResourceTriple(0.7, 470, 300F);
            case "availability" : return new ResourceTriple(0.2, 573, 300F);
            case "temperature" : return new ResourceTriple(0.05, 440, 300F);
            case "warning" : return new ResourceTriple(0.07, 466, 300F);
            case "generatereport" : return new ResourceTriple(0.05, 452, 300F);
            case "calculateoee" : return new ResourceTriple(0.1, 464, 300F);
            default: new ResourceTriple(0.5, 500,300F);
        }

        return new ResourceTriple(0.5, 500,300F);
    }

    private Double incommingToOutgoingRatio = 0.5;

    public DockerContainer createDockerContainerConfiguration(String operator) {
        return new DockerContainer(operator, operator, getExpected(operator).getCores(), getExpected(operator).getMemory(), Math.round(getExpected(operator).getStorage()));
    }

    public DockerContainer createDockerContainerConfiguration(Operator operator) {
        return new DockerContainer(operator.getType(), operator.getName(), getExpected(operator.getType()).getCores(), getExpected(operator.getType()).getMemory(), Math.round(getExpected(operator.getType()).getStorage()));
    }

}
