package ac.at.tuwien.infosys.visp.runtime.resourceManagement;

import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import com.spotify.docker.client.exceptions.DockerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProcessingNodeManagement {

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private ScalingActivityRepository sar;


    private static final Logger LOG = LoggerFactory.getLogger(ProcessingNodeManagement.class);

    public void removeContainerWhichAreFlaggedToShutdown() {
        for (DockerContainer dc : dcr.findByStatus("stopping")) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            LOG.info("removeContainerWhichAreFlaggedToShutdown shuptdown container (" + dc.getOperator() + ") : current time: " + now + " - " + "termination time:" + new DateTime(dc.getTerminationTime()).plusMinutes(graceperiod));
            if (now.isAfter(new DateTime(dc.getTerminationTime()).plusSeconds(graceperiod))) {
                dcm.removeContainer(dc);
            }
        }
    }

    public Boolean scaleup(DockerContainer dc, DockerHost dh, String infrastructureHost) {

        try {
            int count = 0;
            int maxTries = 5;
            while(true) {
                try {
                    dcm.startContainer(dh, dc, infrastructureHost);
                    break;
                } catch (InterruptedException | DockerException e) {
                    LOG.warn("Could not start a docker container - trying again.", e);
                    if (++count == maxTries) throw e;
                }
            }
            sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), dc.getOperator(), "scaleup", dh.getName()));
        } catch (InterruptedException | DockerException e) {
            LOG.error("Could not start a docker container.", e);
            return false;
        }
        LOG.info("VISP - Scale UP " + dc.getOperator() + " on host " + dh.getName());
        return true;
    }

    public void scaleDown(String operator) {
        List<DockerContainer> operators = dcr.findByOperator(operator);

        if (operators.size() < 2) {
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
            dcm.markContainerForRemoval(dc);
            dcm.executeCommand(dc, "cd ~ ; touch killme");


            dc.setTerminationTime((new DateTime(DateTimeZone.UTC).plusSeconds(graceperiod)));
            sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), dc.getOperator(), "scaledown", dc.getHost()));
            LOG.info("VISP - Scale DOWN " + dc.getOperator() + "-" + dc.getContainerid());

        } catch (InterruptedException | DockerException e) {
            LOG.error("Could not trigger scaledown operation.", e);
        }
    }
}