# Phase 3: Asynchronous Processing Architecture Design

## 1. Backend Analysis
**Current State of the Backend:**
- **Messaging:** No messaging abstractions, message brokers, or RabbitMQ dependencies (`spring-boot-starter-amqp`) exist in `pom.xml`.
- **Entities:** The system handles `Order`, `Ticket`, `User`, `TicketCategory`, and `Concert`. However, there are currently no entities for `Artist`, `GuestList`, or file attachments.
- **Integration Points:** 
  - **Email:** Should be triggered at the end of Phase 2's `TicketPurchaseService.finalizeOrderSuccess()` once an order is marked `COMPLETED` and tickets are generated.
  - **CSV & AI:** Currently completely unrepresented in code. These will require new endpoints (e.g., `/api/admin/csv/import` and `/api/admin/artists/{id}/bio/generate`) that simply publish messages and return HTTP 202 Accepted.

## 2. Exchange Design
We will use a multi-exchange topology to separate semantic domain events from direct async commands.

| Exchange Name | Type | Purpose |
|---------------|------|---------|
| `ticketbox.commands` | `direct` | Used for explicit asynchronous tasks (e.g., "send this email", "process this CSV"). Workers bind to specific routing keys. |
| `ticketbox.events` | `topic` | Used for domain events (e.g., `order.completed`). Allows multiple independent consumers to react to the same business event without tight coupling. |
| `ticketbox.dlx` | `direct` | The Dead Letter Exchange. Used to capture poison messages or messages that exhaust all retries. |

## 3. Queue Design

| Queue Name | Bound Exchange | Routing Key | Purpose & Payload |
|------------|----------------|-------------|-------------------|
| `email_queue` | `ticketbox.commands` | `email.send` | **Purpose:** Generates PDF E-tickets and emails them to customers. <br>**Payload:** `{ "orderId": 123, "userId": 456 }` |
| `csv_queue` | `ticketbox.commands` | `csv.import` | **Purpose:** Parses uploaded Guest List CSVs and creates DB records. <br>**Payload:** `{ "fileId": "uuid-1234", "concertId": 789 }` |
| `ai_queue` | `ticketbox.commands` | `ai.generate` | **Purpose:** Calls OpenAI to generate Artist bios. <br>**Payload:** `{ "artistId": 12, "promptParams": "rock band" }` |

*All queues will be configured with `x-dead-letter-exchange: ticketbox.dlx`.*

## 4. Dead Letter Queues (DLQ) Strategy
Messages that fail unrecoverably (e.g., malformed JSON) or exhaust retry limits must not block primary queues. 

- **Naming:** Suffix primary queues with `.dlq` (e.g., `email_queue.dlq`).
- **Routing:** Bound to `ticketbox.dlx` with the same routing keys (`email.send`, `csv.import`, `ai.generate`).
- **Behavior:** 
  - **Email:** Alert DevOps. Customer support may need to manually resend E-tickets.
  - **CSV:** Update the DB import status to `FAILED_UNRECOVERABLE`. Notify the admin via WebSocket/UI notification.
  - **AI:** Update Artist status to `BIO_GENERATION_FAILED`. Allows admin to retry from the UI.

## 5. Retry Strategy
We will implement a hybrid retry strategy to balance responsiveness and system safety.

1. **Immediate Retries (Spring AMQP RetryInterceptor):**
   - For transient network errors (e.g., SMTP timeout, OpenAI rate limits), the consumer thread will pause and retry using Exponential Backoff (Initial: 1s, Multiplier: 2.0, Max: 3 attempts).
2. **DLQ Routing:**
   - If the 3 immediate retries fail, Spring's `RepublishMessageRecoverer` will catch the exception and publish the message directly to `ticketbox.dlx`.
3. **Poison Messages:**
   - Exceptions like `MessageConversionException` (bad JSON) skip retries and go instantly to the DLQ.

## 6. Worker Boundaries

### 1. Email Worker
- **Inputs:** `OrderId`.
- **Dependencies:** `TicketRepository` (to fetch ticket details), `PdfGeneratorService` (to render E-ticket), `JavaMailSender` (to send email).
- **Idempotency:** The worker checks if the `Order` already has an `email_sent = true` flag. If true, skips processing.

### 2. CSV Worker
- **Inputs:** `FileId` or `S3_URI`.
- **Dependencies:** `FileStorageService` (to download CSV), `GuestListRepository` (to save records).
- **Idempotency:** Track import status in a `CsvImportJob` table (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`). Uses optimistic locking to ensure only one worker processes a file.

### 3. AI Worker
- **Inputs:** `ArtistId`.
- **Dependencies:** `OpenAIService`, `ArtistRepository`.
- **Idempotency:** Checks if `Artist.bio` is already populated. Skips if not null, unless the payload explicitly contains `forceUpdate = true`.

## 7. Failure Scenarios & Recovery

| Scenario | Expected Behavior | Recovery Strategy |
|----------|-------------------|-------------------|
| **RabbitMQ Unavailable** | Publisher throws `AmqpException`. | We will implement the **Transactional Outbox Pattern** for Emails. `TicketPurchaseService` will save the event to an `outbox_events` table in the same DB transaction. A scheduler or CDC will publish to RabbitMQ. For CSV/AI, the API returns `503 Service Unavailable` to the admin. |
| **Consumer Crash** | Message remains unacknowledged (Spring `AcknowledgeMode.AUTO` only acks on method completion). | RabbitMQ re-queues the message. Another worker picks it up. Idempotency guarantees safety. |
| **Poison Message** | Deserialization fails. | Instantly routed to DLQ via `RepublishMessageRecoverer`. |
| **OpenAI Timeout** | Worker throws `TimeoutException`. | Trigger Spring Retry (3x). If still failing, route to `ai_queue.dlq`. |
| **Invalid CSV Data** | Worker detects bad formatting. | Considered a business error. Do NOT retry. Mark DB state as `FAILED` with error reason. Acknowledge message (discard). |

## 8. Observability & Monitoring
- **Metrics:** Enable RabbitMQ metrics in Actuator to track `rabbitmq.acknowledged`, `rabbitmq.rejected`, and queue depths.
- **Custom Micrometer Counters:**
  - `ticketbox.async.email.sent`
  - `ticketbox.async.csv.rows.processed`
  - `ticketbox.async.ai.generation.success`
- **Alerting thresholds (Grafana/Prometheus):**
  - Alert if any DLQ depth > 0.
  - Alert if `email_queue` processing latency exceeds 5 minutes.

## 9. Implementation Roadmap
1. **Dependencies:** Add `spring-boot-starter-amqp` to `pom.xml`.
2. **Configuration (`RabbitMQConfig.java`):**
   - Define `ticketbox.commands`, `ticketbox.events`, and `ticketbox.dlx` Exchanges.
   - Define all primary and DLQ queues.
   - Define Bindings.
   - Configure `Jackson2JsonMessageConverter`.
   - Configure `RetryOperationsInterceptor` and `RepublishMessageRecoverer`.
3. **Transactional Outbox:** Create `OutboxEvent` entity and background publisher.
4. **Publishers:** Implement `EmailEventPublisher`, `CsvCommandPublisher`, `AiCommandPublisher`.
5. **Consumers:** Implement `EmailWorker`, `CsvWorker`, `AiWorker`.
6. **Testing:** Write `@RabbitListenerTest` and `Testcontainers` (RabbitMQ) integration tests.
