package com.ticketbox.backend.worker.csv;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.CsvImportMessage;
import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.entity.Guest;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.repository.GuestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1"
})
public class CsvWorkerExtendedValidationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private CsvImportProgressTracker progressTracker;

    private Concert testConcert;
    private File tempCsvFile;

    @BeforeEach
    public void setup() throws IOException {
        guestRepository.deleteAll();
        concertRepository.deleteAll();

        // Drain queues before tests
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        admin.purgeQueue(RabbitMQConfig.QUEUE_CSV, false);
        admin.purgeQueue("csv_queue.dlq", false);

        testConcert = Concert.builder()
                .name("Extended Test Concert")
                .description("Test Description")
                .location("Test Venue")
                .startTime(LocalDateTime.now().plusDays(10))
                .endTime(LocalDateTime.now().plusDays(10).plusHours(2))
                .build();
        concertRepository.save(testConcert);
    }

    @AfterEach
    public void teardown() {
        if (tempCsvFile != null && tempCsvFile.exists()) {
            tempCsvFile.delete();
        }
    }

    private void writeCsvContent(String content) throws IOException {
        tempCsvFile = File.createTempFile("test-guest-list", ".csv");
        try (FileWriter writer = new FileWriter(tempCsvFile)) {
            writer.write(content);
        }
    }

    @Test
    public void testLargeFileProcessing() throws Exception {
        StringBuilder csv = new StringBuilder("email,fullName\n");
        int totalRows = 500;
        for (int i = 0; i < totalRows; i++) {
            csv.append("user").append(i).append("@example.com,User ").append(i).append("\n");
        }
        writeCsvContent(csv.toString());

        String jobId = UUID.randomUUID().toString();
        CsvImportMessage msg = new CsvImportMessage(jobId, tempCsvFile.getAbsolutePath(), testConcert.getId(), 1L);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_CSV, msg);

        // Wait for processing to complete
        Thread.sleep(15000);

        assertEquals(500, guestRepository.count());

        CsvImportProgress progress = progressTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(500, progress.getTotalRows());
        assertEquals(500, progress.getSuccessfulRows());
        assertEquals(0, progress.getFailedRows());
    }

    @Test
    public void testRetryAndDlqBehavior() throws Exception {
        // Create an invalid file path to force an IOException in the worker (simulating system failure)
        String jobId = UUID.randomUUID().toString();
        String invalidPath = "/tmp/non-existent-file-" + UUID.randomUUID() + ".csv";
        CsvImportMessage msg = new CsvImportMessage(jobId, invalidPath, testConcert.getId(), 1L);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_CSV, msg);

        // Wait enough time for 3 retries (1s, 2s, 4s...) -> ~8-10 seconds total
        Thread.sleep(12000);

        // Check DB (should be 0)
        assertEquals(0, guestRepository.count());

        // Check DLQ for the message
        Message dlqMessage = rabbitTemplate.receive("csv_queue.dlq", 5000);
        assertNotNull(dlqMessage, "Message should be routed to DLQ after exhausting retries");

        // The original progress should be marked as FAILED since it crashed during initialization
        CsvImportProgress progress = progressTracker.getProgress(jobId);
        if (progress != null) {
            assertEquals("FAILED", progress.getStatus());
        }
    }
}
