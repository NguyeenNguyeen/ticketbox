# 05 — Payment Protection Design

**Task:** Design the Payment Protection module for TicketBox  
**Author:** Member 4 (Infrastructure & Platform Protection)  
**Date:** 2026-06-04  
**Status:** ✅ Design Complete — Awaiting Implementation Approval

---

## 1. Current Backend Analysis

### 1.1 Purchase Flow (Current State)

The entire ticket purchase is handled by `TicketPurchaseService.purchaseTicket()` in a **single `@Transactional` method**. The current flow:

```
Customer → POST /api/tickets/purchase
    │
    ▼ 1. Idempotency check (Redis SETNX)
    │
    ▼ 2. Distributed lock (Redis per user+category)
    │
    ▼ 3. Pessimistic lock (PostgreSQL SELECT FOR UPDATE)
    │
    ▼ 4. Deduct available quantity
    │
    ▼ 5. Calculate price (Strategy Pattern)
    │
    ▼ 6. Create Order (PENDING)
    │
    ▼ 7. processPayment() → State: PAYING   ← SIMULATED (no external call)
    │
    ▼ 8. completeOrder() → State: COMPLETED ← SIMULATED (immediate)
    │
    ▼ 9. Create OrderItem + Tickets
    │
    ▼ 10. Release distributed lock
```

**Key observations:**

- **No external payment gateway exists.** Steps 7–8 currently call `orderStateContext.processPayment()` and `orderStateContext.completeOrder()` back-to-back within the same transaction. The comment on line 92 says: *"Move order state to Completed for simplicity in this flow, normally would be Paying → wait for gateway → Completed"*.
- **The State Pattern is already wired.** `OrderState` interface defines `processPayment()`, `cancelOrder()`, `completeOrder()`. All four state classes (`PendingState`, `PayingState`, `CompletedState`, `CancelledState`) exist and enforce valid transitions.
- **The Order entity** supports `PENDING`, `PAYING`, `COMPLETED`, `CANCELLED` via `OrderStatus` enum.
- **The entire flow is in one transaction.** If the payment call were to be made external and slow, the DB transaction would hold a pessimistic lock for the entire duration — a scalability and deadlock risk.

### 1.2 Existing Exception Handling

- `TicketController.purchaseTicket()` has a raw `try/catch` around the service call. `IllegalStateException` → 400, everything else → 500.
- `GlobalExceptionHandler` handles `IllegalArgumentException`, `IllegalStateException`, `AccessDeniedException`, `AuthenticationException`, `MethodArgumentNotValidException`, and a catch-all `Exception`.
- No payment-specific exception classes exist.

### 1.3 Existing Redis Usage

`RedisService` provides:
- `acquireLock(key, timeout)` — Distributed lock (SETNX)
- `releaseLock(key)` — Lock release (DELETE)
- `setIfAbsentIdempotencyKey(key, timeout)` — Idempotency guard

These are essential to the purchase flow and must remain operational. Circuit Breaker integration must not interfere with Redis lock management.

### 1.4 State Pattern Analysis

| Current State | `processPayment()` | `cancelOrder()` | `completeOrder()` |
|---------------|--------------------|-----------------|--------------------|
| `PENDING`     | → `PAYING` ✅      | → `CANCELLED` ✅ | ❌ throws           |
| `PAYING`      | ❌ throws           | → `CANCELLED` ✅ | → `COMPLETED` ✅   |
| `COMPLETED`   | ❌ throws           | ❌ throws        | ❌ throws           |
| `CANCELLED`   | ❌ throws           | ❌ throws        | ❌ throws           |

The state machine already supports the transitions needed for Circuit Breaker integration:
- Payment failure → `PAYING` can transition to `CANCELLED`
- Payment success → `PAYING` can transition to `COMPLETED`
- Circuit open → order remains `PAYING` (reservation held temporarily)

---

## 2. Architecture: Where Circuit Breaker Integrates

### 2.1 Integration Point

The Circuit Breaker wraps a **new `PaymentGatewayService`** — an abstraction layer between `TicketPurchaseService` and the external payment provider.

