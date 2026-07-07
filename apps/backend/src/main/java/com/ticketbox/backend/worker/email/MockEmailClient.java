package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.dto.email.EmailAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ticketbox.email.provider", havingValue = "mock")
@Slf4j
public class MockEmailClient implements EmailProviderClient {

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, List<EmailAttachment> attachments, String jobId) {
        log.info("[MockEmailClient] Pretending to send email to: {}", to);
        log.info("[MockEmailClient] Subject: {}", subject);
        log.info("[MockEmailClient] Body length: {}", htmlBody != null ? htmlBody.length() : 0);
        log.info("[MockEmailClient] Attachments count: {}", attachments != null ? attachments.size() : 0);
        log.info("[MockEmailClient] Idempotency Key (Job ID): {}", jobId);
    }
}
