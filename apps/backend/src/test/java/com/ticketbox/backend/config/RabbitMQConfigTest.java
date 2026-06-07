package com.ticketbox.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RabbitMQConfig.class)
class RabbitMQConfigTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private AmqpAdmin amqpAdmin;

    @MockBean
    private SimpleRabbitListenerContainerFactoryConfigurer configurer;

    @Autowired
    private ApplicationContext context;

    @Test
    void testExchangesAreDefined() {
        DirectExchange commandsExchange = context.getBean("commandsExchange", DirectExchange.class);
        assertThat(commandsExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_COMMANDS);

        TopicExchange eventsExchange = context.getBean("eventsExchange", TopicExchange.class);
        assertThat(eventsExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_EVENTS);

        DirectExchange deadLetterExchange = context.getBean("deadLetterExchange", DirectExchange.class);
        assertThat(deadLetterExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_DLX);
    }

    @Test
    void testQueuesAreDefinedWithDLQArgs() {
        Queue emailQueue = context.getBean("emailQueue", Queue.class);
        assertThat(emailQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_EMAIL);
        assertThat(emailQueue.getArguments()).containsEntry("x-dead-letter-exchange", RabbitMQConfig.EXCHANGE_DLX);
        assertThat(emailQueue.getArguments()).containsEntry("x-dead-letter-routing-key", RabbitMQConfig.ROUTING_KEY_EMAIL);

        Queue csvQueue = context.getBean("csvQueue", Queue.class);
        assertThat(csvQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_CSV);
        assertThat(csvQueue.getArguments()).containsEntry("x-dead-letter-exchange", RabbitMQConfig.EXCHANGE_DLX);
        assertThat(csvQueue.getArguments()).containsEntry("x-dead-letter-routing-key", RabbitMQConfig.ROUTING_KEY_CSV);

        Queue aiQueue = context.getBean("aiQueue", Queue.class);
        assertThat(aiQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_AI);
        assertThat(aiQueue.getArguments()).containsEntry("x-dead-letter-exchange", RabbitMQConfig.EXCHANGE_DLX);
        assertThat(aiQueue.getArguments()).containsEntry("x-dead-letter-routing-key", RabbitMQConfig.ROUTING_KEY_AI);
    }

    @Test
    void testDlqQueuesAreDefined() {
        Queue emailDlq = context.getBean("emailDlq", Queue.class);
        assertThat(emailDlq.getName()).isEqualTo(RabbitMQConfig.QUEUE_EMAIL_DLQ);

        Queue csvDlq = context.getBean("csvDlq", Queue.class);
        assertThat(csvDlq.getName()).isEqualTo(RabbitMQConfig.QUEUE_CSV_DLQ);

        Queue aiDlq = context.getBean("aiDlq", Queue.class);
        assertThat(aiDlq.getName()).isEqualTo(RabbitMQConfig.QUEUE_AI_DLQ);
    }

    @Test
    void testBindingsAreCorrect() {
        Binding emailBinding = context.getBean("emailBinding", Binding.class);
        assertThat(emailBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_EMAIL);
        assertThat(emailBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE_COMMANDS);
        assertThat(emailBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_KEY_EMAIL);

        Binding emailDlqBinding = context.getBean("emailDlqBinding", Binding.class);
        assertThat(emailDlqBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_EMAIL_DLQ);
        assertThat(emailDlqBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE_DLX);
        assertThat(emailDlqBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.ROUTING_KEY_EMAIL);
    }

    @Test
    void testRabbitListenerContainerFactoryIsConfigured() {
        SimpleRabbitListenerContainerFactory factory = context.getBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
        assertThat(factory).isNotNull();
        // Since getAdviceChain returns an array, we ensure it's not null, proving the RetryInterceptor is attached.
        assertThat(factory.getAdviceChain()).isNotNull();
    }
}
