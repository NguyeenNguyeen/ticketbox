# Progress Tracker — TicketBox (Member 4)

Last Updated: 2026-06-03

---

## Phase 0 — Project Setup & Scaffolding

- [x] Repository structure created
- [x] README.md with folder structure and git workflow
- [x] .env.example with PostgreSQL, Gemini, Resend keys
- [x] .gitignore configured
- [x] Branch strategy defined (main, develop, backend, frontend, mobileapp, infra)

---

## Phase 1 — Infrastructure (Member 4)

### Docker Compose

- [x] docker-compose.yml created (PostgreSQL 15-alpine, Redis 7-alpine, RabbitMQ 3-management-alpine)
- [x] Named volumes for data persistence (postgres_data, redis_data, rabbitmq_data)
- [x] Healthchecks defined for all 3 services
- [ ] **FIX NEEDED:** PostgreSQL healthcheck uses empty `-U` and `-d` flags
- [x] init.sql with sample concerts table and seed data
- [x] Infrastructure README (infra/README.md) — complete setup guide

### Docker Improvements Needed

- [ ] Add `depends_on` with `condition: service_healthy` for Spring Boot service
- [ ] Fix PostgreSQL healthcheck to use actual env variables
- [ ] Consider adding network configuration for service discovery
- [ ] Consider memory limits for lightweight development environment

---

## Phase 1.5 — Backend Core (Member 1 — COMPLETED)

