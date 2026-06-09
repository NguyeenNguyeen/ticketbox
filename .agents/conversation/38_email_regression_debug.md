# TicketBox Email Delivery — Critical Regression Debug

## 1. Execution Trace (PASS / FAIL per step)
- **Payment Success**: PASS
- **TicketPurchaseService (`finalizeOrderSuccess`)**: PASS
- **EmailTaskMessage creation**: PASS
- **RabbitTemplate.convertAndSend()**: PASS
- **RabbitMQ Exchange (`ticketbox.direct`)**: PASS
- **Queue (`ticketbox.email.queue`)**: PASS
- **EmailConsumer (`receiveEmailJob`)**: PASS (Picks up the message)
- **EmailService (`processEmailJob`)**: PASS (Acquires Redis lock, fetches data, generates PDF and HTML)
- **EmailProvider (ResendEmailClient)**: PASS (Builds the payload with base64 attachment)
- **HTTP API call to Resend**: **FAIL** (Returns HTTP 403 Forbidden)
- **DB Update (`emailStatus`)**: PASS (Transitions to `FAILED` correctly based on the 4xx exception)
- **RabbitMQ DLQ Routing**: PASS (Throws `AmqpRejectAndDontRequeueException` and routes to DLQ)

## 2. Environment Variable Status
**OS Environment Variable Injection via `set -a && source .env`:**
- `RESEND_API_KEY`: **Present** (`re_dx8guLfL_HQ8QyMAVjvv2izU2M1NRGiYZ`)
- `EMAIL_FROM`: **Present** (`tickets@ticketbox.vn`)
- `RABBITMQ_DEFAULT_USER`: **Present** (`ticketbox`)

**Spring Boot Mapping (`@Value` / `application.yml`):**
- `ticketbox.email.provider`: Defaults to `resend` (Valid).
- `ticketbox.email.from`: Resolves to `tickets@ticketbox.vn` (overriding the `onboarding@resend.dev` default).
- `ticketbox.email.resend.key`: Resolves to the correct API key.

## 3. Spring Configuration Issues
There are no missing beans or configuration resolution errors.
- `@ConditionalOnProperty` correctly instantiates `ResendEmailClient`.
- `@Value` annotations are correctly hydrated by the standard OS environment variables.
- The `PropertyChecker` and JVM inspection verified that the properties are perfectly synchronized with the `.env` file.

## 4. RabbitMQ Status
- **Exchange & Queue**: `ticketbox.direct` and `ticketbox.email.queue` exist and are bound correctly.
- **Producer**: Successfully publishes messages.
- **Consumer**: Active and successfully picks up messages.
- **Message Fate**: Messages are **rejected and routed to DLQ** due to the 403 API Exception thrown in the consumer layer.

## 5. Ranked Root Cause Hypotheses
1. **Resend Sandbox Sender Domain Restriction** (Highest Probability): The system successfully injects `EMAIL_FROM=tickets@ticketbox.vn`, but Resend Sandbox strictly requires `onboarding@resend.dev`. The API rejects the payload.
2. **Missing/Empty API Key**: Ruled out. The JVM explicitly logged the presence of the `RESEND_API_KEY`.
3. **spring-dotenv Migration Bug**: Ruled out. The OS injection works perfectly. In fact, it works *better* than before, successfully injecting the production email address which triggered the underlying sandbox limitation.

## 6. Final Root Cause + Confidence %
**Root Cause**: Successful Configuration Overriding violating Resend Sandbox Policy. 
Because the `.env` file contains `EMAIL_FROM=tickets@ticketbox.vn`, the new OS environment variable injection flawlessly overrides the safe default (`onboarding@resend.dev`). The `ResendEmailClient` correctly executes the POST request using `"from": "tickets@ticketbox.vn"`. Because the application is running against a Sandbox Resend account, the API immediately rejects the unverified domain with an **HTTP 403 Forbidden**. `EmailService` catches the 4xx error, marks the order as `FAILED`, and sends the message to the DLQ.

**Confidence**: 100%. Simulated the exact payload via curl using the injected API Key and `"from": "tickets@ticketbox.vn"`, which perfectly reproduced the `403 Forbidden: The ticketbox.vn domain is not verified` error.
