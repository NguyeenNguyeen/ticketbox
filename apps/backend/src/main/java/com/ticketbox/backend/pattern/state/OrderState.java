package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;

public interface OrderState {
    void processPayment(Order order);
    void cancelOrder(Order order);
    void completeOrder(Order order);
}
