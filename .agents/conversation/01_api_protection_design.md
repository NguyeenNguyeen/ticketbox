# 01 — API Protection Design (Rate Limiting)

**Task:** Design the Rate Limiting module for TicketBox  
**Author:** Member 4 (Infrastructure & Platform Protection)  
**Created:** 2026-06-03  
**Status:** ✅ Design Complete — Implementation verified in `02_api_protection_implementation.md`

---

## 1. Existing Backend Analysis

### 1.1 Project Structure

The backend is a Spring Boot 3.2.4 project located at `apps/backend/`.

```text
com.ticketbox.backend/
├── Application.java                         # @SpringBootApplication + @EnableCaching
├── config/
│   └── RedisConfig.java                     # RedisCacheManager with 30min TTL
├── controller/
│   ├── AuthController.java                  # /api/auth/** (login, register)
│   └── TicketController.java                # /api/tickets/purchase
├── entity/
│   ├── Concert.java
│   ├── Order.java                           # Includes idempotency_key index
│   ├── OrderItem.java
│   ├── OrderStatus.java                     # PENDING, PAYING, COMPLETED, CANCELLED
│   ├── RoleName.java                        # CUSTOMER, ORGANIZER, CHECKER
│   ├── Ticket.java
│   ├── TicketCategory.java                  # Has @Version for optimistic lock
│   ├── TicketStatus.java
│   └── User.java                            # id, username, password, role
├── pattern/
│   ├── factory/                             # TicketFactory, VIPTicketFactory, StandardTicketFactory
│   ├── state/                               # OrderState, PendingState, PayingState, etc.
│   └── strategy/                            # PricingStrategy, StandardPricingStrategy, DiscountPricingStrategy
├── repository/
│   ├── ConcertRepository.java
│   ├── OrderItemRepository.java
│   ├── OrderRepository.java                 # findByIdempotencyKey()
│   ├── TicketCategoryRepository.java        # Pessimistic Lock query
│   ├── TicketRepository.java
│   └── UserRepository.java                  # findByUsername()
├── security/
│   ├── CustomUserDetailsService.java        # Loads user, maps ROLE_<name>
│   ├── JwtAuthenticationFilter.java         # OncePerRequestFilter, extracts JWT
│   ├── JwtTokenProvider.java                # Generates/validates JWT (jjwt 0.12.5)
│   └── SecurityConfig.java                  # Filter chain, RBAC authorization rules
└── service/
    ├── ConcertService.java                  # @Cacheable with Redis
    ├── RedisService.java                    # acquireLock(), releaseLock(), setIfAbsentIdempotencyKey()
    └── TicketPurchaseService.java           # Full purchase flow with idempotency + pessimistic lock
```

### 1.2 Dependencies (pom.xml)

| Dependency | Status |
|-----------|--------|
| spring-boot-starter-web | ✅ Present |
| spring-boot-starter-data-jpa | ✅ Present |
| spring-boot-starter-security | ✅ Present |
| spring-boot-starter-data-redis | ✅ Present |
| spring-boot-starter-validation | ✅ Present |
| postgresql | ✅ Present (runtime) |
| lombok | ✅ Present |
| jjwt-api/impl/jackson 0.12.5 | ✅ Present |
| **bucket4j-core** | ❌ **Missing — needs to be added** |
| **bucket4j-redis / bucket4j-lettuce** | ❌ **Missing — needs to be added** |

### 1.3 Spring Security Configuration (SecurityConfig.java)

**Current filter chain:**

```
HTTP Request
    │
    ▼
[JwtAuthenticationFilter]  ← addFilterBefore(UsernamePasswordAuthenticationFilter)
    │
    ▼
[UsernamePasswordAuthenticationFilter]
    │
    ▼
[AuthorizationFilter]  ← authorizeHttpRequests() rules
    │
    ▼
Controller
```

**Current authorization rules:**