```
TicketPurchaseService
    │
    ▼ Creates order (PENDING → PAYING)
    │
    ▼ Calls PaymentGatewayService.processPayment(order)
    │       ┌────────────────────────────────────┐
    │       │  Resilience4j CircuitBreaker        │
    │       │  Resilience4j Bulkhead              │
    │       │       │                             │
    │       │       ▼                             │
    │       │  External Payment API (HTTP)        │
    │       │       │                             │
    │       │       ▼                             │
    │       │  PaymentResult (SUCCESS / FAILURE)  │
    │       └────────────────────────────────────┘
    │
    ▼ On success: completeOrder() → COMPLETED
    ▼ On failure: cancelOrder() → CANCELLED (restore tickets)
    ▼ On circuit-open: PAYMENT_MAINTENANCE fallback → PAYING (hold reservation)
```

### 2.2 Transaction Boundary Refactoring

**Critical architectural decision:** The current single `@Transactional` block must be split.

**Reason:** Holding a pessimistic database lock while waiting for an external HTTP call (potentially 3–5 seconds, or timing out at 10 seconds) will cause:
- Database connection pool exhaustion under load
- Deadlocks when concurrent users target the same ticket category
- Transaction timeout exceptions

**Proposed split:**

| Phase | Scope | Transaction |
|-------|-------|-------------|
| Phase A: Reserve | Idempotency → Lock → Deduct quantity → Create Order (PENDING → PAYING) → Save | `@Transactional` #1 |
| Phase B: Pay | Call `PaymentGatewayService.processPayment()` via Circuit Breaker + Bulkhead | **No transaction** (external I/O) |
| Phase C: Finalize | On success: PAYING → COMPLETED, create tickets. On failure: PAYING → CANCELLED, restore quantity. | `@Transactional` #2 |

This ensures the pessimistic lock is held only for the microseconds needed to deduct quantity, not during external I/O.

> **Member 1 collaboration note:** This refactoring modifies `TicketPurchaseService`, which is Member 1's code. Member 4 should propose the interface change and coordinate. The refactoring is mandatory for correctness — without it, Circuit Breaker integration is unsafe.

---

## 3. Circuit Breaker Design

### 3.1 Technology

**Resilience4j** — approved in `technology_summary.md` (line 210). Lightweight, composable, no runtime agent.

### 3.2 Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        # --- Sliding Window ---
        sliding-window-type: COUNT_BASED          # Per technology_summary.md requirement
        sliding-window-size: 10                    # Evaluate last 10 calls
        minimum-number-of-calls: 5                 # Need at least 5 calls before opening

        # --- Failure Thresholds ---
        failure-rate-threshold: 50                 # Open circuit when ≥50% of calls fail
        slow-call-rate-threshold: 80               # Open circuit when ≥80% of calls are slow
        slow-call-duration-threshold: 3s           # A call taking >3s is "slow"

        # --- State Transitions ---
        wait-duration-in-open-state: 30s           # Stay open 30s before probing half-open
        permitted-number-of-calls-in-half-open-state: 3  # Probe with 3 test calls
        automatic-transition-from-open-to-half-open-enabled: true

        # --- Recording ---
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignore-exceptions:
          - com.ticketbox.backend.exception.PaymentDeclinedException  # Business logic, not infra failure
```

### 3.3 Rationale for Each Setting

| Setting | Value | Justification |
|---------|-------|---------------|
| `sliding-window-type` | `COUNT_BASED` | Required by technology_summary.md. Predictable behavior regardless of traffic volume. |
| `sliding-window-size` | `10` | TicketBox is a medium-traffic app. 10 calls provides a responsive signal without being overly jittery. |
| `minimum-number-of-calls` | `5` | Prevents premature circuit opening during low-traffic periods (e.g., early morning). |
| `failure-rate-threshold` | `50%` | Standard threshold — if half the payment calls fail, the provider is likely down. |
| `slow-call-rate-threshold` | `80%` | If 80% of calls exceed 3 seconds, the provider is degraded. Protecting users from hanging requests is more important than strict uptime. |
| `slow-call-duration-threshold` | `3s` | Payment APIs should respond within 1–2s. 3s is generous. Beyond that, user experience degrades significantly. |
| `wait-duration-in-open-state` | `30s` | 30 seconds balances between user impact (waiting for recovery) and provider recovery time. Payment providers typically recover within 1–5 minutes. |
| `permitted-number-of-calls-in-half-open-state` | `3` | 3 probe calls provide statistical confidence without flooding a recovering provider. |
| `record-exceptions` | I/O + timeout + server errors | These indicate infrastructure failures (the provider is down or unreachable). |
| `ignore-exceptions` | `PaymentDeclinedException` | A declined payment (insufficient funds, expired card) is business logic, not an infrastructure failure. It should not contribute to circuit opening. |

### 3.4 State Machine

```
                    failure-rate ≥ 50%
                    OR slow-call-rate ≥ 80%
        CLOSED ─────────────────────────────────► OPEN
          ▲                                         │
          │                                         │ wait 30s
          │ failure-rate < 50%                      │
          │ AND slow-call-rate < 80%                ▼
          └──────────────────────────────────── HALF_OPEN
                3 probe calls succeed                 │
                                                      │ probe calls fail
                                                      ▼
                                                    OPEN (re-open)
