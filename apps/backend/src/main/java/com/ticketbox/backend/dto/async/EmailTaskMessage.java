package com.ticketbox.backend.dto.async;

import java.io.Serializable;
import java.util.List;

/**
 * Message payload for sending e-tickets via email.
 */
public class EmailTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private Long orderId;
    private Long userId;
    private String recipientEmail;
    private List<Long> ticketIds;
    private String correlationId;

    public EmailTaskMessage() {
    }

    public EmailTaskMessage(String jobId, Long orderId, Long userId, String recipientEmail, List<Long> ticketIds, String correlationId) {
        this.jobId = jobId;
        this.orderId = orderId;
        this.userId = userId;
        this.recipientEmail = recipientEmail;
        this.ticketIds = ticketIds;
        this.correlationId = correlationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public List<Long> getTicketIds() {
        return ticketIds;
    }

    public void setTicketIds(List<Long> ticketIds) {
        this.ticketIds = ticketIds;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "EmailTaskMessage{" +
                "jobId='" + jobId + '\'' +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", ticketIds=" + ticketIds +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
