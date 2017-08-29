package ac.at.tuwien.infosys.visp.runtime.monitoring;

import java.util.List;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.QueueMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.QueueMonitor;
import ac.at.tuwien.infosys.visp.runtime.monitoring.entities.ScalingAction;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@DependsOn("configurationprovider")
public class Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${visp.relaxationfactor}")
    private Double relaxationfactor;

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private QueueMonitorRepository qmr;

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private Configurationprovider config;

    @Scheduled(fixedRateString = "#{@configurationprovider.monitoringperiod}")
    public void recordData() {
        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP())) {
            for (String queue : topologyMgmt.getIncomingQueuesAsList(op.getName())) {
                QueueMonitor qm = getQueueCount(queue, op);

                if (qm == null) {
                    continue;
                }

                qmr.save(qm);
            }
        }
    }


    public ScalingAction analyze(Operator operator) {
        List<String> queues = topologyMgmt.getIncomingQueuesAsList(operator.getName());

        Integer max = getMaximumQueueCount(operator, queues);

        ScalingAction sa = upscalingDuration(operator, max);

        if (sa == ScalingAction.DONOTHING) {
            //assume that we may not have any monitoring information
            if (max > (config.getUpscalingthreshold() * 5)) {
                return ScalingAction.SCALEUP;
            }
        }
        return sa;
    }

    public ScalingAction analyzeBasic(Operator operator, Integer upscaling, Integer downscaling) {
        List<String> queues = topologyMgmt.getIncomingQueuesAsList(operator.getName());

        Integer max = getMaximumQueueCount(operator, queues);

        if (max > (upscaling * 5)) {
            return ScalingAction.SCALEUPDOUBLE;
        }

        if (max > upscaling) {
            return ScalingAction.SCALEUP;
        }

        if (max < downscaling) {
            return ScalingAction.SCALEDOWN;
        }

        return ScalingAction.DONOTHING;
    }

    private ScalingAction upscalingDuration(Operator operator, Integer maxQueue) {
        List<ProcessingDuration> pds = pcr.findFirst5ByOperatornameOrderByIdDesc(operator.getName());

        if (pds.isEmpty()) {
            return ScalingAction.DONOTHING;
        }

        //calculate average of last three observations
        Integer counter = 0;
        Double durationAverage = 0.0;
        for (int i = 0; i < pds.size(); i++) {
            durationAverage += pds.get(i).getDuration();
            counter++;
        }

        durationAverage = durationAverage / counter;

        if (durationAverage * relaxationfactor > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator.getName())).getExpectedDuration()) {

            if ((maxQueue) > config.getUpscalingthreshold() * 5) {
                LOG.info("Scaleup Double: " + operator.getName() + " - durationAverage: " + durationAverage + " - maxqueue: " + maxQueue);
                return ScalingAction.SCALEUPDOUBLE;
            }

            if (maxQueue > config.getUpscalingthreshold()) {
                LOG.info("Scaleup: " + operator.getName() + " - durationAverage: " + durationAverage + " - maxqueue: " + maxQueue);
                return ScalingAction.SCALEUP;
            }
        }

        Integer count = 4;
        double[][] data = new double[5][2];
        for (ProcessingDuration pd : pds) {
            data[count][0] = count;
            data[count][1] = pd.getDuration();
            count--;
        }

        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);

        Double expectedDurationValue = regression.predict(6);

        if (expectedDurationValue * relaxationfactor > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator.getName())).getExpectedDuration()) {
            if (maxQueue > config.getUpscalingthreshold()) {
                LOG.info("Scaleup: " + operator.getName() + " - durationAverage: " + durationAverage + " - maxqueue: " + maxQueue);
                return ScalingAction.SCALEUP;
            }
        }

        return ScalingAction.DONOTHING;
    }


    private ScalingAction upscalingQueue(String operator, Integer max) {
        if (max > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getQueueThreshold()) {
            return ScalingAction.SCALEUP;
        }

        List<QueueMonitor> qms = qmr.findFirst5ByOperatorOrderByIdDesc(operator);

        Integer count = 4;
        double[][] data = new double[5][2];
        for (QueueMonitor qm : qms) {
            data[count][0] = count;
            data[count][1] = qm.getAmount();
            count--;
        }

        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);

        Double expectedDurationValue = regression.predict(6);

        if (expectedDurationValue > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getQueueThreshold()) {
            return ScalingAction.SCALEUP;
        }

        return ScalingAction.DONOTHING;
    }


    private QueueMonitor getQueueCount(String queueName, Operator op) {
        String operatorName = op.getName();
        String infrastructureHost = config.getRabbitMQHost();

        //only consider the operators which are managed for this specific VISP Runtime
        if (!op.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
            return null;
        }

        RestTemplate restTemplate = new RestTemplateBuilder()
                .basicAuthorization(rabbitmqUsername, rabbitmqPassword)
                .build();

        String queueNameRaw = queueName;
        queueName = queueName.replace("/", "%2F");
        queueName = queueName.replace(">", "%3E");

        String URI = "http://" + infrastructureHost + ":15672/api/queues/%2F/" + queueName;

        ResponseEntity<BaseJsonNode> response;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(URI);
            UriComponents components = builder.build(true);

            response = restTemplate.exchange(components.toUri(), HttpMethod.GET, null, BaseJsonNode.class);
        } catch (HttpClientErrorException e) {
            //Handle those metrics, which are hosted on another VISP Runtime
            URI = "http://" + StringUtils.substringBefore(queueName, "%2F") + ":15672/api/queues/%2F/" + queueName;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(URI);
            UriComponents components = builder.build(true);

            try {
                response = restTemplate.exchange(components.toUri(), HttpMethod.GET, null, BaseJsonNode.class);
            } catch (Exception e1) {
                LOG.error("Could not fetch Monitoring data for URL: " + URI);
                return null;
            }
        }

        try {
            BaseJsonNode arrayNode = response.getBody();

            Double incoming = 0.0;
            Double delivered = 0.0;
            Integer queueload = 0;
            if (arrayNode != null) {
                if (arrayNode.has("message_stats")) {
                    JsonNode rateNode = arrayNode.findValue("message_stats").findValue("publish_details");

                    if (rateNode != null) {
                        if (rateNode.findValue("rate") != null) {
                            incoming = rateNode.findValue("rate").asDouble();
                        }
                    }

                    JsonNode deliveryNode = arrayNode.findValue("message_stats").findValue("deliver_get_details");

                    if (deliveryNode != null) {
                        if (deliveryNode.findValue("rate") != null) {
                            delivered = deliveryNode.findValue("rate").asDouble();
                        }
                    }

                    queueload = arrayNode.findValue("messages").asInt();
                }
                return new QueueMonitor(new DateTime(DateTimeZone.UTC), operatorName, queueNameRaw, queueload, incoming, delivered);
            }
        } catch (Exception ex) {
            LOG.error("No data available for queue: " + queueName);
        }
        return null;

    }

    private Integer getMaximumQueueCount(Operator operator, List<String> queues) {
        Integer max = 0;
        Integer min = 0;

        for (String queue : queues) {
            QueueMonitor qm = getQueueCount(queue, operator);

            if (qm == null) {
                continue;
            }

            Integer queueCount = qm.getAmount();
            if (queueCount < min) {
                min = queueCount;
            }

            if (queueCount > max) {
                max = queueCount;
            }
        }
        return max;
    }
}
