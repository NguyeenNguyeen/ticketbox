package com.ticketbox.backend.exception;

import com.ticketbox.backend.dto.ErrorResponse;
import com.ticketbox.backend.service.PaymentFallbackHandler;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler for all REST controllers.
 * <p>
 * Converts exceptions into a consistent {@link ErrorResponse} JSON payload.
 * <p>
 * <b>Note on Rate Limiting:</b> HTTP 429 responses are written directly by
 * {@code RateLimitFilter} at the Servlet filter level (outside the DispatcherServlet).
 * This handler does NOT catch rate limiting errors — that would require the exception
 * to reach a controller, which it never will. The filter handles its own 429 responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private PaymentFallbackHandler paymentFallbackHandler;

    /**
     * HTTP 403 — Access denied (RBAC check failed).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.warn("Access denied: {} | URI: {} | Method: {} | User: {} | Authorities: {}", 
                 ex.getMessage(), 
                 request.getRequestURI(), 
                 request.getMethod(),
                 auth != null ? auth.getName() : "anonymous",
                 auth != null ? auth.getAuthorities() : "none");
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("ACCESS_DENIED")
                .message("You do not have permission to access this resource.")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * HTTP 401 — Authentication failure.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .code("UNAUTHORIZED")
                .message("Authentication required.")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * HTTP 400 — Validation errors from {@code @Valid} annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("VALIDATION_ERROR")
                .message(firstError)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * HTTP 400 — Business logic errors (IllegalArgumentException, IllegalStateException).
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBusinessException(RuntimeException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("BAD_REQUEST")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * HTTP 400 — Payment Declined (business logic, e.g. insufficient funds)
     */
    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentDeclined(PaymentDeclinedException ex) {
        log.warn("Payment declined: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("PAYMENT_DECLINED")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * HTTP 200 (PAYMENT_MAINTENANCE) — Payment Infrastructure Errors 
     * Handled via graceful degradation (Circuit Breaker OPEN, Bulkhead FULL, Gateway Error)
     */
    @ExceptionHandler({
        PaymentGatewayException.class,
        CallNotPermittedException.class,
        BulkheadFullException.class
    })
    public ResponseEntity<Object> handlePaymentInfrastructureError(RuntimeException ex) {
        log.warn("Payment infrastructure degraded: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        // We do not need the orderId here for the fallback response structure we designed.
        // The fallback handler will return the generic maintenance message.
        return paymentFallbackHandler.handlePaymentMaintenance(null);
    }

    /**
     * HTTP 500 — Unexpected server errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_SERVER_ERROR")
                .message(ex.getClass().getName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "Unknown Error"))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
