package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.dto.async.EmailTaskMessage;
import com.ticketbox.backend.entity.EmailStatus;
import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EmailProviderClient emailProviderClient;
    private final EmailJobTracker jobTracker;
    private final TicketAttachmentService attachmentService;
    private final EmailTemplateBuilder templateBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    public EmailService(OrderRepository orderRepository,
                        TicketRepository ticketRepository,
                        EmailProviderClient emailProviderClient,
                        EmailJobTracker jobTracker,
                        TicketAttachmentService attachmentService,
                        EmailTemplateBuilder templateBuilder,
                        RedisTemplate<String, String> redisTemplate,
                        TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.emailProviderClient = emailProviderClient;
        this.jobTracker = jobTracker;
        this.attachmentService = attachmentService;
        this.templateBuilder = templateBuilder;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public void processEmailJob(EmailTaskMessage message) {
        log.info("Processing email job: {}", message.getJobId());
        jobTracker.updateStatus(message.getJobId(), "PROCESSING");

        // 1. Short-term idempotency lock (Redis)
        String lockKey = "email_lock:" + message.getOrderId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, message.getJobId(), 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            String existingJob = redisTemplate.opsForValue().get(lockKey);
            if (!message.getJobId().equals(existingJob)) {
                log.warn("Job {}: Duplicate processing detected for order {}. Skipping.", message.getJobId(), message.getOrderId());
                return;
            }
        }

        try {
            // 2. Fetch data in a short transaction
            OrderData orderData = transactionTemplate.execute(status -> fetchOrderData(message));
            
            if (orderData == null) {
                // Was handled as business failure or already sent
                return;
            }

            // 4. Generate content (outside transaction)
            byte[] pdfAttachment = attachmentService.generateETicketPdf(orderData.tickets);
            String htmlBody = templateBuilder.buildOrderConfirmationHtml(orderData.order);

            // 5. Send Email (outside transaction)
            try {
                emailProviderClient.sendEmailWithAttachment(
                        message.getRecipientEmail(),
                        "Your TicketBox E-Tickets (Order #" + orderData.order.getId() + ")",
                        htmlBody,
                        pdfAttachment,
                        "etickets-" + orderData.order.getId() + ".pdf",
                        message.getJobId()
                );
            } catch (HttpClientErrorException e) {
                // 4xx errors are permanent failures (e.g., malformed email, rejected by provider)
                log.error("Job {}: Provider permanent error (HTTP {}): {}", message.getJobId(), e.getStatusCode(), e.getResponseBodyAsString());
                
                // Update status in a separate short transaction
                transactionTemplate.executeWithoutResult(status -> {
                    Order orderToUpdate = orderRepository.findById(message.getOrderId()).orElse(null);
                    if (orderToUpdate != null) {
                        handleBusinessFailure(message.getJobId(), orderToUpdate, "Provider rejected request: " + e.getStatusCode());
                    }
                });
                // Throw AmqpRejectAndDontRequeueException to route to DLQ immediately without retry
                throw new AmqpRejectAndDontRequeueException("Permanent provider error: " + e.getStatusCode(), e);
            } catch (RestClientResponseException e) {
                // 5xx errors or other RestClient exceptions. Log response body.
                log.error("Job {}: Provider transient error (HTTP {}): {}", message.getJobId(), e.getStatusCode(), e.getResponseBodyAsString());
                throw e; // Bubble up for retry
            }

            // 6. Update Status in a separate short transaction
            transactionTemplate.executeWithoutResult(status -> {
                Order orderToUpdate = orderRepository.findById(message.getOrderId()).orElseThrow();
                orderToUpdate.setEmailStatus(EmailStatus.SENT);
                orderRepository.save(orderToUpdate);
            });
            
            jobTracker.updateStatus(message.getJobId(), "COMPLETED");
            log.info("Job {}: Email sent successfully for order {}", message.getJobId(), message.getOrderId());

        } catch (AmqpRejectAndDontRequeueException e) {
            throw e; // Already logged and handled
        } catch (Exception e) {
            log.error("Job {}: System error occurred: {}", message.getJobId(), e.getMessage());
            throw e; 
        } finally {
            // We do not release the lock immediately to prevent race conditions during rapid retries.
        }
    }
    
    private OrderData fetchOrderData(EmailTaskMessage message) {
        Order order = orderRepository.findById(message.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + message.getOrderId()));

        if (order.getEmailStatus() == EmailStatus.SENT) {
            log.warn("Job {}: Email for order {} was already sent. Skipping.", message.getJobId(), message.getOrderId());
            jobTracker.updateStatus(message.getJobId(), "COMPLETED", "Already sent");
            return null;
        }

        if (message.getRecipientEmail() == null || !message.getRecipientEmail().contains("@")) {
            handleBusinessFailure(message.getJobId(), order, "Invalid recipient email address");
            return null;
        }

        List<Ticket> tickets = ticketRepository.findAllById(message.getTicketIds());
        if (tickets.isEmpty()) {
            handleBusinessFailure(message.getJobId(), order, "No tickets found for order");
            return null;
        }
        
        // Initialize lazy properties needed for email and PDF generation
        order.getUser().getUsername();
        tickets.forEach(t -> {
            t.getCategory().getName();
            t.getCategory().getConcert().getName();
        });
        
        return new OrderData(order, tickets);
    }

    private void handleBusinessFailure(String jobId, Order order, String reason) {
        log.error("Job {}: Business logic failure: {}", jobId, reason);
        order.setEmailStatus(EmailStatus.FAILED);
        orderRepository.save(order);
        jobTracker.updateStatus(jobId, "FAILED", reason);
    }
    
    private static class OrderData {
        final Order order;
        final List<Ticket> tickets;
        
        OrderData(Order order, List<Ticket> tickets) {
            this.order = order;
            this.tickets = tickets;
        }
    }
}
