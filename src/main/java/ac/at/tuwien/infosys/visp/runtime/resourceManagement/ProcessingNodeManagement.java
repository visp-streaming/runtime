package ac.at.tuwien.infosys.visp.runtime.resourceManagement;

import java.util.List;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import com.spotify.docker.client.exceptions.DockerException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


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

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private ReasonerUtility reasonerUtility;


    private static final Logger LOG = LoggerFactory.getLogger(ProcessingNodeManagement.class);

    public void removeContainerWhichAreFlaggedToShutdown() {
        for (DockerContainer dc : dcr.findByStatus("stopping")) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            LOG.debug("removeContainerWhichAreFlaggedToShutdown shuptdown container (" + dc.getOperatorType() + ") : current time: " + now + " - " + "termination time:" + new DateTime(dc.getTerminationTime()).plusMinutes(graceperiod));
            if (now.isAfter(new DateTime(dc.getTerminationTime()).plusSeconds(graceperiod))) {
                dcm.removeContainer(dc);
            }
        }
    }

    public Boolean scaleup(DockerHost dh, Operator op) {
        try {
            int count = 0;
            int maxTries = 3;
            while(true) {
                try {
                    dcm.startContainer(dh, op);
                    break;
                } catch (InterruptedException | DockerException e) {
                    LOG.warn("Could not start a docker container - trying again.", e);
                    //increase port number to avoid any potential conflicts
                    dcm.increasePortNumber(dh);
                    if (++count == maxTries) throw e;
                }
            }
            sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), op.getType(), "scaleup", dh.getName()));
        } catch (InterruptedException | DockerException e) {
            LOG.error("Could not start a docker container.", e);
            return false;
        }
        LOG.debug("VISP - Scale UP " + op.getType() + " on host " + dh.getName());
        return true;
    }


    public void removeAll(Operator operator) {
        List<DockerContainer> dcs = dcr.findByOperatorName(operator.getName());
        removeAllHelper(operator, dcs);
    }

    public void removeAll(Operator operator, String resourcePool) {
        List<DockerContainer> dcs = dcr.findAllRunningByOperatorNameAndResourcepool(operator.getName(), resourcePool);
        removeAllHelper(operator, dcs);
    }

    private void removeAllHelper(Operator operator, List<DockerContainer> dcs) {
        for (DockerContainer dc : dcs) {
            if (dhr.findFirstByName(dc.getHost()).getResourcepool().equals(operator.getConcreteLocation().getResourcePool())) {
                if (dc.getStatus() == null) {
                    dc.setStatus("running");
                }
                if (dc.getStatus().equals("stopping")) {
                    continue;
                }
                triggerShutdown(dc);
            }
        }
    }

    public void scaleDown(String operator) {
        List<DockerContainer> operators = dcr.findByOperatorNameAndStatus(operator, "running");

        if (operators.size() < 2) {
            LOG.warn("Could not scale down because only one operator instance is left.");
            return;
        }
        triggerShutdown(operators.get(0));
    }


    public void triggerShutdown(DockerContainer dc) {
        dcm.markContainerForRemoval(dc);
        try {
            dcm.executeCommand(dc, "cd ~ ; touch killme", false);
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not trigger shutdown on container: " + dc.getContainerid(), e);
        }

        dc.setTerminationTime((new DateTime(DateTimeZone.UTC).plusSeconds(graceperiod)));
        sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), dc.getOperatorType(), "scaledown", dc.getHost()));
        LOG.debug("VISP - Scale DOWN " + dc.getOperatorType() + "-" + dc.getContainerid());

    }
}