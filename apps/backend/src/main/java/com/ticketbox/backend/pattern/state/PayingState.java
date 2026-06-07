package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class PayingState implements OrderState {

    @Override
    public void processPayment(Order order) {
        throw new IllegalStateException("Payment is already in progress");
    }

    @Override
    public void cancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public void completeOrder(Order order) {
        order.setStatus(OrderStatus.COMPLETED);
    }
}
