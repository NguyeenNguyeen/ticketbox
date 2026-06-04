# Payment Protection Implementation

## Objective
Implement the Payment Protection module as designed in `05_payment_protection_design.md`. This includes Resilience4j Circuit Breaker, Semaphore Bulkhead, graceful degradation for infrastructure failures, and safely splitting database transactions to prevent pessimistic lock exhaustion during external API calls.

## Decisions Made
1. **Transaction Split:** Refactored `TicketPurchaseService` into three phases (Reserve, Pay, Finalize) to prevent holding pessimistic DB locks during external I/O.
2. **Resilience4j Configuration:** Integrated `resilience4j-spring-boot3` with `application.yml` configurations for the `paymentGateway` instance, using a count-based sliding window of 10 calls.
3. **Graceful Degradation:** Created `PaymentFallbackHandler` to return HTTP 200 with the `PAYMENT_MAINTENANCE` code instead of HTTP 500 when circuit breaker trips or bulkhead is full.
4. **Stale Order Cleanup:** Implemented `StaleOrderCleanupJob` running every 60 seconds to detect `PAYING` orders older than 10 minutes and correctly restore their ticket inventory.
5. **Observability:** Added `PaymentHealthIndicator` to expose Circuit Breaker and Bulkhead status, and `PaymentProtectionConfig` to log state transitions.

## Files Created
- `apps/backend/src/main/java/com/ticketbox/backend/dto/PaymentResult.java`
- `apps/backend/src/main/java/com/ticketbox/backend/exception/PaymentDeclinedException.java`
- `apps/backend/src/main/java/com/ticketbox/backend/exception/PaymentGatewayException.java`
- `apps/backend/src/main/java/com/ticketbox/backend/service/PaymentGatewayService.java`
- `apps/backend/src/main/java/com/ticketbox/backend/service/impl/MockPaymentGatewayService.java`
- `apps/backend/src/main/java/com/ticketbox/backend/service/PaymentFallbackHandler.java`
- `apps/backend/src/main/java/com/ticketbox/backend/config/PaymentProtectionConfig.java`
- `apps/backend/src/main/java/com/ticketbox/backend/config/PaymentHealthIndicator.java`
- `apps/backend/src/main/java/com/ticketbox/backend/scheduler/StaleOrderCleanupJob.java`
- `apps/backend/src/test/java/com/ticketbox/backend/service/TicketPurchaseServiceTest.java`

## Files Modified
- `apps/backend/pom.xml` (Added resilience4j, spring-boot-starter-aop, actuator)
- `apps/backend/src/main/resources/application.yml` (Added resilience4j circuitbreaker/bulkhead config, payment ttl)
- `apps/backend/src/main/java/com/ticketbox/backend/service/TicketPurchaseService.java` (Split transaction flow)
- `apps/backend/src/main/java/com/ticketbox/backend/exception/GlobalExceptionHandler.java` (Added handler for payment and resilience4j exceptions)
- `apps/backend/src/main/java/com/ticketbox/backend/controller/TicketController.java` (Removed try-catch block to rely on GlobalExceptionHandler)
- `apps/backend/src/main/java/com/ticketbox/backend/repository/OrderRepository.java` (Added `findByStatusAndCreatedAtBefore`)
- `apps/backend/src/main/java/com/ticketbox/backend/repository/OrderItemRepository.java` (Added `findByOrderId`)

## Test Plan
- Unit tests run and pass (`TicketPurchaseServiceTest`).
- Covered scenarios: normal success, business payment decline (insufficient funds), payment gateway exception, and circuit breaker open (CallNotPermittedException).
- **Result:** `BUILD SUCCESS`.

## Dependencies
- `resilience4j-spring-boot3` (v2.2.0)
- `spring-boot-starter-aop`
- `spring-boot-starter-actuator`

## Remaining Work
- End-to-end integration testing in staging.
- Discussion with Member 1 about the final confirmation of the `TicketPurchaseService` transaction split.
- Setup Prometheus/Grafana dashboards for the newly exposed Actuator metrics.

## Open Questions
- Should the `StaleOrderCleanupJob` send notifications to users indicating their order was cancelled due to payment timeout?
