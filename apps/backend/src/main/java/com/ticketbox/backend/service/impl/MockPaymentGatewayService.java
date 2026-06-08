package com.ticketbox.backend.service.impl;

import com.ticketbox.backend.dto.PaymentResult;
import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.exception.PaymentDeclinedException;
import com.ticketbox.backend.exception.PaymentGatewayException;
import com.ticketbox.backend.service.PaymentGatewayService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MockPaymentGatewayService implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayService.class);
    private static final String PAYMENT_GATEWAY = "paymentGateway";

    @Override
    @CircuitBreaker(name = PAYMENT_GATEWAY)
    @Bulkhead(name = PAYMENT_GATEWAY)
    public PaymentResult processPayment(Order order) {
        log.info("Processing payment for Order: {} with amount: {}", order.getId(), order.getTotalAmount());
        
        // Simulate network latency (200ms - 1500ms)
        simulateLatency();
        
        // Simulate failures to test circuit breaker
        simulateFailures();

        // If we get here, payment is successful
        String transactionId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Payment successful for Order: {}. Transaction ID: {}", order.getId(), transactionId);
        
        return PaymentResult.builder()
                .status(PaymentResult.PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .message("Payment successful")
                .build();
    }

    @Override
    public PaymentResult checkPaymentStatus(Order order) {
        log.info("Checking actual payment status for Order: {}", order.getId());
        
        // Simulate checking with provider. In this mock, we assume 1% of stuck orders actually succeeded.
        int random = ThreadLocalRandom.current().nextInt(100);
        if (random < 1) {
            return PaymentResult.builder()
                    .status(PaymentResult.PaymentStatus.SUCCESS)
                    .transactionId("TX-RECOVERED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .message("Payment succeeded previously but was not synced")
                    .build();
        }
        
        return PaymentResult.builder()
                .status(PaymentResult.PaymentStatus.ERROR)
                .message("Payment never completed or was abandoned")
                .build();
    }

    private void simulateLatency() {
        try {
            long latency = ThreadLocalRandom.current().nextLong(200, 1500);
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Payment thread interrupted", e);
        }
    }

    private void simulateFailures() {
        // For local development, disable random failure simulation so payment succeeds.
        // In a real production integration this would call the actual payment provider.
    }
}