- [x] Spring Boot 3.2.4 project initialized (pom.xml, Application.java)
- [x] Entity classes: User, Concert, Order, OrderItem, Ticket, TicketCategory, OrderStatus, TicketStatus, RoleName
- [x] Repository interfaces: UserRepository, ConcertRepository, OrderRepository, OrderItemRepository, TicketRepository, TicketCategoryRepository
- [x] Spring Security: SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider, CustomUserDetailsService
- [x] Redis: RedisConfig (CacheManager), RedisService (distributed locks, idempotency)
- [x] Controllers: AuthController (/api/auth/**), TicketController (/api/tickets/purchase)
- [x] Services: ConcertService (@Cacheable), TicketPurchaseService (full purchase flow)
- [x] Design Patterns: State (Order lifecycle), Strategy (Pricing), Factory (Ticket creation)
- [x] application.yml with PostgreSQL, Redis, JWT config

---

## Phase 2 — API Protection (Member 4)

### Rate Limiting — DESIGN

- [x] Existing backend analysis completed
- [x] Architecture design completed (01_api_protection_design.md)
- [x] Package structure proposed
- [x] Integration plan defined
- [x] Edge case analysis completed
- [x] Risk assessment completed

### Rate Limiting — IMPLEMENTATION ✅ COMPLETE

- [x] Add Bucket4j dependencies to pom.xml (bucket4j-core:8.10.1, bucket4j-redis:8.10.1)
- [x] Create RateLimitProperties.java (@ConfigurationProperties)
- [x] Create RateLimitConfig.java (dedicated Lettuce client + LettuceBasedProxyManager bean)
- [x] Create RateLimitKeyResolver.java (IP/username key resolution)
- [x] Create RateLimitFilter.java (OncePerRequestFilter — core logic)
- [x] Create ErrorResponse.java (standard error DTO, @JsonInclude)
- [x] Create RateLimitExceededException.java
- [x] Create GlobalExceptionHandler.java (@RestControllerAdvice — project-wide)
- [x] Add rate-limit config block to application.yml
- [x] Register RateLimitFilter in SecurityConfig.java (after JwtAuthenticationFilter)
- [x] Unit tests: RateLimitKeyResolverTest (7 tests, all passing)
- [x] Unit tests: RateLimitFilterTest (9 tests, all passing)
- [x] Build verification: mvn compile + mvn test → BUILD SUCCESS (16/16)
- [x] Implementation documentation: 02_api_protection_implementation.md

### Rate Limiting — REVIEW ✅ COMPLETE

- [x] Architecture Review
- [x] Security Review
- [x] Concurrency & Redis Failure Review
- [x] Performance & Edge Case Review
- [x] Code Quality Assessment
- [x] Review documentation: 03_api_protection_review.md

### Rate Limiting — REMEDIATION ✅ COMPLETE (29 tests passing)

- [x] CRITICAL 2.1: IP spoofing fix (trustProxyHeaders flag, default=false)
- [x] CRITICAL 2.2: DoS shield pre-auth Caffeine gate (blocks floods before JWT/DB)
- [x] IMPORTANT 3.1: Auth-tier brute-force protection (/api/auth/** 10/min per IP)
- [x] IMPORTANT 3.2: Redis fallback to local Caffeine (no longer fully fail-open)
- [x] IMPORTANT 3.3: Actuator whitelist narrowed (only health + info)
- [x] MINOR 4.1: Dead code removed from GlobalExceptionHandler
- [x] Tests updated: 29 tests, 0 failures (12 resolver + 17 filter)
- [x] Remediation documentation: 04_api_protection_remediation.md

### Payment Protection — DESIGN ✅ COMPLETE

- [x] Current backend analysis (TicketPurchaseService, State Pattern, Redis usage)
- [x] Circuit Breaker design (count-based sliding window, Resilience4j)
- [x] Bulkhead design (semaphore-based, max 10 concurrent)
- [x] Graceful Degradation design (PAYMENT_MAINTENANCE fallback)
- [x] Transaction boundary refactoring design (3-phase split)
- [x] Order state transition analysis for all failure scenarios
- [x] Observability & monitoring design (metrics, logging, health indicator)
- [x] Risk assessment (business, data consistency, UX, operational)
- [x] Implementation roadmap (16 steps, 9 new classes, 6 modified classes)
- [x] Design documentation: 05_payment_protection_design.md

### Payment Protection — IMPLEMENTATION ✅ COMPLETE

- [x] Add Resilience4j + AOP + Actuator dependencies (pom.xml)
- [x] Create PaymentDeclinedException + PaymentGatewayException
- [x] Create PaymentResult DTO
- [x] Create PaymentGatewayService interface + MockPaymentGatewayService
- [x] Create PaymentFallbackHandler
- [x] Add Resilience4j config to application.yml
- [x] Create PaymentProtectionConfig (event listeners)
- [x] Refactor TicketPurchaseService (split transaction) — Member 1 coordination
- [x] Update TicketController
- [x] Update GlobalExceptionHandler
- [x] Create StaleOrderCleanupJob (expired PAYING → CANCELLED)
- [x] Create PaymentHealthIndicator
- [x] Update OrderRepository and OrderItemRepository
- [x] Unit tests (circuit states, bulkhead, fallback, state transitions, cleanup)
- [x] Integration tests (Mocked via Mockito)
- [x] Implementation documentation: 06_payment_protection_implementation.md

### Payment Protection — REVIEW ✅ COMPLETE

- [x] Architecture compliance review
- [x] Circuit Breaker configuration analysis
- [x] Bulkhead configuration analysis
- [x] Transaction boundary review (Critical Flaw Found: AOP Bypass)
- [x] Order state consistency review (Critical Flaw Found: Blind Cancellation)
- [x] Review documentation: 07_payment_protection_review.md

### Payment Protection — REMEDIATION ✅ COMPLETE

- [x] Fix AOP self-invocation transaction bypass
- [x] Fix blind cancellation in stale order cleanup
- [x] Tune Resilience4j thresholds and bulkhead wait duration
- [x] Add Micrometer observability to background jobs
- [x] Remediation documentation: 08_payment_protection_remediation.md

---

## Phase 3 — Async Processing (Member 4) — DESIGN COMPLETE

### Async Architecture Design ✅ COMPLETE
- [x] Backend analysis and integration points
- [x] Exchange topology (Commands, Events, DLX)
- [x] Queue topology (Email, CSV, AI, DLQ)
- [x] Retry strategy (Spring AMQP Retry + DLQ)
- [x] Worker boundaries and Idempotency
- [x] Failure analysis (Poison messages, RabbitMQ outages)
- [x] Monitoring strategy
- [x] Design document: `09_async_architecture_design.md`

### RabbitMQ Configuration ✅ COMPLETE
- [x] Add `spring-boot-starter-amqp`
- [x] Exchange definitions
- [x] Queue definitions
- [x] DLQ configuration
- [x] Retry mechanism (max 3 retries)
- [x] Spring AMQP configuration
- [x] Implementation documentation: `10_async_infrastructure_implementation.md`
- [x] Independent review: `11_async_infrastructure_review.md` (Identified concurrency bottleneck in stateless retries)
- [x] Remediation applied: `12_async_infrastructure_remediation.md` (Concurrency fixed, YML configured)
- [x] Runtime Validation: `12_5_async_infrastructure_validation.md` (CRITICAL FAILURE: Message Loss in DLQ Routing)
- [x] DLQ Routing Fix: `12_6_dlq_routing_fix.md` (RepublishMessageRecoverer prefix removed, integration test assertions added, APPROVED FOR RE-VALIDATION)

### CSV Import Worker — DESIGN COMPLETE
- [x] Stream processing architecture defined
- [x] Idempotency strategy documented
- [x] Error handling (row-level vs system-level) defined
- [x] Progress tracking mechanism designed
- [x] Design document: `13_csv_worker_design.md`

### CSV Import Worker — IMPLEMENTATION
- [x] Update `CsvImportMessage`
- [x] Implement `CsvRowValidator`
- [x] Implement `ImportProgressTracker`
- [x] CSV streaming reader (`CsvImportService`)
- [x] Fault-tolerant line-by-line processing
- [x] Idempotent import logic
- [x] Duplicate detection
- [x] Error logging for malformed lines
- [x] Integration tests (`CsvWorkerTest.java`)
- [x] Implementation document: `14_csv_worker_implementation.md`

### CSV Import Worker — VALIDATION
- [x] Code Review complete (`15_csv_worker_review.md`)
- [x] Runtime Validation tests executed (`CsvWorkerExtendedValidationTest.java`)
- [x] DLQ, Retry, and large file integrity verified
- [x] Validation document: `16_csv_worker_validation.md`

### AI Worker

- [x] AI Worker Design (`17_ai_worker_design.md`)
- [x] PDF text extraction (`PdfExtractionService`)
- [x] AI Provider integration (`AiProviderClient`, `GeminiAiClient`, `OpenAiClient`)
- [x] Prompt generation (`AiPromptBuilder`)
- [x] Response parsing (`AiResponseValidator`)
- [x] Result storage (`AiBioService`, `Concert.artistBiography`)
- [x] AI Worker Implementation (`18_ai_worker_implementation.md`)

### Email Worker

- [x] Email Worker Design (`23_email_worker_design.md`)
- [x] E-ticket email with QR attachment
- [x] Resend API integration
- [x] Email queue consumer
- [x] Retry handling
- [x] Email Worker Implementation (`24_email_worker_implementation.md`)
- [x] Email Worker Review (`25_email_worker_review.md`)
- [x] Email Worker Remediation (`28_email_delivery_fix.md` - Added missing RabbitMQ producer logic to `TicketPurchaseService`)

---

## Phase 4 — Frontend Bug Fixes & Integration

- [x] Fix GA seat missing in SeatMap (SVG viewBox adjustment)
- [x] Fix Quick Login Demo (use real login logic with default test accounts instead of mock tokens)
- [x] Fix `data.sql` to include correct Bcrypt hashes for test accounts (customer1, admin1, checker1)
- [x] Feature: Admin specific features - redirected ORGANIZER directly to `/admin` upon login.
- [x] Feature: Customer Order History - Added `OrderController` with `/api/orders/history` and Frontend `/orders` page.
- [x] Feature: Admin Order & User Management - Added `UserController`, `GET /api/admin/orders`, `GET /api/admin/users` and corresponding admin dashboard pages.
- [x] Fix: Logout logic now properly redirects to `/auth/login`.

## Phase 4.5 — Frontend Blueprint (Person 2) ✅ COMPLETE

- [x] Create Blueprint Documents (`design.md`, `auth.md`, `payment.md`)
- [x] Integrate Realtime SSE and Polling (`SeatMap.tsx`)
- [x] Add Seat Selection Limits (`useSeatStore.ts`)
- [x] Improve API Client for custom Headers & Offline detection (`api.ts`)
- [x] Implement Checkout Retry, Header Idempotency & Error Handling (`CheckoutForm.tsx`)
- [x] Add Cancel Concert feature (`admin/concerts/[id]/edit/page.tsx`)

## Phase 4.6 — Frontend Blueprint Continuation (Person 2) ✅ COMPLETE

- [x] Apply SSR/ISR for HomePage (`page.tsx`, `ConcertSection.tsx`)
- [x] Add Payment Callback Page (`checkout/callback/page.tsx`)
- [x] Add SVG and PDF Upload to Concert Form (`ConcertForm.tsx`)
- [x] Build CSV Importer with Preview for Guest List (`admin/guests/page.tsx`)

## Phase 4.7 — Frontend Bug Fixes (Member 4) ✅ COMPLETE

- [x] Fix: Prevent freezing at update event page after importing file (added timeout and error handling for AI job polling in `ConcertForm.tsx`).
- [x] Fix: Allow editing max tickets per person in event editing (added `maxPerUser` field to `TicketCategory` entity and updated `ConcertController.java`).
- [x] Feature: Add functionality to resume a postponed event (added `resumeConcert` in `ConcertService.java` and `ConcertController.java`, added `Tiếp tục sự kiện` button in `admin/concerts/[id]/edit/page.tsx`).
- [x] Fix: Block ticket booking on the customer side if the event is postponed/cancelled (updated `TicketPurchaseService.java` and disabled buttons in `TicketSelector.tsx`).
- [x] Fix: Ensure updated AI biography artist data is immediately visible on frontend (corrected cache eviction names to `concertsV7` and `concertsListV7` in `AiBioService.java`).
- [x] Feature: Add manual Guest Artist editing feature in the Admin Event form (`ConcertController`, `ConcertService`, `ConcertForm.tsx`).
- [x] Feature: Add SVG Seat Map upload and display functionality (`ConcertController`, `ConcertForm.tsx`, `page.tsx`).
- [x] Feature: Make SVG Seat Map interactive (`InteractiveSeatMap.tsx`).
- [x] Feature: Interactive Sandbox Payment Gateway (VNPAY & MoMo UI, complete API)
- [x] Feature: Hiển thị vé điện tử trực tiếp sau thanh toán thành công và trong trang chi tiết Lịch sử giao dịch (`payment/callback/page.tsx`, `orders/[id]/page.tsx`).
- [x] Fix: Yêu cầu đăng nhập trước khi mua vé và thanh toán (Thêm `isAuthenticated` check và redirect trong `concerts/[id]/page.tsx` và `checkout/page.tsx`).
- [x] Fix: Chặn người dùng mua vé đối với các sự kiện chưa mở bán (`UPCOMING`) trên frontend và backend.
- [x] Feature: Cho phép xóa hoàn toàn sự kiện khỏi cơ sở dữ liệu nếu sự kiện đó đã bị hủy (`DELETE /api/admin/concerts/{id}/hard`).

---

## Blocked Tasks

| Task | Blocked By | Reason |
|------|-----------|--------|
| All Workers | Business requirements | Implementation of workers requires parsing and generation logic |

---

## Dependencies on Other Members — Updated

| Member | Dependency | Status |
|--------|-----------|--------|
| Member 1 (Backend) | Spring Boot project initialization | ✅ Complete |
| Member 1 (Backend) | Entity/DTO definitions | ✅ Complete (User, Order, Ticket, etc.) |
| Member 1 (Backend) | Security configuration (JWT, RBAC) | ✅ Complete |
| Member 1 (Backend) | Redis configuration | ✅ Complete |
| Member 1 (Backend) | Payment service interface | ❌ Not started |
| Member 2 (Frontend) | None | N/A |
| Member 3 (Mobile) | Technology decision | ❌ Pending |
