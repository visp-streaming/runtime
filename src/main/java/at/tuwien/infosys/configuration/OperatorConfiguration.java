package at.tuwien.infosys.configuration;


import at.tuwien.infosys.entities.DockerContainer;
import org.springframework.stereotype.Service;

@Service
public class OperatorConfiguration {

    public String getImage(String operator) {
        return "chochreiner/" + "vispprocessingnodes";
    }

    public DockerContainer createDockerContainerConfiguration(String operator) {

        return new DockerContainer(operator, 0.33, 200, 1);
    }

}
