package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.dto.email.EmailAttachment;
import java.util.List;

public interface EmailProviderClient {
    void sendEmailWithAttachment(String to, String subject, String htmlBody, List<EmailAttachment> attachments, String jobId);
}
