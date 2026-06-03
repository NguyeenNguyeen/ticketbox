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

- [ ] Resilience4j dependency
- [ ] Circuit Breaker configuration (count-based sliding window)
- [ ] Bulkhead configuration (thread limit for payment calls)
- [ ] Fallback handler (PAYMENT_MAINTENANCE response)
- [ ] Graceful degradation logic

---

## Phase 3 — Async Processing (Member 4) — Not Started

### RabbitMQ Configuration

- [ ] Exchange definitions
- [ ] Queue definitions
- [ ] DLQ configuration
- [ ] Retry mechanism (max 3 retries)
- [ ] Spring AMQP configuration

### CSV Import Worker

- [ ] CSV streaming reader
- [ ] Fault-tolerant line-by-line processing
- [ ] Idempotent import logic
- [ ] Duplicate detection
- [ ] Error logging for malformed lines

### AI Worker

- [ ] PDF text extraction
- [ ] Text cleaning/preprocessing
- [ ] Gemini API integration
- [ ] Request throttling
- [ ] Async consumer from RabbitMQ

### Email Worker

- [ ] Resend API integration
- [ ] Email queue consumer
- [ ] Retry handling
- [ ] E-ticket email with QR attachment
- [ ] Event reminder email

---

## Blocked Tasks

| Task | Blocked By | Reason |
|------|-----------|--------|
| Circuit Breaker | Member 1 | Requires payment service interface |
| RabbitMQ Spring Config | Design phase | Rate limiting complete — this is next |
| All Workers | Design phase | Requires RabbitMQ setup first |

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
