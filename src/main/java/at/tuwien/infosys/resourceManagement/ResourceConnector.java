package at.tuwien.infosys.resourceManagement;

import at.tuwien.infosys.entities.DockerHost;


public interface ResourceConnector {
    DockerHost startVM(DockerHost dh);

    void stopDockerHost(DockerHost dh);

    void markHostForRemoval(String hostId);

    void removeHostsWhichAreFlaggedToShutdown();
}