| Pattern | Access |
|---------|--------|
| `/api/auth/**` | `permitAll()` |
| `/api/public/**` | `permitAll()` |
| `/api/admin/**` | `hasRole("ORGANIZER")` |
| `/api/checker/**` | `hasRole("CHECKER")` |
| `/api/tickets/purchase` | `hasRole("CUSTOMER")` |
| Everything else | `authenticated()` |

### 1.4 JWT Implementation

* **JwtAuthenticationFilter:** Extracts Bearer token → validates → loads UserDetails → sets SecurityContext
* **JwtTokenProvider:** Subject = `username` (not user ID)
* **Token payload:** `sub` (username), `iat`, `exp`

**Key observation:** The JWT subject is the *username*, not the user ID. To rate limit by User ID, we must either:
- (a) Resolve username → User entity → `user.getId()` via `UserRepository`
- (b) Add `userId` as a custom claim to the JWT
- **(c) Use the username itself as the rate limit key** (simplest, avoids DB call per request)

### 1.5 Redis Configuration

* `RedisConfig.java`: Configures `RedisCacheManager` with 30-min TTL for `@Cacheable`
* `application.yml`: Redis at `${REDIS_HOST:localhost}:${REDIS_PORT:6379}`
* `RedisService.java`: Already provides `setIfAbsent()` and distributed lock patterns
* **No `RedissonClient` or `LettuceConnectionFactory` explicit bean** — uses Spring Boot auto-config (Lettuce by default)

### 1.6 Exception Handling

**No global exception handler exists.** Controllers use inline try-catch blocks returning `ResponseEntity`.

There is no `@RestControllerAdvice` or `@ControllerAdvice` class.

### 1.7 Summary: What Can Be Reused

| Component | Reusability for Rate Limiting |
|-----------|-------------------------------|
| `JwtAuthenticationFilter` | ✅ Runs before our filter — gives us SecurityContext with username |
| `JwtTokenProvider` | ✅ Can validate token and extract username |
| `SecurityConfig` | ✅ Integration point — register RateLimitFilter in the chain |
| `RedisConfig` / Spring Data Redis | ✅ Redis connection already configured (Lettuce auto-config) |
| `UserRepository` | ⚠️ Available but should be avoided per-request for performance |
| Error response format | ❌ No standard exists — we define our own |

### 1.8 Summary: What Is Missing

| Component | Required For |
|-----------|-------------|
| Bucket4j dependencies | Rate limiting core |
| Bucket4j-Redis integration (Lettuce) | Distributed buckets |
| RateLimitFilter | Request interception |
| Rate limit configuration (YAML) | Externalized limits |
| Standard error response DTO | HTTP 429 JSON body |
| Global exception handling | Consistent error format |
| Whitelist mechanism | Bypass for internal/health endpoints |

---

## 2. Proposed Rate Limiting Architecture

### 2.1 Algorithm: Token Bucket

Per approved technology decision: **Token Bucket** via **Bucket4j + Redis**.

Token Bucket behavior:
- Each bucket has a capacity (e.g., 100 tokens)
- Tokens are refilled at a fixed rate (e.g., 100 tokens per minute)
- Each request consumes 1 token
- If no tokens remain → reject with HTTP 429

### 2.2 Two-Tier Strategy

