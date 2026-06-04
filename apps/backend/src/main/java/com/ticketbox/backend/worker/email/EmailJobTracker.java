package com.ticketbox.backend.worker.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailJobTracker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "email_job:";

    public EmailJobTracker(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void updateStatus(String jobId, String status) {
        updateStatus(jobId, status, null);
    }

    public void updateStatus(String jobId, String status, String errorReason) {
        try {
            EmailJobProgress progress = new EmailJobProgress(jobId, status, errorReason);
            String json = objectMapper.writeValueAsString(progress);
            redisTemplate.opsForValue().set(KEY_PREFIX + jobId, json, 7, TimeUnit.DAYS);
            log.info("Updated email job {} to status: {}", jobId, status);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email job progress", e);
        }
    }

    public EmailJobProgress getProgress(String jobId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + jobId);
        if (json != null) {
            try {
                return objectMapper.readValue(json, EmailJobProgress.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize email job progress", e);
            }
        }
        return null;
    }

    public static class EmailJobProgress {
        private String jobId;
        private String status;
        private String errorReason;

        public EmailJobProgress() {}

        public EmailJobProgress(String jobId, String status, String errorReason) {
            this.jobId = jobId;
            this.status = status;
            this.errorReason = errorReason;
        }

        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorReason() { return errorReason; }
        public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    }
}
