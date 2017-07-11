package ac.at.tuwien.infosys.visp.runtime.configuration;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Data
@Service
@DependsOn("configurationprovider")
public class OperatorConfigurationBootstrap {

    @Autowired
    private Configurationprovider config;

    private Map<String, ResourceTriple> operatorConfiguration = new HashMap<>();

    private Double incommingToOutgoingRatio = 0.5;

    private static final Logger LOG = LoggerFactory.getLogger(OperatorConfigurationBootstrap.class);

    public OperatorConfigurationBootstrap() {
        initializeOperatorConfiguration();
    }

    public OperatorConfigurationBootstrap(Boolean noInitialization) { }


    public ResourceTriple getExpected(String operatorType) {
        if (operatorConfiguration.containsKey(operatorType)) {
            return operatorConfiguration.get(operatorType);
        } else {
            //Default configuration
            return new ResourceTriple(0.5, 500,300F);
        }
    }


    public DockerContainer createDockerContainerConfiguration(String operator) {
        return new DockerContainer(operator, operator, getExpected(operator).getCores(), getExpected(operator).getMemory(), Math.round(getExpected(operator).getStorage()));
    }

    public DockerContainer createDockerContainerConfiguration(Operator operator) {
        return new DockerContainer(operator.getType(), operator.getName(), getExpected(operator.getType()).getCores(), getExpected(operator.getType()).getMemory(), Math.round(getExpected(operator.getType()).getStorage()));
    }

    public void initializeOperatorConfiguration() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("runtimeConfiguration/operatorConfiguration.json")));
            parseOperatorConfigurationData(content);
        } catch (IOException e) {
            LOG.error("Operator configuration could not be found.");
        } catch (NumberFormatException e) {
            LOG.error("Individual inputs for the operator configuration could not be parsed.");
        }
    }

    public void parseOperatorConfigurationData(String content) throws IOException {
            final JsonNode arrNode = new ObjectMapper().readTree(content).get("operators");

            if (arrNode.isArray()) {
                for (final JsonNode objNode : arrNode) {
                    String name = objNode.findValue("name").toString().replace("\"", "");
                    Double cores = Double.valueOf(objNode.findValue("cores").toString());
                    Integer memory = Integer.valueOf(objNode.findValue("memory").toString());
                    Float storage = Float.valueOf(objNode.findValue("storage").toString());

                    operatorConfiguration.put(name, new ResourceTriple(cores, memory, storage));
                }
            }
    }

}
