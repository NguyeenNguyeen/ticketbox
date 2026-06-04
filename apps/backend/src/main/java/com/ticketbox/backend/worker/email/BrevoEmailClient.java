package com.ticketbox.backend.worker.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ticketbox.email.provider", havingValue = "brevo")
@Slf4j
public class BrevoEmailClient implements EmailProviderClient {

    private final RestTemplate restTemplate;

    @Value("${ticketbox.email.brevo.key}")
    private String apiKey;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_EMAIL = "tickets@ticketbox.com";
    private static final String SENDER_NAME = "TicketBox";

    public BrevoEmailClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachment, String filename, String jobId) {
        log.info("Sending request to Brevo API for email to: {}", maskEmail(to));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);
        headers.set("Idempotency-Key", jobId);

        String base64Attachment = Base64.getEncoder().encodeToString(attachment);

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", SENDER_NAME, "email", SENDER_EMAIL),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody,
                "attachment", List.of(
                        Map.of(
                                "name", filename,
                                "content", base64Attachment
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(BREVO_API_URL, request, String.class);
        log.info("Received successful response from Brevo API for email to: {}", maskEmail(to));
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
