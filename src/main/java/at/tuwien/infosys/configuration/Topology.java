package at.tuwien.infosys.configuration;

import at.tuwien.infosys.entities.Operator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Topology {

    private List<Operator> topology = new ArrayList<>();
    private String imageID = "chochreiner/vispprocessingnodes";

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    public Topology() {

        //taxiDataTopology();
        sequentialWaitTopology();
    }

    private void taxiDataTopology() {
        Operator source = new Operator("source");

        //Speed
        Operator speed = new Operator("speed");
        List<Operator> speedList = new ArrayList<>();
        speedList.add(source);
        speed.setSources(speedList);
        speed.setMessageBrokerHost(infrastructureHost);

        //AVG - Speed
        Operator avgSpeed = new Operator("avgSpeed");
        List<Operator> avgSpeedList = new ArrayList<>();
        avgSpeedList.add(speed);
        avgSpeed.setSources(avgSpeedList);
        avgSpeed.setMessageBrokerHost(infrastructureHost);

        //Aggregation
        Operator aggregation = new Operator("aggregation");
        List<Operator> aggregationList = new ArrayList<>();
        aggregationList.add(source);
        aggregation.setSources(aggregationList);
        aggregation.setMessageBrokerHost(infrastructureHost);

        //Distance
        Operator distance = new Operator("distance");
        List<Operator> distanceList = new ArrayList<>();
        distanceList.add(aggregation);
        distance.setSources(distanceList);
        distance.setMessageBrokerHost(infrastructureHost);

        //Analysis
        Operator analysis = new Operator("analysis");
        List<Operator> analysisList = new ArrayList<>();
        analysisList.add(avgSpeed);
        analysisList.add(distance);
        analysis.setSources(analysisList);
        analysis.setMessageBrokerHost(infrastructureHost);

        //Monitor
        Operator monitor = new Operator("monitor");
        List<Operator> monitorList = new ArrayList<>();
        monitorList.add(source);
        monitorList.add(analysis);
        monitor.setSources(monitorList);
        monitor.setMessageBrokerHost(infrastructureHost);


        topology.add(source);
        topology.add(speed);
        topology.add(avgSpeed);
        topology.add(aggregation);
        topology.add(distance);
        topology.add(analysis);
        topology.add(monitor);
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
