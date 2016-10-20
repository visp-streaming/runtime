package at.tuwien.infosys.reasoner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@DependsOn("utilities")
public class Reasoner {

    private static final Logger LOG = LoggerFactory.getLogger(Reasoner.class);

    @Autowired
    private ReasonerPeerJ reasoner;

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        reasoner.updateResourceconfiguration();
    }
}
