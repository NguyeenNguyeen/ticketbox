package com.ticketbox.backend.dto.async;

import java.io.Serializable;

/**
 * Message payload for sending e-tickets via email.
 */
public class EmailTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;

    public EmailTaskMessage() {
    }

    public EmailTaskMessage(Long orderId, Long userId) {
        this.orderId = orderId;
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "EmailTaskMessage{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                '}';
    }
}
