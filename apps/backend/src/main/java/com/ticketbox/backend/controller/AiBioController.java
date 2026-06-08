package com.ticketbox.backend.controller;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.AiBioMessage;
import com.ticketbox.backend.worker.ai.AiJobProgress;
import com.ticketbox.backend.worker.ai.AiJobTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/concerts")
@RequiredArgsConstructor
@Slf4j
public class AiBioController {

    private final RabbitTemplate rabbitTemplate;
    private final AiJobTracker aiJobTracker;
    
    // Use an absolute or relative path for uploads. We'll create it if it doesn't exist.
    private static final String UPLOAD_DIR = "uploads/pdfs";

    @PostMapping("/{id}/upload-bio")
    public ResponseEntity<Map<String, String>> uploadAiBioPdf(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
            
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // 1. Ensure directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Save file
            String jobId = UUID.randomUUID().toString();
            String fileName = id + "_" + jobId + ".pdf";
            Path filePath = uploadPath.resolve(fileName);
            // Must use absolute path because MultipartFile.transferTo resolves relative paths against the server's temp directory
            file.transferTo(filePath.toAbsolutePath().toFile());

            // 3. Mark job as pending
            aiJobTracker.updateStatus(jobId, id, "PENDING", null);

            // 4. Send to RabbitMQ
            AiBioMessage message = AiBioMessage.builder()
                    .jobId(jobId)
                    .concertId(id)
                    .pdfStoragePath(filePath.toAbsolutePath().toString())
                    .metadata(Map.of("overwrite", true)) // Allow overwrite from Admin panel
                    .build();
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_AI, message);
            log.info("Queued AI Bio Extraction job {} for concert {}", jobId, id);

            return ResponseEntity.ok(Map.of("jobId", jobId));
            
        } catch (IOException e) {
            log.error("Failed to save PDF file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save file on server"));
        }
    }

    @GetMapping("/ai-jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        AiJobProgress progress = aiJobTracker.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
}
