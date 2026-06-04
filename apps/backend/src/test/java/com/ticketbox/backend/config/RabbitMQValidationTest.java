package com.ticketbox.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1"
})
public class RabbitMQValidationTest {

    public static final AtomicInteger retryCount = new AtomicInteger(0);
    public static final String TEST_QUEUE = "test_queue";
    public static final String TEST_DLQ = "test_queue.dlq";
    public static final String TEST_ROUTING_KEY = "test.routing.key";

    @TestConfiguration
    static class TestListenerConfig {

        @Bean
        public Queue testDlq() {
            return new Queue(TEST_DLQ);
        }

        @Bean
        public Queue testQueue() {
            return QueueBuilder.durable(TEST_QUEUE)
                    .withArgument("x-dead-letter-exchange", RabbitMQConfig.EXCHANGE_DLX)
                    .withArgument("x-dead-letter-routing-key", TEST_ROUTING_KEY)
                    .build();
        }

        @Bean
        public Binding testDlqBinding() {
            return BindingBuilder.bind(testDlq()).to(new DirectExchange(RabbitMQConfig.EXCHANGE_DLX)).with(TEST_ROUTING_KEY);
        }

        @Bean
        public Binding testBinding() {
            return BindingBuilder.bind(testQueue()).to(new DirectExchange(RabbitMQConfig.EXCHANGE_COMMANDS)).with(TEST_ROUTING_KEY);
        }

        @RabbitListener(queues = TEST_QUEUE)
        public void listenTest(String msg) {
            retryCount.incrementAndGet();
            throw new RuntimeException("Forced failure to trigger DLQ");
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testDlqRouting() throws InterruptedException {
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        admin.purgeQueue(TEST_QUEUE, false);
        admin.purgeQueue(TEST_DLQ, false);
        
        retryCount.set(0);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, TEST_ROUTING_KEY, "test-message");
        
        // Wait for 3 retries (1000 + 2000 + 4000 = 7000ms max backoff total, plus processing)
        Thread.sleep(8000);

        org.junit.jupiter.api.Assertions.assertEquals(3, retryCount.get(), "Retry count should be exactly 3");

        Object dlqMsg = rabbitTemplate.receiveAndConvert(TEST_DLQ);
        org.junit.jupiter.api.Assertions.assertNotNull(dlqMsg, "Message should be routed to DLQ");
    }
}