```

---

## 4. Bulkhead Design

### 4.1 Bulkhead Type

**Semaphore-based Bulkhead** (not thread-pool based).

**Rationale:**
- Thread-pool Bulkhead creates a dedicated thread pool and requires `CompletableFuture`-based return types. This introduces reactive complexity into an otherwise synchronous Spring MVC codebase.
- Semaphore Bulkhead simply limits concurrent access with a semaphore on the calling thread. It integrates naturally with `@Transactional` and Spring Security's `SecurityContextHolder` (which is thread-local).
- TicketBox runs on Tomcat (thread-per-request). A semaphore on the calling Tomcat thread is the most natural fit.

### 4.2 Configuration

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentGateway:
        max-concurrent-calls: 10          # Max 10 concurrent payment calls
        max-wait-duration: 0ms            # Fail immediately when full (no queueing)
```

### 4.3 Rationale

| Setting | Value | Justification |
|---------|-------|---------------|
| `max-concurrent-calls` | `10` | TicketBox is a university project; expected traffic is 100–1,000 concurrent users. 10 parallel payment calls leaves 190+ Tomcat threads free for browsing, auth, and other endpoints (default Tomcat pool = 200). |
| `max-wait-duration` | `0ms` | Fail immediately. If 10 payment calls are already in-flight, queueing more just increases latency and user frustration. Better to return `PAYMENT_MAINTENANCE` immediately. |

### 4.4 Thread Budget Analysis

| Resource | Budget |
|----------|--------|
| Tomcat thread pool (default) | 200 threads |
| Bulkhead reservation for payment | 10 threads (5%) |
| Available for all other endpoints | 190 threads (95%) |

**Worst case:** If the payment provider hangs for 10 seconds and 10 users hit payment simultaneously, those 10 threads are blocked. All other users (browsing concerts, checking tickets, logging in) are completely unaffected. Without the Bulkhead, all 200 threads could be consumed by hanging payment calls, causing a total site outage.

---

## 5. Graceful Degradation Design

### 5.1 Fallback Strategy

When either the **Circuit Breaker is OPEN** or the **Bulkhead rejects** a call, the system must not crash. Instead:

```json
{
  "status": 200,
  "code": "PAYMENT_MAINTENANCE",
  "message": "Hệ thống thanh toán tạm thời gián đoạn. Đơn hàng của bạn đã được giữ. Vui lòng thử lại sau.",
  "orderId": 12345,
  "retryAfterSeconds": 30
}
```

> **HTTP 200 with a business code** is intentional — per `technology_summary.md` line 225: *"Trả về mã lỗi nghiệp vụ để UI hiển thị"*. This is not an HTTP error; it's a business state the frontend displays as a maintenance message.

### 5.2 Order Behavior During Degradation

| Aspect | Behavior | Rationale |
|--------|----------|-----------|
| Order state | Remains `PAYING` | Per `technology_summary.md` line 233: *"Giữ nguyên trạng thái RESERVED của đơn hàng"*. The `PAYING` state is the reservation state — tickets are deducted but not yet confirmed. |
| Ticket quantity | Already deducted (Phase A committed) | The customer's tickets are reserved. No one else can buy them. |
| Reservation TTL | 10 minutes (same as idempotency key TTL) | After 10 minutes without payment completion, a **scheduled cleanup job** transitions the order to `CANCELLED` and restores the deducted quantity. |
| User action | Frontend shows retry button with countdown | The user can retry payment manually when the circuit closes. |

### 5.3 Reservation Expiry (Cleanup Job)

