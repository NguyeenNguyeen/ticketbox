package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.dto.email.EmailAttachment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ticketbox.email.provider", havingValue = "smtp")
@Slf4j
public class SmtpEmailClient implements EmailProviderClient {

    private final JavaMailSender javaMailSender;

    @Value("${ticketbox.email.from}")
    private String senderEmail;

    public SmtpEmailClient(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, List<EmailAttachment> attachments, String jobId) {
        log.info("Sending request to SMTP Server for email to: {}", maskEmail(to));

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML

            if (attachments != null) {
                for (EmailAttachment attachment : attachments) {
                    ByteArrayResource resource = new ByteArrayResource(attachment.getData());
                    if (attachment.getContentId() != null && !attachment.getContentId().isEmpty()) {
                        helper.addInline(attachment.getContentId(), resource, attachment.getMimeType());
                    } else {
                        helper.addAttachment(attachment.getFilename(), resource);
                    }
                }
            }

            javaMailSender.send(message);
            log.info("Successfully sent email via SMTP to: {}", maskEmail(to));
        } catch (MessagingException e) {
            log.error("Failed to construct MIME message for Job ID {}: {}", jobId, e.getMessage());
            throw new RuntimeException("Failed to construct email", e);
        } catch (Exception e) {
            log.error("SMTP Delivery failed for Job ID {}: {}", jobId, e.getMessage());
            // Throw generic exception to let RabbitMQ retry or DLQ
            throw new RuntimeException("SMTP delivery failed", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
