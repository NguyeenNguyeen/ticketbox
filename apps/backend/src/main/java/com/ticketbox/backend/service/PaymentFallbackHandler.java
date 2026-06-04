package com.ticketbox.backend.service;

import com.ticketbox.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PaymentFallbackHandler {

    /**
     * Fallback method when Circuit Breaker is OPEN, Bulkhead is FULL, or Timeout occurs.
     * Generates the business-friendly PAYMENT_MAINTENANCE response.
     * 
     * @param orderId the ID of the order being processed
     * @return ResponseEntity with HTTP 200 and business code PAYMENT_MAINTENANCE
     */
    public ResponseEntity<Object> handlePaymentMaintenance(Long orderId) {
        // Return 200 OK per Graceful Degradation design, but with a specific business code
        return ResponseEntity.ok(ErrorResponse.builder()
                .status(HttpStatus.OK.value())
                .code("PAYMENT_MAINTENANCE")
                .message("Hệ thống thanh toán tạm thời gián đoạn. Đơn hàng của bạn đã được giữ. Vui lòng thử lại sau.")
                .retryAfterSeconds(30L) // Suggest retry after 30s
                .build());
    }
}