A `@Scheduled` task must periodically scan for orders in `PAYING` state that have exceeded the reservation window:

```
Every 60 seconds:
  SELECT * FROM orders 
  WHERE status = 'PAYING' 
  AND created_at < NOW() - INTERVAL '10 minutes'
  
  For each stale order:
    1. Transition → CANCELLED
    2. Restore ticket category available_quantity
    3. Log: "Order {} expired due to payment reservation timeout"
```

This prevents inventory from being permanently locked when a user abandons a payment or the circuit breaker stays open for an extended period.

---

## 6. Order State Transitions Under Failure

### 6.1 Failure Scenario Matrix

| Scenario | Circuit State | Order State Transition | Ticket Inventory | User Response |
|----------|--------------|----------------------|-------------------|---------------|
| **Payment success** | CLOSED | PAYING → COMPLETED | Confirmed (deducted permanently) | `200 OK` + order details |
| **Payment declined** (insufficient funds) | CLOSED (not recorded) | PAYING → CANCELLED | Restored | `400 BAD_REQUEST` + "Payment declined" |
| **Payment timeout** (>3s) | CLOSED → (counts as slow) | PAYING → CANCELLED | Restored | `408` or `500` + "Payment timed out" |
| **Provider HTTP 500** | CLOSED → (counts as failure) | PAYING → CANCELLED | Restored | `502 BAD_GATEWAY` + "Payment provider error" |
| **Provider HTTP 429** | CLOSED → (counts as failure) | PAYING → CANCELLED | Restored | `502 BAD_GATEWAY` + "Payment provider busy" |
| **Provider unreachable** | CLOSED → (counts as failure) | PAYING → CANCELLED | Restored | `502 BAD_GATEWAY` + "Payment provider unavailable" |
| **Circuit OPEN** | OPEN | Remains PAYING (held) | Reserved (10 min TTL) | `200` + `PAYMENT_MAINTENANCE` |
| **Bulkhead full** | Any | Remains PAYING (held) | Reserved (10 min TTL) | `200` + `PAYMENT_MAINTENANCE` |
| **Circuit HALF_OPEN + probe succeeds** | HALF_OPEN → CLOSED | PAYING → COMPLETED | Confirmed | `200 OK` + order details |
| **Circuit HALF_OPEN + probe fails** | HALF_OPEN → OPEN | Remains PAYING (held) | Reserved (10 min TTL) | `200` + `PAYMENT_MAINTENANCE` |

### 6.2 State Diagram

```
                            purchaseTicket()
                                  │
                                  ▼
                              ┌────────┐
                              │PENDING │
                              └───┬────┘
                                  │ processPayment()
                                  ▼
                              ┌────────┐
                       ┌──────│ PAYING │──────┐
                       │      └───┬────┘      │
                       │          │           │
              Circuit OPEN    Payment     Payment
              OR Bulkhead     Success     Failure/Timeout
              Full            │           │
                       │      │           │
                       ▼      ▼           ▼
                   (hold)  ┌──────────┐ ┌──────────┐
                   10 min  │COMPLETED │ │CANCELLED │
                   TTL     └──────────┘ └──────────┘
                       │
                       ▼
            Expiry cleanup → CANCELLED (restore qty)
```

---

## 7. Observability & Monitoring

### 7.1 Metrics to Collect

Resilience4j auto-publishes metrics to Micrometer (which Spring Boot Actuator exposes). The following metrics are critical:

