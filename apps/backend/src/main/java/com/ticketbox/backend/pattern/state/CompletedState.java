package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class CompletedState implements OrderState {

    @Override
    public void processPayment(Order order) {
        throw new IllegalStateException("Order is already completed");
    }

    @Override
    public void cancelOrder(Order order) {
        throw new IllegalStateException("Cannot cancel a completed order");
    }

    @Override
    public void completeOrder(Order order) {
        throw new IllegalStateException("Order is already completed");
    }
}
