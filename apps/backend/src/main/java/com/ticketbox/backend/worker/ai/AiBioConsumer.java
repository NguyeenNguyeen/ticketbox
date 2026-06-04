package com.ticketbox.backend.worker.ai;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.AiBioMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiBioConsumer {

    private final AiBioService aiBioService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_AI, concurrency = "1-2")
    public void receiveMessage(AiBioMessage message) {
        log.info("Received AI Bio request for Job ID: {}", message.getJobId());
        aiBioService.processBiographyGeneration(message);
    }
}
