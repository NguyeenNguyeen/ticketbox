package com.ticketbox.backend.worker.csv;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.CsvImportMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CsvImportConsumer {

    private final CsvImportService csvImportService;

    public CsvImportConsumer(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CSV)
    public void consumeCsvImport(CsvImportMessage message) {
        log.info("Received CSV Import message for Job: {}, Concert: {}", message.getJobId(), message.getConcertId());
        try {
            csvImportService.processCsv(message);
        } catch (Exception e) {
            log.error("Unhandled exception in CSV Import Worker for Job: {}", message.getJobId(), e);
            // Rethrowing is critical to trigger Spring AMQP's RetryOperationsInterceptor
            // If it succeeds, it's fine. If it fails 3 times, it goes to DLQ.
            throw e;
        }
    }
}
