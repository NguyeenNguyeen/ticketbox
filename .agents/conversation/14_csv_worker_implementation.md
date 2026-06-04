# Phase 3: CSV Guest List Import Worker Implementation

## Files Created
- `Guest.java` (Entity): New JPA entity mapped to the `guests` table with a unique constraint on `(concert_id, email)` for idempotency.
- `GuestRepository.java`: JPA repository for inserting and checking duplicates.
- `CsvImportProgress.java`: DTO representing the state of the import (total rows, processed, success, failure).
- `CsvImportProgressTracker.java`: Redis-backed component ensuring real-time polling capabilities.
- `CsvRowValidator.java`: Domain logic to validate single CSV rows independent of state.
- `CsvImportService.java`: Core orchestration service for streaming, validating, persisting, and tracking progress.
- `CsvImportConsumer.java`: RabbitMQ `@RabbitListener` bound to `csv_queue`.
- `CsvWorkerTest.java`: Comprehensive Spring Boot integration test covering successful import, row failures, and duplicates.

## Files Modified
- `pom.xml`: Added `commons-csv` dependency for reliable, memory-safe CSV parsing.
- `CsvImportMessage.java`: Added `jobId` and `organizerId` to support tracking and context.

## Architecture Alignment
The implementation adheres strictly to the `13_csv_worker_design.md` document. It leverages streaming via `Apache Commons CSV` so the entire file is never held in memory. Redis is updated intelligently (batched inserts are combined with progress updates) rather than on every single row to prevent bottlenecking. 

## Processing Flow
1. **Consumption**: The `CsvImportConsumer` reads a `CsvImportMessage` containing the `jobId` and file reference.
2. **Streaming**: The `CsvImportService` opens a file stream using `CSVParser`.
3. **Row-by-Row Evaluation**: 
   - Each row is validated via `CsvRowValidator`. Bad rows increment the failure count and are skipped.
   - Idempotency is pre-checked via `guestRepository.existsByConcertIdAndEmail`.
4. **Chunked Inserts**: Valid rows are accumulated into memory up to a `BATCH_SIZE` of 50.
5. **Persistence**: The chunk is persisted via `guestRepository.saveAll()`. The progress tracker is updated in Redis.
6. **Completion**: Once the file ends, remaining rows are flushed, and progress is marked as `COMPLETED`.

## Idempotency Strategy
A unique index `uk_guest_concert_email` enforces that no two identical emails can be assigned to the same concert. If the worker crashes mid-stream, RabbitMQ will redeliver the message. The pre-check `existsByConcertIdAndEmail` and database-level unique constraints will naturally treat previously-inserted rows as duplicates and safely skip them, allowing the worker to seamlessly resume without duplicate data generation.

## Fault Tolerance Strategy
- **Row-Level Faults (Bad Data)**: Caught, logged, and skipped. The `failedRows` counter in Redis increments. The loop continues to the next row. The process is never halted by an invalid email or missing name.
- **Batch Insertion Conflicts**: If a chunked DB insertion throws a `DataIntegrityViolationException`, the service automatically falls back to single-row insertions to pinpoint the duplicate, skipping the bad row while preserving the rest of the batch.
- **System-Level Faults**: Errors like DB connection drops or disk read failures are thrown out of the consumer to intentionally trigger the Spring AMQP `RetryOperationsInterceptor`, which will retry up to 3 times before routing to the DLQ.

## Test Coverage
- **Valid Import**: Verifies all rows are inserted and progress reflects 100% success.
- **Invalid Rows**: Deliberately provides malformed emails and missing fields to ensure the worker skips them without crashing.
- **Duplicate Guest**: Ensures pre-existing rows and duplicate rows within the same file are properly ignored without affecting valid rows.

## Known Limitations
- Currently, the file path is assumed to be a local absolute path (e.g., from `File.createTempFile`). In production, this would integrate with a real `StorageService` (like AWS S3) which fetches the file stream directly into the parser without downloading it to disk.