```
                    ┌──────────────────────┐
                    │   Incoming Request    │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │  Is path whitelisted? │
                    │  (health, actuator,   │
                    │   swagger, static)    │
                    └──────┬───────┬───────┘
                       YES │       │ NO
                           │       │
                    ┌──────▼──┐    │
                    │  PASS   │    │
                    └─────────┘    │
                                   │
                    ┌──────────────▼───────┐
                    │ Is request           │
                    │ authenticated?       │
                    │ (SecurityContext has  │
                    │  principal)          │
                    └──────┬───────┬───────┘
                       YES │       │ NO
                           │       │
              ┌────────────▼──┐  ┌─▼────────────────┐
              │ PRIVATE TIER  │  │  PUBLIC TIER      │
              │ Key: user:<id>│  │  Key: ip:<address>│
              │ Limit: 10/min │  │  Limit: 100/min   │
              └───────┬───────┘  └────────┬──────────┘
                      │                   │
              ┌───────▼───────────────────▼──────────┐
              │         Check Bucket4j Bucket         │
              │         (stored in Redis)             │
              └───────┬───────────────────┬──────────┘
                  HAS │                   │ EMPTY
                TOKEN │                   │
              ┌───────▼──┐         ┌──────▼──────────┐
              │  ALLOW   │         │  REJECT          │
              │  request │         │  HTTP 429         │
              │  chain   │         │  + JSON body      │
              │  continues│        │  + Retry-After    │
              └──────────┘         └─────────────────┘
```

### 2.3 Bucket Storage

Buckets are stored in **Redis** (distributed), not in-memory. This ensures:
- Rate limits are shared across multiple backend instances
- Restarting the backend does not reset limits
- Buckets auto-expire via Redis TTL (no memory leak)

### 2.4 Redis Key Format

| Tier | Key Format | Example |
|------|-----------|---------|
| Public | `ratelimit:ip:{client_ip}` | `ratelimit:ip:192.168.1.100` |
| Private | `ratelimit:user:{username}` | `ratelimit:user:john_doe` |

Using `username` instead of `user.id` avoids a database query per request. The JWT subject is the username, which is already available from `SecurityContextHolder` after `JwtAuthenticationFilter` runs.

### 2.5 Default Rate Limits

| Tier | Capacity | Refill Rate | Refill Period |
|------|----------|-------------|---------------|
| Public (by IP) | 100 tokens | 100 tokens | per 1 minute |
| Private (by User) | 10 tokens | 10 tokens | per 1 minute |

These values should be **externalized in `application.yml`** to allow tuning without code changes.

### 2.6 HTTP 429 Response Format

```json
{
    "status": 429,
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "retryAfterSeconds": 12
}
```

Response headers:
- `Retry-After: <seconds>` — time until at least 1 token is available
- `X-Rate-Limit-Remaining: <count>` — tokens remaining (on successful requests)

---

## 3. Proposed Package Structure

### 3.1 New Packages

```text
com.ticketbox.backend/
├── security/
│   └── ratelimit/
│       ├── RateLimitFilter.java              # Servlet filter — core rate limiting logic
│       ├── RateLimitConfig.java              # @Configuration — Bucket4j ProxyManager bean
│       ├── RateLimitProperties.java          # @ConfigurationProperties — externalized limits
│       └── RateLimitKeyResolver.java         # Resolves rate limit key (IP or username)
├── exception/
│   ├── RateLimitExceededException.java       # Custom exception
│   └── GlobalExceptionHandler.java           # @RestControllerAdvice — unified error responses
└── dto/
    └── ErrorResponse.java                    # Standard error DTO
```

### 3.2 Class Responsibilities

#### RateLimitFilter

- **Type:** `OncePerRequestFilter` (Spring component)
- **Responsibility:** Intercepts every HTTP request, determines the rate limit key, checks the Bucket4j bucket, and either allows or rejects the request
- **Position:** Registered in Spring Security filter chain **BEFORE** `JwtAuthenticationFilter`
- **Whitelist logic:** Skips rate limiting for internal/health/static endpoints
- **Key resolution:** Delegates to `RateLimitKeyResolver`

#### RateLimitConfig

- **Type:** `@Configuration`
- **Responsibility:** Creates Bucket4j `ProxyManager<String>` bean backed by Redis (Lettuce)
- **Dependencies:** `RedisConnectionFactory` (from Spring auto-config)
- **Key behavior:** Uses `LettuceBasedProxyManager` to store buckets in Redis

#### RateLimitProperties

