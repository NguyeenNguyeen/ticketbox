# Email Worker Implementation

## Objective
Implement the Email E-ticket Worker as per the design in `23_email_worker_design.md`. This worker handles order confirmation emails, dynamically generates PDF e-tickets with QR codes in-memory, and provides a provider abstraction layer for email delivery.

## Decisions Made
- **Idempotency**: Utilized a short-term Redis distributed lock to prevent concurrent processing of the same job, coupled with a long-term database status check (`emailStatus` on the `Order` entity).
- **In-Memory E-Ticket Generation**: Configured `TicketAttachmentService` using Apache PDFBox and ZXing to generate PDFs with QR codes purely in memory. This avoids persisting temporary PII files on disk.
- **Provider Abstraction**: Implemented `EmailProviderClient` interface with `ResendEmailClient` and `BrevoEmailClient` (via `@ConditionalOnProperty`). Default provider set to `resend`.
- **Job Tracking**: Leveraged `EmailJobTracker` backed by Redis for precise job progression and failure reason visibility.
- **Retry Mechanism & DLQ**: Allowed transient external provider exceptions (like timeouts or API errors) to propagate up to `EmailConsumer`, trusting Spring AMQP's `SimpleMessageListenerContainer` to manage backoff and DLQ routing.
- **Test Isolation**: Improved test hygiene globally across the worker test suites by introducing a dedicated DLQ test queue in `RabbitMQValidationTest` and adding strictly ordered `ticketCategoryRepository.deleteAll()` cleanup hooks across all test suites to prevent `DataIntegrityViolation` cascading failures.

## Files Created
- `com.ticketbox.backend.worker.email.EmailProviderClient` (Interface)
- `com.ticketbox.backend.worker.email.ResendEmailClient`
- `com.ticketbox.backend.worker.email.BrevoEmailClient`
- `com.ticketbox.backend.worker.email.TicketAttachmentService`
- `com.ticketbox.backend.worker.email.EmailTemplateBuilder`
- `com.ticketbox.backend.worker.email.EmailJobTracker`
- `com.ticketbox.backend.worker.email.EmailService`
- `com.ticketbox.backend.worker.email.EmailConsumer`
- `com.ticketbox.backend.worker.email.EmailWorkerTest`

## Files Modified
- `pom.xml`: Added ZXing dependencies.
- `application.yml`: Added email provider configurations (`ticketbox.email.*`).
- `Order.java`: Added `emailStatus` tracking field.
- `EmailTaskMessage.java`: Added comprehensive job parameters (`jobId`, `recipientEmail`, `ticketIds`, etc.).
- `RabbitMQValidationTest.java`: Refactored to test DLQ logic dynamically on a separate test queue, preventing race conditions with real consumer beans.
- `AiWorkerTest.java` / `CsvWorkerTest.java`: Patched database teardown hooks for newly introduced relationship constraints.

## Dependencies
- ZXing (for QR codes).
- PDFBox (for PDF generation).
- Spring Data Redis (for idempotency locks and job tracking).

## Remaining Work
- The individual backend infrastructure layers are successfully implemented. The next logical stage is to integrate these pieces into the primary user flows via a GraphQL/REST unified API layer or move to frontend consumption.
- End-to-end load testing on the email worker in a real cloud environment (when deployed).

## Open Questions
- None.
