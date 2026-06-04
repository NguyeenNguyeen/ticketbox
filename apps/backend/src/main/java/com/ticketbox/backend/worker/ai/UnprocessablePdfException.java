package com.ticketbox.backend.worker.ai;

public class UnprocessablePdfException extends RuntimeException {
    public UnprocessablePdfException(String message) {
        super(message);
    }
    public UnprocessablePdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
