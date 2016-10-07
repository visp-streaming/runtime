package at.tuwien.infosys.monitoring;


import entities.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("utilities")
public class ErrorMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMonitor.class);

    @RabbitListener(queues =  "error" )
    public void assign(Message message) throws InterruptedException {
        LOG.warn("Processing Node Exception: " + message.getPayload());
    }

}






