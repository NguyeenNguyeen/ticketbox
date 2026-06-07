package com.ticketbox.backend.exception;

/**
 * Thrown when a rate limit bucket is exhausted for a given key.
 * <p>
 * Carries {@code retryAfterSeconds} so the caller/handler can include
 * a {@code Retry-After} header in the HTTP 429 response without needing
 * to re-calculate the wait time.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
