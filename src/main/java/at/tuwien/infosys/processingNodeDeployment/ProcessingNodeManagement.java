package at.tuwien.infosys.processingNodeDeployment;

import at.tuwien.infosys.configuration.Topology;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.Operator;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class ProcessingNodeManagement {

    @Autowired
    DockerContainerManagement dcm;

    @Autowired
    private Topology topology;

    private Integer graceperiod = 2;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;


    private void housekeeping() {
        for (DockerContainer dc : dcr.findByStatus("stopping")) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            if (now.isAfter(new DateTime(dc.getTerminationTime()).plusMinutes(graceperiod))) {
                try {
                    dcm.removeContainer(dc);
                } catch (DockerException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void initializeTopology(String dockerHost, String infrastructureHost) {
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
        List<String> hostString = new ArrayList<>();

        for (DockerHost dh : dhr.findAll()) {
            hostString.add(dh.getHostid());
        }


        try {
            dcm.updateDeployedContainer(hostString);

            for (DockerContainer dc : dcr.findAll()) {
                dcm.removeContainer(dc);
            }

        } catch (DockerCertificateException e) {
            e.printStackTrace();
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void scaleup(String operator, String dockerHost, String infrastructureHost) {
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

        for (DockerContainer dc : dcr.findByOperator(operator)) {
                try {
                    dcm.executeCommand(dc, "cd ~ ; touch killme");
                } catch (DockerException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                dcm.removeDeployedContainerFromList(dc);
                break;
        }
    }


}