- **Type:** `@ConfigurationProperties(prefix = "ticketbox.rate-limit")`
- **Responsibility:** Externalized configuration for rate limit values
- **Fields:** `publicCapacity`, `publicRefillTokens`, `publicRefillDurationSeconds`, `privateCapacity`, `privateRefillTokens`, `privateRefillDurationSeconds`, `whitelistedPaths`
- **Enables:** Changing limits without code changes via `application.yml`

#### RateLimitKeyResolver

- **Type:** `@Component`
- **Responsibility:** Determines the rate limit bucket key based on request context
- **Logic:**
  1. Check `SecurityContextHolder` for authenticated principal
  2. If authenticated → key = `ratelimit:user:{username}`
  3. If unauthenticated → key = `ratelimit:ip:{clientIp}`
- **IP extraction:** Uses `X-Forwarded-For` header (for proxy/LB) with fallback to `request.getRemoteAddr()`

#### RateLimitExceededException

- **Type:** `RuntimeException`
- **Responsibility:** Carries rate limit context (retry-after seconds) for the exception handler
- **Usage:** Thrown by `RateLimitFilter` when bucket is empty

#### GlobalExceptionHandler

- **Type:** `@RestControllerAdvice`
- **Responsibility:** Catches `RateLimitExceededException` and other exceptions, returns standardized JSON error responses
- **Note:** This class benefits the entire project, not just rate limiting. It provides a unified error format.

#### ErrorResponse

- **Type:** DTO (record or class)
- **Fields:** `status` (int), `code` (String), `message` (String), `retryAfterSeconds` (Long, nullable)
- **Used by:** `GlobalExceptionHandler` and `RateLimitFilter` (direct response write for filter-level rejections)

---

## 4. Integration Design

### 4.1 Filter Chain Position

The `RateLimitFilter` must execute **BEFORE** `JwtAuthenticationFilter` for two reasons:

1. **Bot protection:** Block abusive IPs before spending resources on JWT validation
2. **Defense in depth:** Even malformed JWT requests consume rate limit tokens

However, there's a nuance: for private (authenticated) rate limiting, we need the JWT to be parsed first to know the username. This creates a **two-pass challenge**.

**Solution: Single filter, after JWT filter**

Place `RateLimitFilter` **AFTER** `JwtAuthenticationFilter` but **BEFORE** `AuthorizationFilter`:

```
HTTP Request
    │
    ▼
[JwtAuthenticationFilter]       ← Extracts JWT, sets SecurityContext
    │
    ▼
[RateLimitFilter]               ← Checks rate limit (IP or username)
    │                              If over limit → HTTP 429 + stop
    ▼
[AuthorizationFilter]           ← RBAC checks
    │
    ▼
Controller
```

**Rationale:** 
- JWT filter is lightweight (only parses header + validates signature)
- Rate limit filter benefits from knowing whether the request is authenticated
- Unauthenticated requests still hit the public tier (by IP)
- This avoids complexity of running rate limiting twice

**Integration in SecurityConfig.java:**

```
http.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
```

This adds the rate limit filter immediately after JWT auth in the chain.

### 4.2 How User Identity Is Obtained

1. `JwtAuthenticationFilter` runs first, parsing the `Authorization: Bearer <token>` header
2. If valid, it sets `SecurityContextHolder.getContext().setAuthentication(...)` with a `UserDetails` principal
3. `RateLimitFilter` then checks `SecurityContextHolder.getContext().getAuthentication()`
4. If authentication exists and `isAuthenticated() == true` → extract `getName()` (returns username)
5. Bucket key = `ratelimit:user:{username}`
6. If no authentication → bucket key = `ratelimit:ip:{clientIp}`

**No database call required.** The username comes directly from the JWT claim → SecurityContext.

### 4.3 How Redis Stores Distributed Buckets

Bucket4j with Lettuce ProxyManager stores each bucket as a Redis key-value entry:

