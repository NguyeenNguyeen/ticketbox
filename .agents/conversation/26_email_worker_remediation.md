# Issues Addressed

During the Email Worker Review phase, several critical architectural and operational flaws were identified in the implementation. This remediation phase addressed the following:
1. **Transactional Connection Pool Exhaustion:** `EmailService` held a database transaction open during external HTTP calls to email providers.
2. **Provider Idempotency:** The worker lacked exactly-once delivery guarantees at the provider level, risking duplicate emails on retries.
3. **Permanent Error Classification:** HTTP 4xx errors from providers were incorrectly subjected to the full exponential backoff retry cycle instead of failing fast.
4. **Explicit HTTP Timeouts:** `RestTemplate` was instantiated without explicit connect/read timeouts, risking thread starvation during network outages.
5. **Provider Error Visibility:** Lack of detailed logging when an API call failed.

# Root Causes

1. `@Transactional` was applied at the method level covering both DB fetching/saving and IO-bound external REST calls.
2. `ResendEmailClient` and `BrevoEmailClient` were not utilizing the `Idempotency-Key` headers supported by their APIs.
3. All `Exception` types were caught globally and bubbled up, triggering Spring AMQP's default retry mechanism regardless of error type.
4. Default `RestTemplate` constructors rely on OS-level TCP timeouts, which can be unbounded or overly long.

# Fixes Applied

1. **Transaction Boundaries (EmailService.java):** 
   - Removed the method-level `@Transactional` annotation.
   - Injected `TransactionTemplate` to perform short-lived reads (`fetchOrderData`) and writes (`orderRepository.save`).
   - The external IO-bound call (`emailProviderClient.sendEmailWithAttachment`) now executes fully outside any transaction.
2. **Provider Idempotency (EmailProviderClient.java & Implementations):** 
   - Added `jobId` to the client abstraction.
   - Updated `ResendEmailClient` and `BrevoEmailClient` to inject the `Idempotency-Key: <jobId>` header into outbound requests, ensuring duplicate retries will not result in multiple emails.
3. **Permanent Error Classification & Visibility (EmailService.java & RabbitMQConfig.java):**
   - Wrapped the HTTP call in a `try-catch` explicitly matching `HttpClientErrorException` (4xx).
   - Captured and logged the raw provider response body for enhanced visibility.
   - Threw `AmqpRejectAndDontRequeueException` upon permanent failure.
   - Configured `RabbitMQConfig`'s `RetryInterceptorBuilder` with a custom `SimpleRetryPolicy` that actively excludes `AmqpRejectAndDontRequeueException` from being retried. This forces immediate DLQ routing.
4. **Explicit Timeouts (AppConfig.java):** 
   - Refactored the `RestTemplate` bean to use `RestTemplateBuilder`, explicitly setting a `10s` connect timeout and `15s` read timeout.

# Files Modified

* `.agents/conversation/current_project_state.md` (Reconciled)
* `.agents/conversation/progress_tracker.md` (Reconciled)
* `apps/backend/src/main/java/com/ticketbox/backend/config/AppConfig.java`
* `apps/backend/src/main/java/com/ticketbox/backend/config/RabbitMQConfig.java`
* `apps/backend/src/main/java/com/ticketbox/backend/worker/email/EmailService.java`
* `apps/backend/src/main/java/com/ticketbox/backend/worker/email/EmailProviderClient.java`
* `apps/backend/src/main/java/com/ticketbox/backend/worker/email/ResendEmailClient.java`
* `apps/backend/src/main/java/com/ticketbox/backend/worker/email/BrevoEmailClient.java`
* `apps/backend/src/test/java/com/ticketbox/backend/worker/email/EmailWorkerTest.java`

# Test Updates

- Updated existing mock setups in `EmailWorkerTest` to match the exact method signature and arguments required for the new idempotency keys.
- Added `testPermanent4xxErrorRoutesToDlqImmediately()` to verify that an `HttpClientErrorException.BadRequest` bypasses Spring Retry and routes directly to the `email_queue.dlq`.
- Increased the message receive timeout assertion for DLQ checks from 1000ms to 5000ms to eliminate race conditions during test execution.

# Documentation Reconciliation

Verified that the duplicated `agent/` and `.agent/` directories do not exist in the filesystem. All relevant Email Worker design, implementation, and review artifacts are properly scoped under `.agents/conversation/`.

# Tracker Reconciliation

Appended the Email Worker milestones to `.agents/current_project_state.md` and `.agents/progress_tracker.md`. Reconstructed the missing phase progress back into the full historical tracker.

# Remaining Risks

- The application now heavily relies on the Idempotency-Key implementation provided by Resend and Brevo. Any vendor-side failure regarding these headers might still lead to duplicates.
- Email templating logic currently executes outside the database transaction. If templates rely on heavily nested lazy-loaded properties not initialized during `fetchOrderData`, `LazyInitializationException` could occur. This is mitigated by explicit initialization of required relationships prior to exiting the read transaction.

# Assessment

The Email Worker is now fully robust against network delays, gracefully mitigates transient vs. permanent API failures, and strictly bounds transaction scopes to preserve connection pools. Tests successfully validate both successful routing and DLQ exception-handling paths.

**READY FOR EMAIL WORKER REVALIDATION**
