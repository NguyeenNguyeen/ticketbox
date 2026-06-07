# Phase 3: RabbitMQ Infrastructure Implementation

## 1. Infrastructure Summary
The RabbitMQ messaging infrastructure for Phase 3 (Async Processing) has been fully implemented based on `09_async_architecture_design.md`.

We have successfully integrated Spring AMQP, established the multi-exchange topology, configured all primary and Dead Letter Queues (DLQ), and set up the robust retry mechanism using `RetryOperationsInterceptor` and `RepublishMessageRecoverer`.

This implementation sets the groundwork for the actual async workers (CSV, AI, and Email) without containing any worker-specific business logic.

## 2. Files Created
1. `apps/backend/src/main/java/com/ticketbox/backend/config/RabbitMQConfig.java`
2. `apps/backend/src/main/java/com/ticketbox/backend/dto/async/EmailTaskMessage.java`
3. `apps/backend/src/main/java/com/ticketbox/backend/dto/async/CsvImportMessage.java`
4. `apps/backend/src/main/java/com/ticketbox/backend/dto/async/AiGenerationMessage.java`
5. `apps/backend/src/test/java/com/ticketbox/backend/config/RabbitMQConfigTest.java`

## 3. Files Modified
1. `apps/backend/pom.xml` (Added `spring-boot-starter-amqp`)

## 4. Configuration Changes
The core messaging topology is strictly defined in `RabbitMQConfig.java` to prevent decentralized and duplicated definitions:
- **`Jackson2JsonMessageConverter`**: Configured on the `RabbitTemplate` to ensure JSON serialization of all message DTOs automatically.
- **`SimpleRabbitListenerContainerFactory`**: Configured with a stateful retry interceptor enforcing a strict retry policy (3 attempts, exponential backoff) for all `@RabbitListener` consumers by default.

## 5. Queue Topology

### Exchanges
- `ticketbox.commands` (Direct): Used for explicit commands (AI, CSV, Email).
- `ticketbox.events` (Topic): Used for domain events (e.g., `order.completed`).
- `ticketbox.dlx` (Direct): The Dead Letter Exchange for all unrecoverable messages.

### Primary Queues & Bindings
- `email_queue` -> Bound to `ticketbox.commands` (`email.send`). Configured with DLX routing.
- `csv_queue` -> Bound to `ticketbox.commands` (`csv.import`). Configured with DLX routing.
- `ai_queue` -> Bound to `ticketbox.commands` (`ai.generate`). Configured with DLX routing.

### DLQ Queues & Bindings
- `email_queue.dlq` -> Bound to `ticketbox.dlx` (`email.send`).
- `csv_queue.dlq` -> Bound to `ticketbox.dlx` (`csv.import`).
- `ai_queue.dlq` -> Bound to `ticketbox.dlx` (`ai.generate`).

## 6. Retry Strategy (Message Lifecycle)
1. **Primary Queue**: Message is consumed by a `@RabbitListener`.
2. **Immediate Retry**: If the consumer throws an exception (e.g., SMTP timeout, OpenAI failure), the `RetryInterceptor` catches it. It pauses the consumer thread and retries immediately (up to 3 times, scaling from 1s to 10s wait times).
3. **DLQ Routing**: If all 3 retries fail, or if it's a poison message (`MessageConversionException`), the `RepublishMessageRecoverer` kicks in. It acknowledges the message in the primary queue and manually publishes it to `ticketbox.dlx`, which routes it into the corresponding `.dlq` queue.

## 7. Message Contracts
Three Data Transfer Objects (DTOs) were created to standardize payloads:
- `EmailTaskMessage`: Contains `orderId` and `userId`.
- `CsvImportMessage`: Contains `fileId` and `concertId`.
- `AiGenerationMessage`: Contains `artistId` and `promptParams`.

*All contracts implement `Serializable` and feature a `serialVersionUID` for version stability.*

## 8. Test Plan & Validation
A dedicated Spring Boot integration test (`RabbitMQConfigTest.java`) was written to validate the infrastructure using the standard `@SpringBootTest` context loading.
- **Context Load Test**: Verified that Spring AMQP successfully parses the configuration.
- **Topology Tests**: Verified that the `AmqpAdmin` defines exactly 3 exchanges, 6 queues, and 6 bindings with exact name matches and DLQ arguments.
- **Result**: `BUILD SUCCESS` (All 5 infrastructure tests passing).

## 9. Remaining Risks
- **RabbitMQ Connection During Local Dev**: If a developer starts the application without the RabbitMQ Docker container running, the AMQP connection factory will continuously attempt to connect to `localhost:5672`, filling logs with warnings.
- **Missing Application YAML Properties**: We have not explicitly configured `spring.rabbitmq.host` or `port` in `application.yml`. It correctly defaults to `localhost:5672` (matching Docker), but for production deployment, environmental variables (`SPRING_RABBITMQ_HOST`, etc.) must be passed.
