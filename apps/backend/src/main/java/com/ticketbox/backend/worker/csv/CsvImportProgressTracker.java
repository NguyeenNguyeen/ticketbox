package com.ticketbox.backend.worker.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CsvImportProgressTracker {

    private static final String KEY_PREFIX = "import:progress:";
    private static final Duration TTL = Duration.ofDays(7); // Keep status for 7 days

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CsvImportProgressTracker(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void initializeProgress(String jobId, int totalRows) {
        CsvImportProgress progress = CsvImportProgress.builder()
                .status("PENDING")
                .totalRows(totalRows)
                .processedRows(0)
                .successfulRows(0)
                .failedRows(0)
                .build();
        saveProgress(jobId, progress);
    }

    public void updateProgress(String jobId, CsvImportProgress progress) {
        saveProgress(jobId, progress);
    }

    public void completeProgress(String jobId, CsvImportProgress progress) {
        progress.setStatus("COMPLETED");
        saveProgress(jobId, progress);
    }

    public void failProgress(String jobId, CsvImportProgress progress) {
        progress.setStatus("FAILED");
        saveProgress(jobId, progress);
    }

    public CsvImportProgress getProgress(String jobId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + jobId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CsvImportProgress.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize progress", e);
        }
    }

    private void saveProgress(String jobId, CsvImportProgress progress) {
        try {
            String json = objectMapper.writeValueAsString(progress);
            redisTemplate.opsForValue().set(KEY_PREFIX + jobId, json, TTL);
        } catch (Exception e) {
            // Non-fatal, just logging could be added.
            // Failing to update progress should not kill the worker process.
        }
    }
}
