package com.ticketbox.backend.worker.email;

public interface EmailProviderClient {
    void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] attachment, String filename, String jobId);
}
