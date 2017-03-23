package ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq;

import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdate;

import java.util.List;

public class UpdateResult {
    public enum UpdateStatus  {SUCCESSFUL, RUNTIMES_NOT_AVAILABLE, LOCAL_DEPLOYMENT_NOT_POSSIBLE, DEPLOYMENT_NOT_POSSIBLE}

    private UpdateStatus status;

    private String errorMessage;

    private List<TopologyUpdate> updatesPerformed;
    public String dotPath;
    public boolean distributedUpdateSuccessful;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UpdateResult(List<TopologyUpdate> updatesPerformed, String dotPath, UpdateStatus status) {
        this.updatesPerformed = updatesPerformed;
        this.dotPath = dotPath;
        this.status = status;
        this.errorMessage = "";
    }

    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }

    public List<TopologyUpdate> getUpdatesPerformed() {
        return updatesPerformed;
    }

    public void setUpdatesPerformed(List<TopologyUpdate> updatesPerformed) {
        this.updatesPerformed = updatesPerformed;
    }

    public String getDotPath() {
        return dotPath;
    }

    public void setDotPath(String dotPath) {
        this.dotPath = dotPath;
    }

    public boolean isDistributedUpdateSuccessful() {
        return distributedUpdateSuccessful;
    }

    public void setDistributedUpdateSuccessful(boolean distributedUpdateSuccessful) {
        this.distributedUpdateSuccessful = distributedUpdateSuccessful;
    }

    @Override
    public String toString() {
        return "UpdateResult{" +
                "status=" + status +
                ", updatesPerformed=" + updatesPerformed +
                ", dotPath='" + dotPath + '\'' +
                ", distributedUpdateSuccessful=" + distributedUpdateSuccessful +
                '}';
    }


}