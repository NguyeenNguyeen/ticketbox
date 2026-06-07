package com.ticketbox.backend.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentProtectionConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentProtectionConfig.class);

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void setupEventLogging() {
        CircuitBreaker paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");

        paymentCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("CircuitBreaker 'paymentGateway' state changed: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.warn("CircuitBreaker 'paymentGateway' recorded an error: {}",
                        event.getThrowable().getMessage()))
                .onSlowCallRateExceeded(event -> log.warn("CircuitBreaker 'paymentGateway' slow call rate exceeded: {}%",
                        event.getSlowCallRate()))
                .onFailureRateExceeded(event -> log.warn("CircuitBreaker 'paymentGateway' failure rate exceeded: {}%",
                        event.getFailureRate()));
    }
}
