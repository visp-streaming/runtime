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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class ProcessingNodeManagement {

    @Autowired
    DockerContainerManagement dcm;

    @Autowired
    private Topology topology;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingNodeManagement.class);


    public void housekeeping() {
        for (DockerContainer dc : dcr.findByStatus("stopping")) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            LOG.info("housekeeping shuptdown container (" + dc.getOperator() + ") : current time: " + now + " - " + "termination time:" + new DateTime(dc.getTerminationTime()).plusMinutes(graceperiod));
            if (now.isAfter(new DateTime(dc.getTerminationTime()).plusSeconds(graceperiod))) {
                try {
                    dcm.removeContainer(dc);
                } catch (DockerException e) {
                    LOG.error("Cloud not remove docker Container while houskeeping.", e);
                } catch (InterruptedException e) {
                    LOG.error("Cloud not remove docker Container while houskeeping.", e);
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
            LOG.error("Could not initialize topology.", e);
        } catch (DockerException e) {
            LOG.error("Could not initialize topology.", e);
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
            LOG.error("Could not remove Docker Container.", e);
        } catch (DockerException e) {
            LOG.error("Could not remove Docker Container.", e);
        } catch (InterruptedException e) {
            LOG.error("Could not remove Docker Container.", e);
        }
    }

    public void scaleup(String operator, String dockerHost, String infrastructureHost) {
        housekeeping();
        try {
            dcm.startContainer(dockerHost, operator, infrastructureHost);
        } catch (DockerException e) {
            LOG.error("Could not start a docker container.", e);
        } catch (InterruptedException e) {
            LOG.error("Could not start a docker container.", e);
        }
    }

    public void scaleDown(String operator) {
        housekeeping();

        List<DockerContainer> operators = dcr.findByOperator(operator);

        if (operators.size()<2) {
            return;
        }

        for (DockerContainer dc : operators) {
            if (dc.getStatus() == null) {
                dc.setStatus("running");
            }

            if (dc.getStatus().equals("stopping")) {
                continue;
            }

            triggerShutdown(dc);
                break;
        }
    }

    public void triggerShutdown(DockerContainer dc) {
        try {
            dcm.executeCommand(dc, "cd ~ ; touch killme");
        } catch (DockerException e) {
            LOG.error("Could not trigger scaledown operation.", e);
        } catch (InterruptedException e) {
            LOG.error("Could not trigger scaledown operation.", e);
        }

        dcm.markContainerForRemoval(dc);
    }
}