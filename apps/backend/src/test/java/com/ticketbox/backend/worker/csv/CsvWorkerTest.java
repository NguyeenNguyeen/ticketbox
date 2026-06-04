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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1"
})
public class CsvWorkerTest {

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

        testConcert = Concert.builder()
                .name("Test Concert")
                .description("Test Description")
                .location("Test Venue")
                .startTime(LocalDateTime.now().plusDays(10))
                .endTime(LocalDateTime.now().plusDays(10).plusHours(2))
                .build();
        concertRepository.save(testConcert);

        tempCsvFile = File.createTempFile("test-guest-list", ".csv");
    }

    @AfterEach
    public void teardown() {
        if (tempCsvFile != null && tempCsvFile.exists()) {
            tempCsvFile.delete();
        }
    }

    private void writeCsvContent(String content) throws IOException {
        try (FileWriter writer = new FileWriter(tempCsvFile)) {
            writer.write(content);
        }
    }

    @Test
    public void testValidCsvImport() throws Exception {
        String csv = "email,fullName\n" +
                     "alice@example.com,Alice Smith\n" +
                     "bob@example.com,Bob Jones\n";
        writeCsvContent(csv);

        String jobId = UUID.randomUUID().toString();
        CsvImportMessage msg = new CsvImportMessage(jobId, tempCsvFile.getAbsolutePath(), testConcert.getId(), 1L);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_CSV, msg);

        // Wait for async processing
        Thread.sleep(3000);

        assertEquals(2, guestRepository.count());

        CsvImportProgress progress = progressTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(2, progress.getTotalRows());
        assertEquals(2, progress.getProcessedRows());
        assertEquals(2, progress.getSuccessfulRows());
        assertEquals(0, progress.getFailedRows());
    }

    @Test
    public void testInvalidRowHandling() throws Exception {
        String csv = "email,fullName\n" +
                     "alice@example.com,Alice Smith\n" +
                     "invalid-email,Bad User\n" +
                     ",Missing Email\n" +
                     "bob@example.com,Bob Jones\n";
        writeCsvContent(csv);

        String jobId = UUID.randomUUID().toString();
        CsvImportMessage msg = new CsvImportMessage(jobId, tempCsvFile.getAbsolutePath(), testConcert.getId(), 1L);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_CSV, msg);

        Thread.sleep(3000);

        assertEquals(2, guestRepository.count());

        CsvImportProgress progress = progressTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(4, progress.getTotalRows());
        assertEquals(2, progress.getSuccessfulRows());
        assertEquals(2, progress.getFailedRows());
    }

    @Test
    public void testDuplicateGuestHandling() throws Exception {
        // Pre-insert a guest
        Guest existingGuest = Guest.builder()
                .concert(testConcert)
                .email("alice@example.com")
                .fullName("Existing Alice")
                .build();
        guestRepository.save(existingGuest);

        String csv = "email,fullName\n" +
                     "alice@example.com,Alice Smith\n" + // Duplicate (should skip)
                     "bob@example.com,Bob Jones\n" +     // New (should insert)
                     "bob@example.com,Bob Copy\n";       // Duplicate within same file (should skip)
        writeCsvContent(csv);

        String jobId = UUID.randomUUID().toString();
        CsvImportMessage msg = new CsvImportMessage(jobId, tempCsvFile.getAbsolutePath(), testConcert.getId(), 1L);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_CSV, msg);

        Thread.sleep(3000);

        assertEquals(2, guestRepository.count()); // Pre-existing + Bob

        CsvImportProgress progress = progressTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals(3, progress.getTotalRows());
        assertEquals(1, progress.getSuccessfulRows());
        assertEquals(2, progress.getFailedRows());
    }
}
