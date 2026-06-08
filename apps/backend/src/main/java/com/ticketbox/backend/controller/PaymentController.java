package com.ticketbox.backend.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment callback/verification endpoint.
 *
 * In a real system, payment providers (VNPAY, MoMo) would POST to a secure
 * server-to-server webhook. The order would already have been finalized during
 * the purchase flow (Phase C). This endpoint is provided for the demo frontend
 * to confirm the gateway's response code and get a structured reply.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    /**
     * Called by the frontend after the user returns from the payment gateway.
     * Logs the transaction details. The actual order status was already set
     * during the purchase flow (finalizeOrderSuccess / finalizeOrderFailure).
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody VerifyRequest req) {
        log.info("Payment verification received: provider={}, txnRef={}, code={}",
                req.getProvider(), req.getTransactionRef(), req.getResponseCode());

        boolean success = "00".equals(req.getResponseCode()) || "0".equals(req.getResponseCode());

        return ResponseEntity.ok(Map.of(
                "verified", success,
                "transactionRef", req.getTransactionRef() != null ? req.getTransactionRef() : "",
                "message", success ? "Payment verified successfully" : "Payment was not successful"
        ));
    }

    @Data
    static class VerifyRequest {
        private String provider;       // "VNPAY" or "MOMO"
        private String transactionRef; // vnp_TxnRef or MoMo transactionId
        private String responseCode;   // "00" for VNPAY success, "0" for MoMo success
    }
}
