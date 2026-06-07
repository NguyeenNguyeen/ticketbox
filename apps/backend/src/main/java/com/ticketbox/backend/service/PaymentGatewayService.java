package com.ticketbox.backend.service;

import com.ticketbox.backend.dto.PaymentResult;
import com.ticketbox.backend.entity.Order;

public interface PaymentGatewayService {
    /**
     * Processes payment for an order through the external payment provider.
     * 
     * @param order the order to pay for
     * @return PaymentResult indicating success or decline
     */
    PaymentResult processPayment(Order order);

    /**
     * Checks the payment status of an order directly with the provider.
     * Used by the cleanup job to verify if a payment actually succeeded
     * before cancelling an abandoned order.
     * 
     * @param order the order to check
     * @return PaymentResult indicating the actual status at the provider
     */
    PaymentResult checkPaymentStatus(Order order);
}
