package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.dto.async.EmailTaskMessage;
import com.ticketbox.backend.entity.EmailStatus;
import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EmailService(OrderRepository orderRepository,
                        TicketRepository ticketRepository,
                        EmailProviderClient emailProviderClient,
                        EmailJobTracker jobTracker,
                        TicketAttachmentService attachmentService,
                        EmailTemplateBuilder templateBuilder,
                        RedisTemplate<String, String> redisTemplate) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.emailProviderClient = emailProviderClient;
        this.jobTracker = jobTracker;
        this.attachmentService = attachmentService;
        this.templateBuilder = templateBuilder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
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
            // 2. Fetch data
            Order order = orderRepository.findById(message.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + message.getOrderId()));

            // 3. Long-term idempotency check (Database)
            if (order.getEmailStatus() == EmailStatus.SENT) {
                log.warn("Job {}: Email for order {} was already sent. Skipping.", message.getJobId(), message.getOrderId());
                jobTracker.updateStatus(message.getJobId(), "COMPLETED", "Already sent");
                return;
            }

            if (message.getRecipientEmail() == null || !message.getRecipientEmail().contains("@")) {
                handleBusinessFailure(message.getJobId(), order, "Invalid recipient email address");
                return;
            }

            List<Ticket> tickets = ticketRepository.findAllById(message.getTicketIds());
            if (tickets.isEmpty()) {
                handleBusinessFailure(message.getJobId(), order, "No tickets found for order");
                return;
            }

            // 4. Generate content
            byte[] pdfAttachment = attachmentService.generateETicketPdf(tickets);
            String htmlBody = templateBuilder.buildOrderConfirmationHtml(order);

            // 5. Send Email
            emailProviderClient.sendEmailWithAttachment(
                    message.getRecipientEmail(),
                    "Your TicketBox E-Tickets (Order #" + order.getId() + ")",
                    htmlBody,
                    pdfAttachment,
                    "etickets-" + order.getId() + ".pdf"
            );

            // 6. Update Status
            order.setEmailStatus(EmailStatus.SENT);
            orderRepository.save(order);
            jobTracker.updateStatus(message.getJobId(), "COMPLETED");
            log.info("Job {}: Email sent successfully for order {}", message.getJobId(), order.getId());

        } catch (Exception e) {
            // Note: Transient exceptions (e.g., RestClientException) should bubble up to trigger AMQP retry.
            // Do NOT mark as FAILED here, otherwise retries won't happen.
            log.error("Job {}: System error occurred: {}", message.getJobId(), e.getMessage());
            throw e; 
        } finally {
            // We do not release the lock immediately to prevent race conditions during rapid retries.
            // It will naturally expire after 5 minutes.
        }
    }

    private void handleBusinessFailure(String jobId, Order order, String reason) {
        log.error("Job {}: Business logic failure: {}", jobId, reason);
        order.setEmailStatus(EmailStatus.FAILED);
        orderRepository.save(order);
        jobTracker.updateStatus(jobId, "FAILED", reason);
    }
}
