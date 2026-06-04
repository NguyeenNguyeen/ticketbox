# CSV Guest List Import Worker Design

## Requirements
1. Large files must not block HTTP requests.
2. CSV processing must run in the background.
3. Invalid rows must not stop the entire import.
4. Duplicate guests must not be imported twice.
5. Import progress must be observable.
6. Failed jobs must be recoverable.

## Architecture Overview
The CSV import process follows an asynchronous event-driven architecture:
1. **Upload:** The Organizer uploads a CSV file via a REST endpoint.
2. **Persistence:** The backend stores the file temporarily (e.g., local disk or cloud storage) and generates a unique `fileId` and `jobId`.
3. **Dispatch:** The backend initializes progress in Redis and publishes a `CsvImportMessage` to the `ticketbox.commands` exchange with routing key `csv.import`.
4. **Consumption:** The `CsvImportConsumer` asynchronously receives the message from `csv_queue`.
5. **Processing:** The `CsvImportService` streams the CSV file, passing each row to the `CsvRowValidator`.
6. **Insertion:** Valid rows are inserted into the database. Duplicates and invalid rows are skipped.
7. **Tracking:** Progress is periodically flushed to Redis so the Organizer can poll for status.
8. **Completion:** Once the stream ends, the final summary is saved, and the temporary file can be cleaned up.

## Message Contract
The existing `CsvImportMessage` should be extended to support robust tracking and authorization:

```java
public class CsvImportMessage implements Serializable {
    private String jobId;       // Unique ID for the import task and progress tracking
    private String fileId;      // Path or identifier of the uploaded CSV
    private Long concertId;     // Target concert for the guest list
    private Long organizerId;   // ID of the user who initiated the import
}
```

## Component Design
To maintain clean separation of concerns, the worker will consist of the following components:

- **`CsvImportConsumer`**: The entry point. A Spring `@RabbitListener` bound to `csv_queue`. It receives the message, logs the start, and delegates to the service.
- **`CsvImportService`**: The core orchestrator. Manages the file stream, orchestrates validation, and handles batch database insertions.
- **`CsvRowValidator`**: A pure validation component. Checks for required fields (e.g., valid email format, non-empty names) and applies business rules to a single CSV row.
- **`ImportProgressTracker`**: A Redis-backed component responsible for creating, updating, and fetching the import status and metrics.
- **`GuestService` / `TicketRepository`**: The data access layer. Handles checking for duplicates and saving the guests.

## Processing Strategy
**Recommendation: Streaming with Chunked Inserts**

- **Why Streaming?** Loading a multi-gigabyte CSV into memory (Full File Loading) will cause `OutOfMemoryError` crashes. Streaming reads one line at a time, keeping the memory footprint at O(1).
- **Why Chunking?** Inserting one row at a time is inefficient due to network round-trips. We will stream the file line-by-line, accumulate valid rows into a chunk (e.g., 50-100 rows), and perform a batch DB insert using JPA `saveAll()`.

## Fault Tolerance
The worker must be highly resilient. Faults are handled at two levels:

1. **Row-Level Failures (Business Errors):**
   - *Invalid Format / Missing Data:* Caught by `CsvRowValidator`. The row is discarded, the `failedRows` counter increments, and processing continues.
   - *Duplicate Guest:* Handled by DB constraints or pre-insert checks. The row is ignored, `failedRows` increments, and processing continues.
   - *Result:* The import completes successfully, skipping only the bad data.

2. **System-Level Failures (Infrastructure Errors):**
   - *Database Down / Redis Down:* Caught as a system exception. The exception bubbles up out of the listener, triggering the Spring AMQP `RetryOperationsInterceptor`.
   - *Result:* The entire job is retried later.

## Idempotency
- **Identity:** A guest is uniquely identified by the composite key `(concertId, email)`.
- **Database Constraint:** A `UNIQUE` constraint must exist on `(concert_id, email)` in the target table to guarantee safety at the lowest level.
- **Retry Safety:** If the worker crashes at row 5,000 and RabbitMQ redelivers the message, the worker starts again from row 1. Because of the unique constraint, rows 1-5,000 will be treated as duplicates and safely skipped. The worker will seamlessly pick up new inserts from row 5,001.

## Progress Tracking
Progress will be tracked in Redis to allow fast, high-concurrency polling from the frontend.

- **Data Structure:** A Redis Hash under key `import:progress:{jobId}`.
- **Fields:** 
  - `status`: PENDING, PROCESSING, COMPLETED, FAILED
  - `totalRows`: Total lines in the CSV (can be pre-calculated or updated as streamed)
  - `processedRows`: Number of lines read so far
  - `successfulRows`: Number of guests successfully inserted
  - `failedRows`: Number of invalid/duplicate rows skipped
- **Performance:** To prevent overwhelming Redis, `ImportProgressTracker` will flush updates in batches (e.g., every 100 processed rows) and once exactly at the end.

## Failure Recovery
- **Worker Crash:** If the Spring Boot JVM dies mid-import, the message remains unacknowledged in RabbitMQ. Upon restart, RabbitMQ will redeliver the message. Idempotency ensures safe re-processing.
- **Retry Exhaustion:** If a system error persists and 3 retries fail, the `RepublishMessageRecoverer` will route the `CsvImportMessage` to `csv_queue.dlq`.
- **DLQ Handling:** Failed jobs in the DLQ can be monitored via the RabbitMQ UI. They will not block new imports. Administrators can replay them once the underlying system issue is fixed.

## Backend Integration Points
To avoid duplicating Member 1's business logic:
- **RedisService:** We must leverage the existing `RedisService` or `RedisTemplate` for the `ImportProgressTracker`.
- **Ticket/User Entities:** If a guest is modeled as a `Ticket` with a specific `TicketStatus` or a `User` entity, we must use the existing JPA repositories and entity models. We should consult the existing domain model before inserting rows directly.
- **Exception Handling:** We should rely on existing logging and potentially integrate with the `GlobalExceptionHandler` for the REST endpoint that initiates the upload.

## Risks
1. **Long-Running DB Transactions:** Annotating the entire `processCsv` method with `@Transactional` will lock tables for minutes during a large import.
2. **Redis Overload:** Updating progress for every single row will cause severe network latency and Redis CPU spikes.

## Recommendations
- **Transaction Scope:** Do NOT use method-level `@Transactional` on the consumer. Apply `@Transactional` only at the chunk-insertion level (e.g., saving a batch of 100 rows).
- **Library:** Use `Apache Commons CSV` or `OpenCSV` for standard-compliant, performant CSV parsing.
- **Progress Batching:** Aggregate progress metrics in memory and sync to Redis periodically (e.g., every 50-100 rows).
