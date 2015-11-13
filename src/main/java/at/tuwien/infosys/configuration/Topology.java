package at.tuwien.infosys.configuration;

import at.tuwien.infosys.entities.Operator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Topology {

    private List<Operator> topology = new ArrayList<>();
    private String imageID = "chochreiner/vispprocessingnodes";


    public Topology() {

        Operator source = new Operator("source");

        //Speed
        Operator speed = new Operator("speed");
        List<Operator> speedList = new ArrayList<>();
        speedList.add(source);
        speed.setSources(speedList);

        //AVG - Speed
        Operator avgSpeed = new Operator("avgSpeed");
        List<Operator> avgSpeedList = new ArrayList<>();
        avgSpeedList.add(speed);
        avgSpeed.setSources(avgSpeedList);


        //Aggregation
        Operator aggregation = new Operator("aggregation");
        List<Operator> aggregationList = new ArrayList<>();
        aggregationList.add(source);
        aggregation.setSources(aggregationList);

        //Distance
        Operator distance = new Operator("distance");
        List<Operator> distanceList = new ArrayList<>();
        distanceList.add(aggregation);
        distance.setSources(distanceList);

        //Analysis
        Operator analysis = new Operator("analysis");
        List<Operator> analysisList = new ArrayList<>();
        analysisList.add(avgSpeed);
        analysisList.add(distance);
        analysis.setSources(analysisList);

        //Monitor
        Operator monitor = new Operator("monitor");
        List<Operator> monitorList = new ArrayList<>();
        monitorList.add(source);
        monitorList.add(analysis);
        monitor.setSources(monitorList);


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
