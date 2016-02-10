package at.tuwien.infosys.monitoring;


import entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ErrorMonitor {


    @Value("${role}")
    private String role;

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMonitor.class);


    @RabbitListener(queues = { "error" } )
    public void assign(Message message) throws InterruptedException {
        LOG.warn("Processing Node Exception: " + message.getPayload());
    }


    }






