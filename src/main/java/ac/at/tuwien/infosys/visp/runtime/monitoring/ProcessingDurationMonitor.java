package ac.at.tuwien.infosys.visp.runtime.monitoring;


import ac.at.tuwien.infosys.visp.common.Message;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("utilities")
public class ProcessingDurationMonitor {

    @Autowired
    private ProcessingDurationRepository pcr;

    @RabbitListener(queues = "processingduration")
    public void assign(Message message) {
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), message.getHeader(), Double.parseDouble(message.getPayload())));
    }

}






