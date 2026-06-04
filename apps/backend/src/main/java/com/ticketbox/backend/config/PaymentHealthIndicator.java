package com.ticketbox.backend.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("paymentGateway")
public class PaymentHealthIndicator implements HealthIndicator {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Override
    public Health health() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        Bulkhead bh = bulkheadRegistry.bulkhead("paymentGateway");

        Map<String, Object> details = new HashMap<>();
        details.put("circuitBreakerState", cb.getState().toString());
        details.put("failureRate", cb.getMetrics().getFailureRate() + "%");
        details.put("slowCallRate", cb.getMetrics().getSlowCallRate() + "%");
        details.put("availableBulkheadPermits", bh.getMetrics().getAvailableConcurrentCalls());

        if (cb.getState() == CircuitBreaker.State.OPEN) {
            return Health.down().withDetails(details).build();
        } else if (cb.getState() == CircuitBreaker.State.HALF_OPEN) {
            return Health.status("DEGRADED").withDetails(details).build();
        }

        return Health.up().withDetails(details).build();
    }
}
