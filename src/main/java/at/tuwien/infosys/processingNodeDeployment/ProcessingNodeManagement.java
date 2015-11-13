package at.tuwien.infosys.processingNodeDeployment;

import at.tuwien.infosys.configuration.Topology;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.Operator;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ProcessingNodeManagement {

    @Autowired
    DockerContainerManagement dcm;

    @Autowired
    private Topology topology;

    private Integer graceperiod = 2;

    private Map<DateTime, DockerContainer> toBeShutDown = new HashMap<>();


    //TODO remove host definition from here

    private String dockerHost = "http://128.130.172.224:2375";
    //TODO split up for different hosts broker, redis, ...
    private String infrastructureHost = "128.130.172.225";


    private void housekeeping() {
        for (Map.Entry<DateTime, DockerContainer> container : toBeShutDown.entrySet()) {
            DateTime now = new DateTime(DateTimeZone.UTC);

            if (now.isAfter(container.getKey().plusMinutes(2))) {
                try {
                    dcm.removeContainer(container.getValue());
                } catch (DockerException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void initializeTopology() {
        try {
            for (Operator op : topology.getTopologyAsList()) {
                if (op.getName().equals("source")) {
                    continue;
                }
                dcm.startContainer(dockerHost, op.getName(), infrastructureHost);
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (DockerException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        List<String> hosts = new ArrayList<>();
        hosts.add(dockerHost);

        try {
            dcm.updateDeployedContainer(hosts);

            for (Map.Entry<String, DockerContainer> container : dcm.getDeployedContainer().entrySet()) {
                dcm.removeContainer(container.getValue());
            }


        } catch (DockerCertificateException e) {
            e.printStackTrace();
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //TODO consider more than 1 dockerHost
    }

    public void scaleup(String operator) {
        housekeeping();
        try {
            dcm.startContainer(dockerHost, operator, infrastructureHost);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void scaleDown(String operator) {
        housekeeping();

        for (Map.Entry<String, DockerContainer> container : dcm.getDeployedContainer().entrySet()) {
            if (container.getValue().getOperator().equals(operator)) {
                //TODO in processing nodes: check if killme file is available


                try {
                    dcm.executeCommand(container.getValue(), "cd ~ ; touch killme");
                } catch (DockerException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                toBeShutDown.put(new DateTime(DateTimeZone.UTC), container.getValue());
                dcm.removeDeployedContainerFromList(container.getKey());

                break;
            }
        }

    }



}
