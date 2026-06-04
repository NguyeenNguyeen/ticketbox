package com.ticketbox.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    private PaymentStatus status;
    private String transactionId;
    private String message;

    public enum PaymentStatus {
        SUCCESS,
        DECLINED,
        ERROR
    }
}
