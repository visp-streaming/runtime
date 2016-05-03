package at.tuwien.infosys.configuration;

import at.tuwien.infosys.entities.Operator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TopologyWait {

    private List<Operator> topology = new ArrayList<>();
    private String imageID = "chochreiner/vispprocessingnodes";

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    public TopologyWait() {

        sequentialWaitTopology();
    }

    private void sequentialWaitTopology() {
        Operator source = new Operator("source");

        Operator step1 = new Operator("step1");
        List<Operator> step1List = new ArrayList<>();
        step1List.add(source);
        step1.setSources(step1List);
        step1.setMessageBrokerHost(infrastructureHost);

        Operator step2 = new Operator("step2");
        List<Operator> step2List = new ArrayList<>();
        step2List.add(step1);
        step2.setSources(step2List);
        step2.setMessageBrokerHost(infrastructureHost);

        Operator step3 = new Operator("step3");
        List<Operator> step3List = new ArrayList<>();
        step3List.add(step2);
        step3.setSources(step3List);
        step3.setMessageBrokerHost(infrastructureHost);

        Operator step4 = new Operator("step4");
        List<Operator> step4List = new ArrayList<>();
        step4List.add(step3);
        step4.setSources(step4List);
        step4.setMessageBrokerHost(infrastructureHost);

        Operator step5 = new Operator("step5");
        List<Operator> step5List = new ArrayList<>();
        step5List.add(step4);
        step5.setSources(step5List);
        step5.setMessageBrokerHost(infrastructureHost);

        Operator log = new Operator("log");
        List<Operator> logList = new ArrayList<>();
        logList.add(step5);
        log.setSources(logList);
        log.setMessageBrokerHost(infrastructureHost);


        topology.add(source);
        topology.add(step1);
        topology.add(step2);
        topology.add(step3);
        topology.add(step4);
        topology.add(step5);
        topology.add(log);
    }

    public List<Operator> getTopologyAsList() {
        return topology;
    }
}
