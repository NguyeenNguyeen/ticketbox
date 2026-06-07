package com.ticketbox.backend.pattern.state;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderStateContext {

    @Autowired
    private PendingState pendingState;

    @Autowired
    private PayingState payingState;

    @Autowired
    private CompletedState completedState;

    @Autowired
    private CancelledState cancelledState;

    private OrderState getState(Order order) {
        if (order.getStatus() == null) return pendingState;
        
        return switch (order.getStatus()) {
            case PENDING -> pendingState;
            case PAYING -> payingState;
            case COMPLETED -> completedState;
            case CANCELLED -> cancelledState;
        };
    }

    public void processPayment(Order order) {
        getState(order).processPayment(order);
    }

    public void cancelOrder(Order order) {
        getState(order).cancelOrder(order);
    }

    public void completeOrder(Order order) {
        getState(order).completeOrder(order);
    }
}
