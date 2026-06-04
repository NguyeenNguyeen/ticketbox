package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.EmailTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void receiveEmailJob(EmailTaskMessage message) {
        log.info("Received Email request for Job ID: {}", message.getJobId());
        
        try {
            emailService.processEmailJob(message);
        } catch (Exception e) {
            log.warn("Job {}: Exception caught in consumer, propagating for retry: {}", message.getJobId(), e.getMessage());
            throw e; // Bubble up for Spring AMQP retry/DLQ interceptor
        }
    }
}
