package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class PendingState implements OrderState {

    @Override
    public void processPayment(Order order) {
        order.setStatus(OrderStatus.PAYING);
        // Transition to PayingState is managed by context/service
    }

    @Override
    public void cancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public void completeOrder(Order order) {
        throw new IllegalStateException("Cannot complete an order that is pending payment");
    }
}
