package com.ticketbox.backend.worker.ai;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.AiBioMessage;
import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.repository.ConcertRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1",
    "ticketbox.ai.provider=gemini" // Using Gemini as default for test
})
public class AiWorkerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private com.ticketbox.backend.repository.TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private AiJobTracker jobTracker;

    @MockBean
    private RestTemplate restTemplate;

    private Concert testConcert;
    private File tempPdfFile;
    private File emptyPdfFile;
    private File corruptedPdfFile;

    @BeforeEach
    public void setup() throws IOException {
        ticketCategoryRepository.deleteAll();
        concertRepository.deleteAll();

        // Drain queues
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        admin.purgeQueue(RabbitMQConfig.QUEUE_AI, false);
        admin.purgeQueue("ai_queue.dlq", false);

        testConcert = Concert.builder()
                .name("AI Test Concert")
                .description("Test Description")
                .location("Test Venue")
                .startTime(LocalDateTime.now().plusDays(10))
                .endTime(LocalDateTime.now().plusDays(10).plusHours(2))
                .build();
        concertRepository.save(testConcert);

        tempPdfFile = createTestPdf("This is a test biography of the artist John Doe. He plays rock music.", "test-artist");
        emptyPdfFile = createTestPdf("", "empty-artist");
        
        corruptedPdfFile = File.createTempFile("corrupted-artist", ".pdf");
        java.nio.file.Files.writeString(corruptedPdfFile.toPath(), "This is not a real PDF file, just some garbage text.");
    }

    @AfterEach
    public void teardown() {
        if (tempPdfFile != null && tempPdfFile.exists()) tempPdfFile.delete();
        if (emptyPdfFile != null && emptyPdfFile.exists()) emptyPdfFile.delete();
        if (corruptedPdfFile != null && corruptedPdfFile.exists()) corruptedPdfFile.delete();
    }

    private File createTestPdf(String text, String prefix) throws IOException {
        File file = File.createTempFile(prefix, ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(file);
        }
        return file;
    }

    @Test
    public void testSuccessfulAiBioGeneration() throws Exception {
        // Mock API response
        Map<String, Object> mockResponse = Map.of(
            "candidates", List.of(
                Map.of("content", Map.of(
                    "parts", List.of(
                        Map.of("text", "```json\n{\"biography\": \"John Doe is a legendary rock artist... this is a sufficiently long mock biography text that passes the thirty word minimum limit check inside the validator to prevent warning logs and simulate a real output.\", \"keyGenres\": [\"Rock\"]}\n```")
                    )
                ))
            )
        );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<Map>>any()))
                .thenReturn(mockResponse);

        String jobId = UUID.randomUUID().toString();
        AiBioMessage msg = new AiBioMessage(jobId, testConcert.getId(), 1L, tempPdfFile.getAbsolutePath(), "corr-1", null);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_AI, msg);

        Thread.sleep(3000); // Wait for async processing

        Concert updated = concertRepository.findById(testConcert.getId()).orElseThrow();
        assertFalse(updated.getArtists().isEmpty());
        assertTrue(updated.getArtists().iterator().next().getBio().contains("John Doe is a legendary rock artist"));

        AiJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
    }

    @Test
    public void testEmptyPdfFailure() throws Exception {
        String jobId = UUID.randomUUID().toString();
        AiBioMessage msg = new AiBioMessage(jobId, testConcert.getId(), 1L, emptyPdfFile.getAbsolutePath(), "corr-1", null);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_AI, msg);

        Thread.sleep(2000);

        Concert updated = concertRepository.findById(testConcert.getId()).orElseThrow();
        assertTrue(updated.getArtists().isEmpty() || updated.getArtists().iterator().next().getBio() == null);

        AiJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("FAILED", progress.getStatus());
        assertTrue(progress.getErrorReason().contains("insufficient extractable text"));
    }

    @Test
    public void testCorruptedPdfFailure() throws Exception {
        String jobId = UUID.randomUUID().toString();
        AiBioMessage msg = new AiBioMessage(jobId, testConcert.getId(), 1L, corruptedPdfFile.getAbsolutePath(), "corr-1", null);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_AI, msg);

        Thread.sleep(2000);

        Concert updated = concertRepository.findById(testConcert.getId()).orElseThrow();
        assertTrue(updated.getArtists().isEmpty() || updated.getArtists().iterator().next().getBio() == null);

        AiJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("FAILED", progress.getStatus());
        assertTrue(progress.getErrorReason().contains("Failed to read PDF file") || progress.getErrorReason().contains("PdfExtractionService"));
    }

    @Test
    public void testInvalidJsonResponse() throws Exception {
        // Mock API response with bad JSON structure (missing biography field)
        Map<String, Object> mockResponse = Map.of(
            "candidates", List.of(
                Map.of("content", Map.of(
                    "parts", List.of(
                        Map.of("text", "```json\n{\"wrongField\": \"Some text\"}\n```")
                    )
                ))
            )
        );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<Map>>any()))
                .thenReturn(mockResponse);

        String jobId = UUID.randomUUID().toString();
        AiBioMessage msg = new AiBioMessage(jobId, testConcert.getId(), 1L, tempPdfFile.getAbsolutePath(), "corr-1", null);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_AI, msg);

        Thread.sleep(3000);

        Concert updated = concertRepository.findById(testConcert.getId()).orElseThrow();
        assertTrue(updated.getArtists().isEmpty() || updated.getArtists().iterator().next().getBio() == null, "Biography should remain null on invalid JSON");

        AiJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("FAILED", progress.getStatus());
        assertTrue(progress.getErrorReason().contains("missing the 'biography' field"));
    }

    @Test
    public void testAiTimeoutAndDlq() throws Exception {
        // Mock a consistent failure
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<Map>>any()))
                .thenThrow(new RestClientException("Simulated API timeout"));

        String jobId = UUID.randomUUID().toString();
        AiBioMessage msg = new AiBioMessage(jobId, testConcert.getId(), 1L, tempPdfFile.getAbsolutePath(), "corr-1", null);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_AI, msg);

        // Wait for 3 retries (1s, 2s, 4s...) -> ~8-10 seconds total
        Thread.sleep(12000);

        Concert updated = concertRepository.findById(testConcert.getId()).orElseThrow();
        assertTrue(updated.getArtists().isEmpty() || updated.getArtists().iterator().next().getBio() == null);

        // Check DLQ
        Message dlqMessage = rabbitTemplate.receive("ai_queue.dlq", 5000);
        assertNotNull(dlqMessage, "Message should be routed to DLQ after exhausting retries");

        AiJobProgress progress = jobTracker.getProgress(jobId);
        // It remains processing or gets updated eventually (in our code we don't mark FAILED on system exceptions directly, so it stays PENDING/PROCESSING)
        assertNotNull(progress);
        assertEquals("PROCESSING", progress.getStatus());
    }
}
