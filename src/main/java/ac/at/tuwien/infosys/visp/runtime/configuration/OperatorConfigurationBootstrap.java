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

        //TODO put them in a txt file

        switch (operatorType) {
            case "calculateperformance" : return new ResourceTriple(0.4, 534, 300F);
            case "calculateavailability" : return new ResourceTriple(0.56, 417, 300F);
            case "calculatequality" : return new ResourceTriple(0.42, 527, 300F);
            case "distributedata" : return new ResourceTriple(1.2, 554, 300F);
            case "availability" : return new ResourceTriple(0.4, 629, 300F);
            case "temperature" : return new ResourceTriple(0.25, 515, 300F);
            case "warning" : return new ResourceTriple(0.14, 509, 300F);
            case "generatereport" : return new ResourceTriple(0.3, 452, 300F);
            case "calculateoee" : return new ResourceTriple(0.4, 513, 300F);
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
