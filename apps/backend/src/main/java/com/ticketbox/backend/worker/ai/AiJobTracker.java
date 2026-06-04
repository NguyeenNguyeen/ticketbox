package com.ticketbox.backend.worker.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobTracker {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "ticketbox:job:ai:";
    private static final Duration TTL = Duration.ofDays(1);

    public void updateStatus(String jobId, Long concertId, String status, String errorReason) {
        AiJobProgress progress = AiJobProgress.builder()
                .jobId(jobId)
                .concertId(concertId)
                .status(status)
                .errorReason(errorReason)
                .build();
        try {
            String json = objectMapper.writeValueAsString(progress);
            redisTemplate.opsForValue().set(KEY_PREFIX + jobId, json, TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AI job progress for jobId {}", jobId, e);
        }
    }

    public AiJobProgress getProgress(String jobId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + jobId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AiJobProgress.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize AI job progress for jobId {}", jobId, e);
            return null;
        }
    }
}
