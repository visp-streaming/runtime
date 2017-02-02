package at.tuwien.infosys.monitoring;


import at.tuwien.infosys.datasources.ProcessingDurationRepository;
import at.tuwien.infosys.datasources.entities.ProcessingDuration;
import entities.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("utilities")
public class ProcessingDurationMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingDurationMonitor.class);

    @Autowired
    private ProcessingDurationRepository pcr;


    @RabbitListener(queues = "processingduration")
    public void assign(Message message) throws InterruptedException {
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), message.getHeader(), Double.parseDouble(message.getPayload())));
    }

}






