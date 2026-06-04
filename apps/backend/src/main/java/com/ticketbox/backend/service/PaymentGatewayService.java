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
}