- **Key:** The rate limit key (e.g., `ratelimit:ip:192.168.1.100`)
- **Value:** Serialized Bucket4j bucket state (tokens remaining, last refill time)
- **TTL:** Automatically set based on the bucket configuration's longest refill period + buffer

On each request:
1. `ProxyManager.builder().build(key, bucketConfigSupplier)` → lazily creates or retrieves the bucket
2. `bucket.tryConsumeAndReturnRemaining(1)` → atomically consumes 1 token
3. If `ConsumptionProbe.isConsumed() == true` → allow request
4. If `false` → `probe.getNanosToWaitForRefill()` → calculate `Retry-After` → reject

### 4.4 Avoiding Duplicate Bucket Creation

Bucket4j `ProxyManager` handles this natively:
- `builder().build(key, configSupplier)` → **atomically** checks if a bucket exists in Redis
- If exists → uses existing state
- If not → creates from `configSupplier`
- This is an atomic Redis operation (uses Lua scripts internally)
- No race condition between concurrent requests creating the same bucket

### 4.5 Multi-Instance Support

The design is inherently multi-instance ready:
- All bucket state lives in Redis (shared across all backend instances)
- No in-memory state in the application
- `ProxyManager` uses Redis as the single source of truth
- Rate limits apply **globally** across all instances (a user hitting 3 different instances still shares one bucket)
- Redis itself handles atomic operations — no additional distributed coordination needed

---

## 5. Edge Case Analysis

### 5.1 Requests That MUST Bypass Rate Limiting

| Request Type | Reason | How To Detect |
|-------------|--------|---------------|
| Health check endpoints | Monitoring/orchestration | Path: `/actuator/**` |
| Swagger / OpenAPI docs | Development tooling | Path: `/swagger-ui/**`, `/v3/api-docs/**` |
| Static resources | CSS/JS/images | Path: `/static/**`, `/favicon.ico` |
| Internal error pages | Spring error handling | Path: `/error` |

### 5.2 Requests That MUST Be Rate Limited

| Request Type | Tier | Key |
|-------------|------|-----|
| Public concert browsing (`/api/public/**`) | Public | IP |
| Auth endpoints (`/api/auth/**`) | Public | IP |
| Ticket purchase (`/api/tickets/purchase`) | Private | Username |
| Admin operations (`/api/admin/**`) | Private | Username |
| Checker operations (`/api/checker/**`) | Private | Username |

### 5.3 Edge Case: Missing JWT

- `JwtAuthenticationFilter` runs, finds no `Authorization` header
- SecurityContext remains empty (no authentication set)
- `RateLimitFilter` detects no authentication → **falls back to Public tier (by IP)**
- This is correct: unauthenticated users should be limited by IP

### 5.4 Edge Case: Invalid JWT

- `JwtAuthenticationFilter` catches `JwtException`, logs error, continues chain
- SecurityContext remains empty
- `RateLimitFilter` → same as missing JWT → **Public tier (by IP)**
- The invalid JWT request consumes a public-tier token — this is intentional (defends against token-guessing attacks)

### 5.5 Edge Case: Internal Service-to-Service Calls

- Currently not applicable (monolithic architecture)
- If added in future: internal calls should include a shared internal token/header
- `RateLimitFilter` can check for a `X-Internal-Service` header with a pre-shared key
- **For now:** Not implemented. Document as future consideration.

### 5.6 Edge Case: RabbitMQ Internal Processing

- RabbitMQ consumers (`@RabbitListener`) do NOT go through the HTTP filter chain
- They invoke service methods directly
- **No rate limiting impact.** These are internal method calls, not HTTP requests.

### 5.7 Edge Case: Scheduled Jobs

- `@Scheduled` methods are internal Java calls, not HTTP requests
- **No rate limiting impact.** The filter chain is not involved.

### 5.8 Edge Case: Client Behind Shared IP (NAT)

