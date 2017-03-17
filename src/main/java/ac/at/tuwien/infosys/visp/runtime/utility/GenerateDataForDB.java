package ac.at.tuwien.infosys.visp.runtime.utility;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPConnectionRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPInstanceRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPConnection;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@DependsOn("configurationprovider")
public class GenerateDataForDB {

    @Autowired
    private VISPConnectionRepository vcr;

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private VISPInstanceRepository vir;

    @Autowired
    private Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(GenerateDataForDB.class);

    public void generateConnections() {
        Double availabilityIntern = (Math.floor(90 + Math.random() * (99 - 90 + 1))) / 100;
        Double datarateIntern = (Math.floor(1000 + Math.random() * (5000 - 1000 + 1)));

        //intern connections
        vcr.save(new VISPConnection(config.getRuntimeIP(), config.getRuntimeIP(), 0.01, datarateIntern, availabilityIntern));

        //extern connections
        for (VISPInstance vispInstance : vir.findAll()) {
            if(vispInstance.getUri().equals(config.getRuntimeIP())) {
                continue;
            }

            Double availabilityExtern = (Math.floor(80 + Math.random() * (99 - 80 + 1))) / 100;

            Double datarateExtern = (Math.floor(10 + Math.random() * (150 - 10 + 1)));
            if (new Random().nextInt(3) == 0) {
                 datarateExtern = (Math.floor(10 + Math.random() * (100 - 10 + 1))) / 100;
            }

            //-1.0 represents not reachable
            Double delay = -1.0;
            try {
                delay = pingUrl(vispInstance.getUri() + ":8080") / 1000;
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage());
            }
            vcr.save(new VISPConnection(config.getRuntimeIP(), vispInstance.getUri(), delay, datarateExtern, availabilityExtern));
        }
    }

    public void generateInstances() {
        Map<String, VISPInstance> instances = new HashMap<>();

        for (Operator op: topologyMgmt.getOperators()) {
            for (Operator.Location loc : op.getAllowedLocationsList()) {
                if (!instances.containsKey(loc.getIpAddress())) {
                    instances.put(loc.getIpAddress(), new VISPInstance(loc.getIpAddress()));
                }
            }
        }

        for (VISPInstance instance : instances.values()) {
            vir.save(instance);
        }

    }



        public Double pingUrl(final String address) throws IOException {
        final URL url = new URL("http://" + address);
        final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setConnectTimeout(1000 * 10);
        final long startTime = System.currentTimeMillis();
        urlConn.connect();
        final long endTime = System.currentTimeMillis();
        if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return Double.valueOf((endTime - startTime));
        }
        return -1.0;
    }

}
