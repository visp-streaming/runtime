package ac.at.tuwien.infosys.visp.runtime.restAPI.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A DTO for consuming the testDeploymentForTopologyFile endpoint of other VISP instances
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestDeploymentDTO {
    private String errorMessage;
    private boolean deploymentPossible;

    public TestDeploymentDTO() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isDeploymentPossible() {
        return deploymentPossible;
    }

    public void setDeploymentPossible(boolean deploymentPossible) {
        this.deploymentPossible = deploymentPossible;
    }

    @Override
    public String toString() {
        return "TestDeploymentDTO{" +
                "errorMessage='" + errorMessage + '\'' +
                ", deploymentPossible=" + deploymentPossible +
                '}';
    }
}
