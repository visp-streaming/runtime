package at.tuwien.infosys.monitoring;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityWatchdog {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityWatchdog.class);

    public void checkAvailablitiyOfContainer() {
        //TODO implement me: iterate through all docker container and check if they are available

    }

}






