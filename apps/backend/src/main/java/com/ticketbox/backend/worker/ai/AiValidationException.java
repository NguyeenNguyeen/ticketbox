package com.ticketbox.backend.worker.ai;

public class AiValidationException extends RuntimeException {
    public AiValidationException(String message) {
        super(message);
    }
    public AiValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
