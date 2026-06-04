# Email E-ticket Worker Design

## Requirements
The Email Worker is responsible for asynchronously sending e-tickets to users upon successful completion of a purchase order. It must ensure that the HTTP purchase request is fully decoupled from email delivery, handling PDF/QR code generation, provider communication (Resend/Brevo), transient fault retries, DLQ routing for permanent failures, and strict idempotency to prevent duplicate ticket emails.

## Architecture Overview
The architecture follows a stream-processing model. A `ticketbox.commands` exchange routes `EmailJobMessage` payloads to `email_queue`. The `EmailConsumer` picks up the message, delegates to `EmailService`, which orchestrates HTML rendering (`EmailTemplateBuilder`), on-the-fly PDF/QR generation (`TicketAttachmentService`), and dispatch via a strategy-based `EmailProviderClient`.

## Message Contract
The `email_queue` message schema (`EmailJobMessage.java`) must be lightweight and contain identifiers rather than bulky payload data:
- `jobId` (String, UUID) - Unique identifier for the async job.
- `orderId` (Long) - Reference to the completed Order.
- `userId` (Long) - Reference to the purchasing User.
- `recipientEmail` (String) - Target email address.
- `ticketIds` (List<Long>) - List of ticket IDs to attach.
- `correlationId` (String) - Trace ID for distributed logging.
- `metadata` (Map<String, String>) - Optional context (e.g., `isResend=true`).

## Component Design
1. **`EmailConsumer`**: Thin RabbitMQ listener. Passes messages to `EmailService`.
2. **`EmailService`**: Core orchestrator. Manages the transaction, idempotency checks, and the overall flow.
3. **`EmailTemplateBuilder`**: Generates HTML email bodies (using Thymeleaf, FreeMarker, or simple string replacement).
4. **`TicketAttachmentService`**: Generates a PDF containing QR codes for each ticket. Operates purely in-memory (`ByteArrayOutputStream`) to avoid disk I/O bottlenecks and cleanup issues.
5. **`EmailProviderClient`**: Interface defining the provider contract.
6. **`EmailJobTracker`**: Redis-backed service for tracking real-time delivery status.

## Provider Strategy
An **Abstraction Strategy Pattern** will be used.
- Interface: `EmailProviderClient` with method `sendEmailWithAttachment(email, subject, body, attachmentBytes)`.
- Implementations: `ResendEmailClient` and `BrevoEmailClient`.
- Both clients will use Spring's native `RestTemplate` (or `WebClient`) to interact with provider REST APIs, avoiding bulky vendor SDKs.
- Selection is dynamically controlled via `@ConditionalOnProperty(name = "ticketbox.email.provider")` in `application.yml`.

## E-ticket Strategy
- **Format:** A single PDF attachment containing all tickets for the order. Each ticket will feature a visually scannable QR Code.
- **Timing:** On-the-fly generation during `EmailService` processing. This avoids storing highly sensitive PDF binaries permanently.
- **Library:** `ZXing` (Zebra Crossing) for QR generation, and `Apache PDFBox` or `iText` for PDF construction.
- **Storage:** No persistent file storage. The PDF bytes are held in memory, base64 encoded for the provider API payload, and then garbage collected.

## Delivery Tracking
Status is tracked both in Redis (for fast async polling) and PostgreSQL (for long-term audit).
**Transitions:**
- `PENDING`: Message sits in RabbitMQ.
- `PROCESSING`: Picked up by consumer.
- `SENT`: Provider accepted the HTTP request (HTTP 200). Order entity updated (`email_status = SENT`).
- `FAILED`: Unrecoverable business error (e.g., malformed email).

## Retry Strategy
Relies on Spring AMQP `RetryInterceptorBuilder`:
- **Transient Failures (Retryable):** HTTP 429 (Rate Limit), HTTP 5xx (Provider Outage), Network Timeouts (`RestClientException`).
- **Backoff:** 3 maximum attempts, exponential backoff (e.g., initial 2s, multiplier 2.0).
- **DLQ Routing:** If 3 attempts fail, the `RepublishMessageRecoverer` securely routes the message to `ticketbox.dlx` -> `email_queue.dlq`.
- **Business Failures (Non-Retryable):** Invalid email format, Order not found. Caught immediately and marked `FAILED`.

## Idempotency
Preventing duplicate emails is critical for customer trust.
1. **Short-Term (Redis):** A Redis distributed lock/key (`email_lock:{orderId}`) with a short TTL prevents concurrent duplicate processing.
2. **Long-Term (DB):** The `Order` entity will have an `emailStatus` column (`PENDING`, `SENT`, `FAILED`). Before sending, `EmailService` verifies the DB state. If `emailStatus == SENT`, it gracefully drops the message and logs a skip.

## Observability
Comprehensive `log.info`, `log.warn`, and `log.error` markers:
- `[START]` Job received, mapping to `orderId` and `correlationId`.
- `[PROCESS]` E-ticket PDF generated successfully.
- `[NETWORK]` Sending HTTP request to {Provider}.
- `[SUCCESS]` Provider responded HTTP 200, DB updated to SENT.
- `[ERROR]` Exception stack trace for failures.
- `[DLQ]` Warning log when message exhausts retries and is recovered to DLQ.

## Security
- **Attachment Exposure:** PII is protected because PDFs are generated entirely in RAM and never written to the `/tmp/` filesystem.
- **Email Masking:** Application logs will mask emails (e.g., `j***@gmail.com`) to comply with data privacy standards.
- **Credentials:** Provider API keys are strictly injected via `.env` and `application.yml` placeholders, never hardcoded.

## Risks
- **High Memory Pressure:** Generating PDFs in-memory for massive orders (e.g., 50 tickets in one order) could spike heap usage.
- **Provider Timeouts:** Heavy base64 payload attachments might cause slow API uploads, triggering false timeouts.
- **QR Code Quality:** Ensure `ZXing` generates high-resolution QR codes to prevent scanning issues at the venue gates.

## Recommendations
- Limit the maximum number of tickets per order (e.g., 10 max) at the checkout phase to bound the in-memory PDF size.
- Set generous HTTP connection timeouts (e.g., 15 seconds) specifically for the Email `RestTemplate` to accommodate base64 attachment uploads.
- Ensure the `email_queue` prefetch count is explicitly constrained to avoid pulling thousands of heavy PDF-generation jobs into RAM simultaneously.

## Final Assessment
The design leverages the existing proven asynchronous architecture (RabbitMQ + AMQP Retries + DLQ). By implementing on-the-fly, in-memory PDF generation, we bypass major security and storage lifecycle headaches. The strategy pattern ensures provider flexibility, and the dual-layer idempotency (Redis + DB) guarantees reliable, exactly-once delivery.

READY FOR EMAIL WORKER IMPLEMENTATION
