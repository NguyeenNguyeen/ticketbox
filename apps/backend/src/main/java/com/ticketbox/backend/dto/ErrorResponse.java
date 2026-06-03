package com.ticketbox.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response DTO used across all error handlers and filters.
 * <p>
 * Fields are non-null unless annotated otherwise. The {@code retryAfterSeconds}
 * field is only included in the JSON response when it has a value (e.g. HTTP 429).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final int status;
    private final String code;
    private final String message;
    private final Long retryAfterSeconds;

    private ErrorResponse(Builder builder) {
        this.status = builder.status;
        this.code = builder.code;
        this.message = builder.message;
        this.retryAfterSeconds = builder.retryAfterSeconds;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String code;
        private String message;
        private Long retryAfterSeconds;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder retryAfterSeconds(Long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}
