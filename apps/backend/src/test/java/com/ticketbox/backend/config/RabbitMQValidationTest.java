package com.ticketbox.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.TestConfiguration;

import com.ticketbox.backend.dto.async.EmailTaskMessage;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1"
})
public class RabbitMQValidationTest {

    public static final AtomicInteger retryCount = new AtomicInteger(0);

    @TestConfiguration
    static class TestListenerConfig {
        @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
        public void listenEmail(EmailTaskMessage msg) {
            retryCount.incrementAndGet();
            throw new RuntimeException("Forced failure to trigger DLQ");
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testDlqRouting() throws InterruptedException {
        // Clear DLQ first if needed, but assuming it's fresh
        rabbitTemplate.receive(RabbitMQConfig.QUEUE_EMAIL_DLQ); // Drain one if exists
        retryCount.set(0);

        EmailTaskMessage emailMsg = new EmailTaskMessage(123L, 456L);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_EMAIL, emailMsg);
        
        // Wait for 3 retries (1000 + 2000 + 4000 = 7000ms max backoff total, plus processing)
        Thread.sleep(8000);

        org.junit.jupiter.api.Assertions.assertEquals(3, retryCount.get(), "Retry count should be exactly 3");

        Object dlqMsg = rabbitTemplate.receiveAndConvert(RabbitMQConfig.QUEUE_EMAIL_DLQ);
        org.junit.jupiter.api.Assertions.assertNotNull(dlqMsg, "Message should be routed to DLQ");
    }
}
