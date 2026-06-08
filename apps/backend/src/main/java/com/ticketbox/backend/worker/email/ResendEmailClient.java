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
@ConditionalOnProperty(name = "ticketbox.email.provider", havingValue = "resend", matchIfMissing = true)
@Slf4j
public class ResendEmailClient implements EmailProviderClient {

    private final RestTemplate restTemplate;

    @Value("${ticketbox.email.resend.key}")
    private String apiKey;

    @Value("${ticketbox.email.from}")
    private String senderEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    public ResendEmailClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachment, String filename, String jobId) {
        log.info("Sending request to Resend API for email to: {}", maskEmail(to));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("Idempotency-Key", jobId);

        String base64Attachment = Base64.getEncoder().encodeToString(attachment);

        Map<String, Object> body = Map.of(
                "from", senderEmail,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody,
                "attachments", List.of(
                        Map.of(
                                "filename", filename,
                                "content", base64Attachment
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(RESEND_API_URL, request, String.class);
        log.info("Received successful response from Resend API for email to: {}", maskEmail(to));
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
