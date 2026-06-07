package com.ticketbox.backend.exception;

/**
 * Thrown when the payment provider cannot be reached or returns an unexpected error.
 * This is an infrastructure exception and SHOULD trigger the Circuit Breaker.
 */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