| Metric | Purpose |
|--------|---------|
| `resilience4j.circuitbreaker.state{name=paymentGateway}` | Current circuit state (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |
| `resilience4j.circuitbreaker.calls{name=paymentGateway,kind=successful}` | Count of successful payment calls |
| `resilience4j.circuitbreaker.calls{name=paymentGateway,kind=failed}` | Count of failed payment calls |
| `resilience4j.circuitbreaker.failure.rate{name=paymentGateway}` | Current failure rate percentage |
| `resilience4j.circuitbreaker.slow.call.rate{name=paymentGateway}` | Current slow call rate percentage |
| `resilience4j.bulkhead.available.concurrent.calls{name=paymentGateway}` | Remaining semaphore permits |
| Custom: `ticketbox.payment.fallback.invocations` | Count of PAYMENT_MAINTENANCE fallbacks triggered |
| Custom: `ticketbox.payment.reservation.expired` | Count of orders auto-cancelled by cleanup job |

### 7.2 Logging Strategy

| Event | Level | Content |
|-------|-------|---------|
| Payment call initiated | `INFO` | Order ID, amount, user |
| Payment call succeeded | `INFO` | Order ID, provider transaction ID, latency |
| Payment call failed (exception) | `WARN` | Order ID, exception class, message |
| Circuit state changed | `WARN` | Old state → New state, failure rate |
| Bulkhead rejected | `WARN` | Order ID, current concurrent count |
| Fallback invoked | `WARN` | Order ID, reason (circuit-open / bulkhead-full) |
| Reservation expired (cleanup) | `INFO` | Order ID, age in minutes |

### 7.3 Health Indicator

Register a custom `HealthIndicator` that reports:
- Circuit breaker state
- Failure rate
- Bulkhead available permits

This allows `/actuator/health` to include payment provider status, enabling Docker healthchecks and monitoring dashboards to detect degradation proactively.

---

## 8. Risk Assessment

### 8.1 Business Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Tickets reserved but never paid (inventory leak) | **HIGH** | 10-minute TTL + scheduled cleanup job restores quantity |
| Circuit stays open for extended period → all payments fail | **MEDIUM** | `automatic-transition-from-open-to-half-open-enabled=true` + 30s wait duration ensures probing resumes automatically |
| False-positive circuit opening (flaky single call) | **LOW** | `minimum-number-of-calls=5` prevents premature opening |
| User pays via another channel but circuit is open | **LOW** | Idempotency key prevents double-charging if user retries |

### 8.2 Data Consistency Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Phase A commits but Phase B/C crashes → orphaned `PAYING` order | **HIGH** | Cleanup job auto-cancels after 10 min. Also, idempotency key prevents re-reservation. |
| Payment provider processes charge but our system doesn't receive confirmation (network split) | **HIGH** | This is a distributed systems problem beyond Circuit Breaker scope. Requires a payment reconciliation job (future enhancement). Member 4 notes this as a known limitation. |
| Concurrent ticket quantity restoration races | **MEDIUM** | Restoration must use pessimistic locking or atomic `UPDATE ... SET qty = qty + N WHERE id = ?` |

### 8.3 User Experience Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| User sees `PAYMENT_MAINTENANCE` during peak ticket sale | **HIGH** | Bulkhead of 10 concurrent calls is generous. Circuit opens only at 50% failure rate. This should only trigger when the provider is genuinely unhealthy. |
| User doesn't understand retry message | **LOW** | Frontend team (Member 2) renders clear Vietnamese-language maintenance banner with countdown timer |

### 8.4 Operational Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Resilience4j misconfiguration causes permanent circuit open | **MEDIUM** | Extensive unit testing of all state transitions + actuator endpoint for manual circuit reset |
| Scheduled cleanup job fails → permanent inventory leak | **HIGH** | Monitor `ticketbox.payment.reservation.expired` metric. Alert if zero for >15 minutes during active hours. |

---

## 9. Implementation Roadmap

### 9.1 New Dependencies (`pom.xml`)

| Dependency | Purpose |
|------------|---------|
| `io.github.resilience4j:resilience4j-spring-boot3` | Auto-configuration for Circuit Breaker + Bulkhead + metrics integration |
| `org.springframework.boot:spring-boot-starter-aop` | Required for Resilience4j annotation-based decorators (`@CircuitBreaker`, `@Bulkhead`) |
| `org.springframework.boot:spring-boot-starter-actuator` | Metrics export for Resilience4j health indicators (may already be present or planned) |

### 9.2 New Classes to Create

| Class | Package | Purpose |
|-------|---------|---------|
| `PaymentGatewayService` | `service` | Interface defining `processPayment(Order): PaymentResult` |
| `MockPaymentGatewayService` | `service.impl` | Simulated external payment (for development/testing) |
| `PaymentResult` | `dto` | Encapsulates payment outcome: `SUCCESS`, `DECLINED`, `ERROR` + provider transaction ID |
| `PaymentProtectionConfig` | `config` | Resilience4j bean customization + event listeners for state change logging |
| `PaymentFallbackHandler` | `service` | Produces `PAYMENT_MAINTENANCE` response, holds reservation |
| `PaymentDeclinedException` | `exception` | Business exception (ignored by Circuit Breaker) |
| `PaymentGatewayException` | `exception` | Infrastructure exception (recorded by Circuit Breaker) |
| `PaymentHealthIndicator` | `config` | Custom `/actuator/health` contributor for payment circuit state |
| `StaleOrderCleanupJob` | `scheduler` | `@Scheduled` task that cancels expired `PAYING` orders + restores ticket qty |

### 9.3 Classes to Modify

| Class | Modification |
|-------|-------------|
| `TicketPurchaseService` | Split `@Transactional` into Phase A (reserve) and Phase C (finalize). Insert `PaymentGatewayService` call between them. **Requires Member 1 coordination.** |
| `TicketController` | Handle `PAYMENT_MAINTENANCE` response from fallback. Map to clean HTTP 200 JSON body. |
| `GlobalExceptionHandler` | Add `PaymentGatewayException` and `PaymentDeclinedException` handlers |
| `application.yml` | Add Resilience4j configuration block |
| `OrderRepository` | Add `findByStatusAndCreatedAtBefore(OrderStatus, LocalDateTime)` for cleanup job |
| `pom.xml` | Add Resilience4j + AOP + Actuator dependencies |

### 9.4 Configuration to Add (`application.yml`)

```yaml
# --- Resilience4j: Payment Protection ---
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        slow-call-rate-threshold: 80
        slow-call-duration-threshold: 3s
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
  bulkhead:
    instances:
      paymentGateway:
        max-concurrent-calls: 10
        max-wait-duration: 0ms

# --- Payment settings ---
ticketbox:
  payment:
    reservation-ttl-minutes: 10
    cleanup-interval-seconds: 60
```

### 9.5 Testing Requirements

| Test Category | What to Test |
|---------------|-------------|
| **Unit: Circuit Breaker states** | Verify CLOSED → OPEN → HALF_OPEN → CLOSED transitions |
| **Unit: Bulkhead rejection** | Verify correct fallback when concurrent calls exceed limit |
| **Unit: Fallback response** | Verify `PAYMENT_MAINTENANCE` JSON structure |
| **Unit: State transitions** | Verify correct order state for each failure scenario |
| **Unit: Cleanup job** | Verify stale `PAYING` orders are cancelled and quantity restored |
| **Unit: Exception classification** | Verify `PaymentDeclinedException` is ignored, `IOException` is recorded |
| **Integration: Full purchase flow** | Reserve → Pay (mock success) → Complete |
| **Integration: Circuit open flow** | Reserve → Pay (circuit open) → PAYMENT_MAINTENANCE → Hold |

### 9.6 Implementation Order

```
Step 1: Add dependencies (pom.xml)
Step 2: Create exception classes (PaymentDeclinedException, PaymentGatewayException)
Step 3: Create DTOs (PaymentResult)
Step 4: Create PaymentGatewayService interface + MockPaymentGatewayService
Step 5: Create PaymentFallbackHandler
Step 6: Add Resilience4j config to application.yml
Step 7: Create PaymentProtectionConfig (event listeners)
Step 8: Refactor TicketPurchaseService (split transaction) — coordinate with Member 1
Step 9: Update TicketController
Step 10: Update GlobalExceptionHandler
Step 11: Create StaleOrderCleanupJob
Step 12: Create PaymentHealthIndicator
Step 13: Update OrderRepository
Step 14: Write unit tests
Step 15: Write integration tests
Step 16: Update documentation
```

---

## 10. Open Questions for Team

> [!IMPORTANT]
> These questions should be resolved before implementation begins.

1. **Member 1 coordination:** Does Member 1 approve splitting `TicketPurchaseService.purchaseTicket()` into three phases? This is mandatory for correct Circuit Breaker integration.

2. **Payment provider:** Is a real payment provider API planned (e.g., Stripe, VNPay), or will the mock implementation suffice for the final project demo? This affects whether we need a real HTTP client (`RestTemplate`/`WebClient`) or just the mock.

3. **Actuator dependency:** Is `spring-boot-starter-actuator` already planned or can Member 4 add it? It's required for Resilience4j metrics and the custom `PaymentHealthIndicator`.

4. **Reservation TTL:** Is 10 minutes acceptable for holding tickets while payment is unavailable? This is the maximum time a user's selected tickets are reserved without payment.
