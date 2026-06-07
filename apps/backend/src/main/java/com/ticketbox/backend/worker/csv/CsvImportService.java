package com.ticketbox.backend.worker.csv;

import com.ticketbox.backend.dto.async.CsvImportMessage;
import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.entity.Guest;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.repository.GuestRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CsvImportService {

    private static final int BATCH_SIZE = 50;

    private final GuestRepository guestRepository;
    private final ConcertRepository concertRepository;
    private final CsvRowValidator rowValidator;
    private final CsvImportProgressTracker progressTracker;

    public CsvImportService(GuestRepository guestRepository,
                            ConcertRepository concertRepository,
                            CsvRowValidator rowValidator,
                            CsvImportProgressTracker progressTracker) {
        this.guestRepository = guestRepository;
        this.concertRepository = concertRepository;
        this.rowValidator = rowValidator;
        this.progressTracker = progressTracker;
    }

    public void processCsv(CsvImportMessage message) {
        log.info("Starting CSV import job: {}", message.getJobId());

        Concert concert = concertRepository.findById(message.getConcertId())
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + message.getConcertId()));

        CsvImportProgress progress = progressTracker.getProgress(message.getJobId());
        if (progress == null) {
            progressTracker.initializeProgress(message.getJobId(), 0); // Total rows unknown at start if streaming
            progress = progressTracker.getProgress(message.getJobId());
        }
        progress.setStatus("PROCESSING");
        progressTracker.updateProgress(message.getJobId(), progress);

        List<Guest> guestBatch = new ArrayList<>();

        // In a real scenario, fileId would be resolved from a StorageService.
        // For this worker implementation, we assume fileId is a local absolute path for simplicity.
        try (Reader reader = new FileReader(message.getFileId());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                progress.setTotalRows(progress.getTotalRows() + 1);
                progress.setProcessedRows(progress.getProcessedRows() + 1);

                if (!rowValidator.isValid(csvRecord)) {
                    log.warn("Job {}: Invalid row at line {}", message.getJobId(), csvRecord.getRecordNumber());
                    progress.setFailedRows(progress.getFailedRows() + 1);
                    continue;
                }

                String email = csvRecord.get("email");
                String fullName = csvRecord.get("fullName");

                // Idempotency: Pre-check duplicate
                if (guestRepository.existsByConcertIdAndEmail(concert.getId(), email)) {
                    log.info("Job {}: Duplicate guest found for email {} at line {}, skipping", message.getJobId(), email, csvRecord.getRecordNumber());
                    progress.setFailedRows(progress.getFailedRows() + 1);
                    continue;
                }

                Guest guest = Guest.builder()
                        .concert(concert)
                        .email(email)
                        .fullName(fullName)
                        .build();

                guestBatch.add(guest);

                if (guestBatch.size() >= BATCH_SIZE) {
                    saveBatch(guestBatch, progress, message.getJobId());
                    guestBatch.clear();
                    progressTracker.updateProgress(message.getJobId(), progress);
                }
            }

            // Save remaining
            if (!guestBatch.isEmpty()) {
                saveBatch(guestBatch, progress, message.getJobId());
            }

            progressTracker.completeProgress(message.getJobId(), progress);
            log.info("Completed CSV import job: {}. Successful: {}, Failed: {}", 
                     message.getJobId(), progress.getSuccessfulRows(), progress.getFailedRows());

        } catch (Exception e) {
            log.error("Job {}: System error during CSV processing: {}", message.getJobId(), e.getMessage(), e);
            progressTracker.failProgress(message.getJobId(), progress);
            // Rethrow to trigger RabbitMQ retry
            throw new RuntimeException("Failed to process CSV", e);
        }
    }

    protected void saveBatch(List<Guest> guestBatch, CsvImportProgress progress, String jobId) {
        try {
            guestRepository.saveAll(guestBatch);
            progress.setSuccessfulRows(progress.getSuccessfulRows() + guestBatch.size());
        } catch (DataIntegrityViolationException e) {
            log.warn("Job {}: Batch insert failed due to duplicate/integrity constraint. Falling back to single inserts.", jobId);
            // Fallback: insert one by one to isolate the duplicate
            for (Guest guest : guestBatch) {
                try {
                    saveSingle(guest);
                    progress.setSuccessfulRows(progress.getSuccessfulRows() + 1);
                } catch (DataIntegrityViolationException ex) {
                    log.info("Job {}: Duplicate guest found during fallback for email {}, skipping", jobId, guest.getEmail());
                    progress.setFailedRows(progress.getFailedRows() + 1);
                }
            }
        }
    }

    protected void saveSingle(Guest guest) {
        guestRepository.save(guest);
    }
}
