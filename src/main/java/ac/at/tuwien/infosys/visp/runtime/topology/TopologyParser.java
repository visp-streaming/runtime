package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.runtime.entities.operators.*;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Service
public class TopologyParser {

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private Scanner sc;

    private Map<String, Operator> topology = new LinkedHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public Map<String, Operator> getTopology() {
        return topology;
    }


    public void loadTopology(String file) {

        String topologyText = "";

        try {
            topologyText = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(file), "UTF-8");
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        parse(topologyText);

        enrichTopology();

    }

    private void enrichTopology() {

        for (Operator operator : topology.values()) {
            List<Operator> realOperators = new ArrayList<>();

            for (String op : operator.getSourcesText()) {
                realOperators.add(topology.get(op));
            }

            operator.setSources(realOperators);
            operator.setMessageBrokerHost(infrastructureHost); // TODO get rid of this during deployment
            topology.put(operator.getName(), operator);
        }

    }


    public void parse(String input) {
        sc = new Scanner(input);

        while (sc.hasNextLine()) {
            String start = sc.nextLine();
            start = start.trim();

            if (start.contains("Sink")) {
                parseSink(start);
                continue;
            }

            if (start.contains("Source")) {
                parseSource(start);
                continue;
            }

            if (start.contains("Operator")) {
                parseOperator(start);
                continue;
            }

            if (start.contains("ExternalService")) {
                parseExternalService(start);
                continue;
            }
        }
    }

    private Sink parseSink(String start) {
        Sink sink = new Sink();

        sink.setName(start.substring(start.indexOf("$") + 1, start.indexOf(" ")));

        List<String> sources = getSources(start);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.contains("}")) {
                sink.setSourcesText(sources);
                topology.put(sink.getName(), sink);
                return sink;
            }

            String[] data = line.split("=");

            switch (data[0].trim()) {
                case "allowedLocations":
                    sink.setAllowedLocations(data[1].trim().replace("\"", ""));
                    break;
                case "destination":
                    sink.setDestination(data[1].trim().replace("\"", ""));
                    break;
                case "inputFormat":
                    sink.setInputFormat(data[1].trim().replace("\"", ""));
                    break;
                case "type":
                    sink.setType(data[1].trim().replace("\"", ""));
                    break;
                case "outputFormat":
                    sink.setOutputFormat(data[1].trim().replace("\"", ""));
                    break;
            }
        }

        throw new RuntimeException("Could not parse topology due to syntaktic error");
    }

    private Source parseSource(String start) {
        Source source = new Source();

        source.setName(start.substring(start.indexOf("$") + 1, start.indexOf(" ")));

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.contains("}")) {
                topology.put(source.getName(), source);
                return source;
            }

            String[] data = line.split("=");

            switch (data[0].trim()) {
                case "allowedLocations":
                    source.setAllowedLocations(data[1].trim().replace("\"", ""));
                    break;
                case "inputFormat":
                    source.setInputFormat(data[1].trim().replace("\"", ""));
                    break;
                case "mechanism":
                    source.setMechanism(data[1].trim().replace("\"", ""));
                    break;
                case "type":
                    source.setType(data[1].trim().replace("\"", ""));
                    break;
                case "outputFormat":
                    source.setOutputFormat(data[1].trim().replace("\"", ""));
                    break;
            }
        }
        throw new RuntimeException("Could not parse topology due to syntaktic error");
    }


    private Operator parseOperator(String start) {
        ProcessingOperator operator = new ProcessingOperator();

        operator.setName(start.substring(start.indexOf("$") + 1, start.indexOf(" ")));
        List<String> sources = getSources(start);


        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.contains("}")) {
                operator.setSourcesText(sources);
                topology.put(operator.getName(), operator);
                return operator;
            }

            String[] data = line.split("=");

            switch (data[0].trim()) {
                case "allowedLocations":
                    operator.setAllowedLocations(data[1].trim().replace("\"", ""));
                    break;
                case "inputFormat":
                    operator.setInputFormat(data[1].trim().replace("\"", ""));
                    break;
                case "type":
                    operator.setType(data[1].trim().replace("\"", ""));
                    break;
                case "outputFormat":
                    operator.setOutputFormat(data[1].trim().replace("\"", ""));
                    break;
                case "scalingThreshold":
                    operator.setScalingThreshold(data[1].trim().replace("\"", ""));
                    break;
                case "expectedDuration":
                    operator.setExpectedDuration(data[1].trim().replace("\"", ""));
                    break;
                case "queueThreshold":
                    operator.setQueueThreshold(data[1].trim().replace("\"", ""));
                    break;
            }
        }

        if (operator.getExpectedDuration().isEmpty()) {
            operator.setExpectedDuration("500");
        }

        if (operator.getQueueThreshold().isEmpty()) {
            operator.setQueueThreshold("100");
        }

        throw new RuntimeException("Could not parse topology due to syntaktic error");
    }

    private ExternalService parseExternalService(String start) {
        ExternalService service = new ExternalService();

        service.setName(start.substring(start.indexOf("$") + 1, start.indexOf(" ")));
        List<String> sources = getSources(start);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.contains("}")) {
                service.setSourcesText(sources);
                topology.put(service.getName(), service);
                return service;
            }

            String[] data = line.split("=");

            switch (data[0].trim()) {
                case "inputFormat":
                    service.setInputFormat(data[1].trim().replace("\"", ""));
                    break;
                case "type":
                    service.setType(data[1].trim().replace("\"", ""));
                    break;
                case "location":
                    service.setLocation(data[1].trim().replace("\"", ""));
                    break;
                case "outputFormat":
                    service.setOutputFormat(data[1].trim().replace("\"", ""));
                    break;
            }

        }
        throw new RuntimeException("Could not parse topology due to syntaktic error");
    }


    private List<String> getSources(String start) {
        List<String> sources = new ArrayList<>();

        String sourcesText = start.substring(start.indexOf("(") + 1, start.indexOf(")"));
        if (sourcesText.contains(",")) {

            for (String source : sourcesText.split(",")) {
                source = source.trim();
                sources.add(source.substring(1));
            }
        } else {
            sources.add(sourcesText.substring(1));
        }
        return sources;
    }

}
