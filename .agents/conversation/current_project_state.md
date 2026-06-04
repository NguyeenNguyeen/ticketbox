# Current Project State — TicketBox

Last Updated: 2026-06-04 (Post-Payment Protection Design)

---

## Current Phase

**Phase 3 — Async Processing**
- ✅ Async Architecture: Design complete (`09_async_architecture_design.md`)
- ✅ RabbitMQ Config: Implementation complete (`10_async_infrastructure_implementation.md`)
- ✅ RabbitMQ Review: Review complete (`11_async_infrastructure_review.md`)
- ✅ RabbitMQ Remediation: Remediation complete (`12_async_infrastructure_remediation.md`)
- ✅ RabbitMQ Validation: Validation failed (`12_5_async_infrastructure_validation.md`), then fixed and re-validated (`12_6_dlq_routing_fix.md`)
- ⏳ Async Workers: Pending implementation

Phase 3 RabbitMQ infrastructure is now fully validated. The application-level `RepublishMessageRecoverer` misrouting bug has been fixed, and DLQ message retention works correctly without message loss. The infrastructure is now approved for worker implementation.

---

## Existing Architecture Status

### Backend (apps/backend/)

| Layer | Components | Status |
|-------|-----------|--------|
| Entry Point | `Application.java` (@SpringBootApplication + @EnableCaching) | ✅ |
| Configuration | `RedisConfig.java`, `application.yml` | ✅ |
| Security | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `CustomUserDetailsService` | ✅ |
| Controllers | `AuthController` (/api/auth/**), `TicketController` (/api/tickets/purchase) | ✅ |
| Services | `ConcertService`, `TicketPurchaseService`, `RedisService` | ✅ |
| Entities | `User`, `Concert`, `Order`, `OrderItem`, `Ticket`, `TicketCategory`, `OrderStatus`, `TicketStatus`, `RoleName` | ✅ |
| Repositories | All 6 repositories including pessimistic lock query | ✅ |
| Design Patterns | State (Order lifecycle), Strategy (Pricing), Factory (Ticket creation) | ✅ |
| Rate Limiting | `security.ratelimit` package (4 classes) | ✅ Implemented + tested |
| Global Exception Handler | `exception.GlobalExceptionHandler` | ✅ Implemented |
| Error Response DTO | `dto.ErrorResponse` | ✅ Implemented |

### Infrastructure (infra/)

| Component | Status |
|-----------|--------|
| PostgreSQL 15-alpine | ✅ Docker configured (healthcheck has bug) |
| Redis 7-alpine | ✅ Docker configured |
| RabbitMQ 3-management-alpine | ✅ Docker configured |

### Frontend (apps/frontend/)

| Component | Status |
|-----------|--------|
| Next.js project | ❌ Placeholder README only |

### Mobile (apps/mobileapp/)

| Component | Status |
|-----------|--------|
| Mobile app | ❌ Placeholder README only |

---

## Existing Decisions

### Key Technical Decisions Already Implemented

* JWT subject = `username` (not user ID)
* UserDetails authority = `ROLE_<RoleName.name()>`
* Redis connection = Spring Boot auto-config (Lettuce)
* Caching = `@Cacheable` with `RedisCacheManager` (30min TTL)
* Locking = `@Lock(PESSIMISTIC_WRITE)` in `TicketCategoryRepository`
* Idempotency = Redis `SETNX` with 10-minute TTL

### Rate Limiting Decision (New — 01_api_protection_design.md)

* Algorithm: Token Bucket (Bucket4j + Redis)
* Public tier: 100 req/min by client IP
* Private tier: 10 req/min by username
* Filter position: After JwtAuthenticationFilter, before AuthorizationFilter
* Redis failure: Fail-open with warning log
* Configuration: Externalized via @ConfigurationProperties

---

## Missing Implementations (Member 4 Scope)

1. **Rate Limiting** — Design complete, implementation pending approval
2. **Circuit Breaker + Bulkhead** — Not started (requires payment service interface from Member 1)
3. **RabbitMQ Spring AMQP config** — Not started
4. **DLQ + Retry mechanism** — ✅ Complete and validated
5. **CSV Import Worker** — ✅ Complete and fully verified (`16_csv_worker_validation.md`)
6. **AI Worker** — Not started
7. **Email Worker** — Not started
8. **Docker Compose fixes** — PostgreSQL healthcheck bug still present
9. **GlobalExceptionHandler** — Not started (planned with rate limiting)

---

## Recommended Next Task

**Implement Rate Limiting** (pending design approval)

This is the highest-priority task for Member 4 because:
1. Design is complete and documented (01_api_protection_design.md)
2. All backend dependencies are available (Spring Security, JWT, Redis)
3. No blocking dependencies on other team members
4. Critical for meeting the 80,000 CCU requirement
5. Provides foundational infrastructure (GlobalExceptionHandler, ErrorResponse) used by the entire project

### Files To Create (7)

* `security/ratelimit/RateLimitFilter.java`
* `security/ratelimit/RateLimitConfig.java`
* `security/ratelimit/RateLimitProperties.java`
* `security/ratelimit/RateLimitKeyResolver.java`
* `exception/RateLimitExceededException.java`
* `exception/GlobalExceptionHandler.java`
* `dto/ErrorResponse.java`

### Files To Modify (3)

* `pom.xml` — Add Bucket4j dependencies
* `application.yml` — Add rate limit config
* `SecurityConfig.java` — Register filter
