package com.ticketbox.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String EXCHANGE_COMMANDS = "ticketbox.commands";
    public static final String EXCHANGE_EVENTS = "ticketbox.events";
    public static final String EXCHANGE_DLX = "ticketbox.dlx";

    // Primary Queues
    public static final String QUEUE_EMAIL = "email_queue";
    public static final String QUEUE_CSV = "csv_queue";
    public static final String QUEUE_AI = "ai_queue";

    // Dead Letter Queues
    public static final String QUEUE_EMAIL_DLQ = "email_queue.dlq";
    public static final String QUEUE_CSV_DLQ = "csv_queue.dlq";
    public static final String QUEUE_AI_DLQ = "ai_queue.dlq";

    // Routing Keys (using the queue names for simplicity on direct commands)
    public static final String ROUTING_KEY_EMAIL = "email.send";
    public static final String ROUTING_KEY_CSV = "csv.import";
    public static final String ROUTING_KEY_AI = "ai.generate";

    @Bean
    public DirectExchange commandsExchange() {
        return new DirectExchange(EXCHANGE_COMMANDS);
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE_EVENTS);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    // --- Primary Queues ---
    
    private Queue createQueueWithDlx(String queueName, String routingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", routingKey);
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public Queue emailQueue() {
        return createQueueWithDlx(QUEUE_EMAIL, ROUTING_KEY_EMAIL);
    }

    @Bean
    public Queue csvQueue() {
        return createQueueWithDlx(QUEUE_CSV, ROUTING_KEY_CSV);
    }

    @Bean
    public Queue aiQueue() {
        return createQueueWithDlx(QUEUE_AI, ROUTING_KEY_AI);
    }

    // --- DLQ Queues ---

    @Bean
    public Queue emailDlq() {
        return new Queue(QUEUE_EMAIL_DLQ, true);
    }

    @Bean
    public Queue csvDlq() {
        return new Queue(QUEUE_CSV_DLQ, true);
    }

    @Bean
    public Queue aiDlq() {
        return new Queue(QUEUE_AI_DLQ, true);
    }

    // --- Bindings ---

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange commandsExchange) {
        return BindingBuilder.bind(emailQueue).to(commandsExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding csvBinding(Queue csvQueue, DirectExchange commandsExchange) {
        return BindingBuilder.bind(csvQueue).to(commandsExchange).with(ROUTING_KEY_CSV);
    }

    @Bean
    public Binding aiBinding(Queue aiQueue, DirectExchange commandsExchange) {
        return BindingBuilder.bind(aiQueue).to(commandsExchange).with(ROUTING_KEY_AI);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDlq).to(deadLetterExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding csvDlqBinding(Queue csvDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(csvDlq).to(deadLetterExchange).with(ROUTING_KEY_CSV);
    }

    @Bean
    public Binding aiDlqBinding(Queue aiDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(aiDlq).to(deadLetterExchange).with(ROUTING_KEY_AI);
    }

    // --- Serialization and Infrastructure ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            RabbitTemplate rabbitTemplate) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        
        // Mitigate stateless retry thread-blocking by allowing concurrent consumers
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, EXCHANGE_DLX))
                .build());
                
        return factory;
    }
}
