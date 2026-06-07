package com.ticketbox.backend.worker.ai;

import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.dto.async.AiBioMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiBioService {

    private final PdfExtractionService pdfExtractionService;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiProviderClient aiProviderClient;
    private final AiResponseValidator aiResponseValidator;
    private final ConcertRepository concertRepository;
    private final AiJobTracker aiJobTracker;

    @Transactional
    public void processBiographyGeneration(AiBioMessage message) {
        log.info("Starting AI biography generation for job: {}", message.getJobId());
        
        // 1. Mark job as processing
        aiJobTracker.updateStatus(message.getJobId(), message.getConcertId(), "PROCESSING", null);
        
        try {
            // 2. Fetch Concert to ensure it exists and prevent overwriting
            Concert concert = concertRepository.findById(message.getConcertId())
                    .orElseThrow(() -> new RuntimeException("Concert not found: " + message.getConcertId()));
            
            // Allow overwrite if metadata flag is present
            boolean overwrite = message.getMetadata() != null && 
                                Boolean.TRUE.equals(message.getMetadata().get("overwrite"));
                                
            if (concert.getArtistBiography() != null && !concert.getArtistBiography().isEmpty() && !overwrite) {
                log.warn("Job {}: Concert {} already has a biography. Skipping overwrite.", message.getJobId(), concert.getId());
                aiJobTracker.updateStatus(message.getJobId(), message.getConcertId(), "COMPLETED", "Skipped: Already exists");
                return;
            }

            // 3. Extract Text from PDF
            String rawText = pdfExtractionService.extractText(message.getPdfStoragePath());
            
            // 4. Build Prompt
            String prompt = aiPromptBuilder.buildPrompt(rawText);
            
            // 5. Call AI Provider (this is protected by @Retryable for transient HTTP faults)
            String rawAiResponse = aiProviderClient.generateBio(prompt);
            
            // 6. Validate & Extract
            String biography = aiResponseValidator.validateAndExtractBiography(rawAiResponse);
            
            // 7. Save to DB
            concert.setArtistBiography(biography);
            concertRepository.save(concert);
            
            // 8. Mark Complete
            aiJobTracker.updateStatus(message.getJobId(), message.getConcertId(), "COMPLETED", null);
            log.info("Job {}: Biography successfully generated and saved.", message.getJobId());
            
        } catch (UnprocessablePdfException | AiValidationException e) {
            // Business failures: Do not retry.
            log.error("Job {}: Business logic failure: {}", message.getJobId(), e.getMessage());
            aiJobTracker.updateStatus(message.getJobId(), message.getConcertId(), "FAILED", e.getMessage());
            // We acknowledge to RabbitMQ (no throw) because retrying won't fix a bad PDF or bad JSON format from LLM
        } catch (Exception e) {
            // System faults (e.g. DB down, HTTP timeout)
            log.error("Job {}: System error occurred: {}", message.getJobId(), e.getMessage());
            // We don't mark FAILED here immediately because RabbitMQ might retry this message. 
            // We rethrow to trigger the RabbitMQ backoff/DLQ logic.
            throw e;
        }
    }
}