- Multiple users behind the same corporate NAT/VPN share one IP
- Public-tier rate limit applies per IP → they share the 100 req/min bucket
- **Mitigation:** The public-tier limit is generous (100/min). Once users authenticate, they get individual private-tier limits.
- For future enhancement: Consider `X-Forwarded-For` with per-subnet rules

---

## 6. Risk Assessment

### 6.1 Redis Unavailability

| Risk | Redis goes down → Bucket4j cannot check/create buckets |
|------|------|
| **Severity** | Critical |
| **Impact** | All requests could be blocked or all could be allowed (depending on failure mode) |
| **Mitigation** | **Fail-open strategy:** If Redis is unreachable, allow the request through with a warning log. The system should degrade gracefully — better to allow some extra requests than to block all users. |
| **Implementation** | Wrap bucket check in try-catch. On `RedisConnectionException` → log warning, allow request. |

### 6.2 Race Conditions

| Risk | Two concurrent requests create/consume from the same bucket simultaneously |
|------|------|
| **Severity** | Low |
| **Impact** | Slightly inaccurate token count |
| **Mitigation** | Bucket4j's `LettuceBasedProxyManager` uses Redis Lua scripts for atomic operations. Race conditions are handled by the library. No additional mitigation needed. |

### 6.3 Memory Growth in Redis

| Risk | Millions of unique IPs/users create millions of Redis keys |
|------|------|
| **Severity** | Medium |
| **Impact** | Redis memory grows unbounded |
| **Mitigation** | Bucket4j ProxyManager sets TTL on Redis keys automatically based on bucket refill period. Keys expire when not accessed. Additionally, Redis `maxmemory-policy allkeys-lru` can be set as a safety net. |

### 6.4 IP Spoofing

| Risk | Attacker spoofs `X-Forwarded-For` header to bypass IP-based rate limiting |
|------|------|
| **Severity** | Medium |
| **Impact** | Bot can rotate through fake IPs to avoid rate limits |
| **Mitigation** | 1. Trust `X-Forwarded-For` ONLY from known reverse proxy IPs. 2. If no trusted proxy: use `request.getRemoteAddr()` (cannot be spoofed at TCP level). 3. For production: configure Spring's `ForwardedHeaderFilter` with trusted proxy list. |

### 6.5 Misconfigured Limits

| Risk | Limits set too low → legitimate users blocked. Too high → bots not stopped. |
|------|------|
| **Severity** | Medium |
| **Impact** | User experience degradation or system overload |
| **Mitigation** | 1. Externalize all limits in `application.yml`. 2. Log every rate limit hit for monitoring. 3. Start with conservative limits and adjust based on logs. 4. Consider different limits for different endpoint groups in future iterations. |

### 6.6 Performance Impact

| Risk | Redis round-trip on every request adds latency |
|------|------|
| **Severity** | Low |
| **Impact** | ~1-3ms additional latency per request |
| **Mitigation** | 1. Redis is already in use for caching and locks — the connection pool is warm. 2. Bucket4j operations are single Redis command (Lua script). 3. Monitor p99 latency after implementation. |

---

## 7. Configuration Design

### 7.1 application.yml Additions

```yaml
ticketbox:
  rate-limit:
    enabled: true
    public:
      capacity: 100
      refill-tokens: 100
      refill-duration-seconds: 60
    private:
      capacity: 10
      refill-tokens: 10
      refill-duration-seconds: 60
    whitelisted-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /error
      - /favicon.ico
```

### 7.2 pom.xml Additions

```xml
<!-- Bucket4j Core -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Bucket4j Redis (Lettuce) Integration -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.1</version>
</dependency>
```

---

## 8. Recommended Implementation Plan

### Step 1: Add Dependencies

- Add `bucket4j-core` and `bucket4j-redis` to `pom.xml`
- Verify compatibility with Spring Boot 3.2.4 and Java 17

### Step 2: Create DTOs and Exception Classes

- Create `ErrorResponse` DTO
- Create `RateLimitExceededException`
- Create `GlobalExceptionHandler` (benefits entire project)

