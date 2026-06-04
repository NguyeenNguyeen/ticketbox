package com.ticketbox.backend.exception;

/**
 * Thrown when a payment is declined by the provider (e.g., insufficient funds, expired card).
 * This is a business exception and should NOT trigger the Circuit Breaker.
 */
public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
