package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class CancelledState implements OrderState {

    @Override
    public void processPayment(Order order) {
        throw new IllegalStateException("Cannot process payment for a cancelled order");
    }

    @Override
    public void cancelOrder(Order order) {
        throw new IllegalStateException("Order is already cancelled");
    }

    @Override
    public void completeOrder(Order order) {
        throw new IllegalStateException("Cannot complete a cancelled order");
    }
}
