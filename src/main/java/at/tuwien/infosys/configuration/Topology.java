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

    public List<Operator> getTopologyAsList() {
        return topology;
    }
}