### Step 3: Create Rate Limit Configuration

- Create `RateLimitProperties` with `@ConfigurationProperties`
- Add rate limit section to `application.yml`
- Create `RateLimitConfig` with `LettuceBasedProxyManager` bean

### Step 4: Create Rate Limit Key Resolver

- Create `RateLimitKeyResolver`
- Implement IP extraction (with `X-Forwarded-For` support)
- Implement username extraction from `SecurityContext`

### Step 5: Create Rate Limit Filter

- Create `RateLimitFilter` extending `OncePerRequestFilter`
- Implement whitelist check
- Implement bucket resolution and token consumption
- Implement HTTP 429 response writing
- Add fail-open behavior for Redis failures

### Step 6: Register Filter in Security Config

- Modify `SecurityConfig.java` to add `RateLimitFilter` after `JwtAuthenticationFilter`

### Step 7: Test

- Unit test: `RateLimitKeyResolver` logic
- Integration test: Filter behavior with mock Redis
- Manual test: Verify HTTP 429 response format
- Load test: Verify rate limits work correctly under concurrent requests

---

## 9. Architectural Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Filter position | After JWT filter | Enables both IP-based and user-based rate limiting in a single filter |
| Rate limit key for private tier | Username (not user ID) | Username is available from JWT without DB query |
| Redis failure strategy | Fail-open | Prefer availability over strictness — log warning and allow requests |
| Bucket storage | Redis via Bucket4j ProxyManager | Distributed, auto-expiring, atomic operations |
| Configuration approach | @ConfigurationProperties + YAML | Externalized, type-safe, no code changes to adjust limits |
| Error response format | Custom ErrorResponse DTO | Consistent across all error types |

---

## 10. Files To Create

| File | Package | Type |
|------|---------|------|
| `RateLimitFilter.java` | `security.ratelimit` | Filter |
| `RateLimitConfig.java` | `security.ratelimit` | Configuration |
| `RateLimitProperties.java` | `security.ratelimit` | Properties |
| `RateLimitKeyResolver.java` | `security.ratelimit` | Component |
| `RateLimitExceededException.java` | `exception` | Exception |
| `GlobalExceptionHandler.java` | `exception` | Controller Advice |
| `ErrorResponse.java` | `dto` | DTO |

## 11. Files To Modify

| File | Change |
|------|--------|
| `pom.xml` | Add Bucket4j dependencies |
| `application.yml` | Add rate limit configuration block |
| `SecurityConfig.java` | Register `RateLimitFilter` in filter chain |

---

## 12. Dependencies

| Dependency | Owner | Status |
|-----------|-------|--------|
| Spring Boot project initialized | Member 1 | ✅ Complete |
| Spring Security configured | Member 1 | ✅ Complete |
| JWT filter implemented | Member 1 | ✅ Complete |
| Redis configured | Member 1 | ✅ Complete |
| `GlobalExceptionHandler` | Member 4 | ❌ To create (benefits entire project) |

---

## 13. Open Questions

1. **Should auth endpoints have separate, stricter limits?** Login/register endpoints are prime targets for brute-force attacks. Consider a separate tier: e.g., 20 req/min for `/api/auth/**`.

2. **Should rate limit headers be returned on ALL successful requests?** Adding `X-Rate-Limit-Remaining` and `X-Rate-Limit-Reset` to every response helps clients self-throttle. Adds slight overhead.

3. **Will Member 1 add more public endpoints?** The current backend only has `/api/auth/**` and `/api/public/**` as public. If concert browsing endpoints are added, they need the public tier.

4. **Is `Retry-After` header required?** Recommended by HTTP spec (RFC 6585) but optional. Included in this design.

---

## Remaining Work

- [ ] Obtain approval for this design
- [ ] Implement the 7 new files
- [ ] Modify the 3 existing files
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update progress_tracker.md and current_project_state.md
