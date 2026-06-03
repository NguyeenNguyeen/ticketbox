# 02 — API Protection Implementation (Rate Limiting)

**Task:** Implement the Rate Limiting module  
**Author:** Member 4 (Infrastructure & Platform Protection)  
**Created:** 2026-06-03  
**Status:** ✅ Complete — All 16 tests passing, compilation verified

---

## Summary

The Token Bucket rate limiting module has been fully implemented and tested.
It is integrated into the Spring Security filter chain and backed by Redis via Bucket4j.

All implementation decisions follow the approved design in `01_api_protection_design.md`.
No architecture was redesigned.

---

## 1. Build Verification

```
BUILD SUCCESS
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
  - RateLimitKeyResolverTest : 7 tests passed
  - RateLimitFilterTest      : 9 tests passed
```

Maven compiler: Spring Boot 3.2.4 / Java 17  
Maven binary used: IntelliJ bundled Maven at `/home/daonguyennguyen/Downloads/applications/idea-IU-253.32098.37/plugins/maven/lib/maven3/bin/mvn`

---

## 2. Files Created

| File | Package | Purpose |
|------|---------|---------|
| `ErrorResponse.java` | `com.ticketbox.backend.dto` | Standard JSON error DTO with fluent builder |
| `RateLimitExceededException.java` | `com.ticketbox.backend.exception` | Carries retry-after seconds for HTTP 429 |
| `GlobalExceptionHandler.java` | `com.ticketbox.backend.exception` | @RestControllerAdvice — unified error format for all controllers |
| `RateLimitProperties.java` | `com.ticketbox.backend.security.ratelimit` | @ConfigurationProperties bound to `ticketbox.rate-limit.*` |
| `RateLimitConfig.java` | `com.ticketbox.backend.security.ratelimit` | Dedicated Lettuce client + ProxyManager<String> bean |
| `RateLimitKeyResolver.java` | `com.ticketbox.backend.security.ratelimit` | Resolves rate limit key from SecurityContext or IP |
| `RateLimitFilter.java` | `com.ticketbox.backend.security.ratelimit` | OncePerRequestFilter — core rate limiting logic |

### Test Files Created

| File | Tests | Purpose |
|------|-------|---------|
| `RateLimitKeyResolverTest.java` | 7 | Unit tests for key resolution logic |
| `RateLimitFilterTest.java` | 9 | Unit tests for filter behavior (no Redis needed) |

---

## 3. Files Modified

| File | Change |
|------|--------|
| `pom.xml` | Added `bucket4j-core:8.10.1` and `bucket4j-redis:8.10.1` dependencies |
| `application.yml` | Added `ticketbox.rate-limit.*` configuration block |
| `SecurityConfig.java` | Injected `RateLimitFilter` and registered it after `JwtAuthenticationFilter` |

---

## 4. Final Package Structure

```text
com.ticketbox.backend/
├── dto/
│   └── ErrorResponse.java                    ← NEW: JSON error DTO
├── exception/
│   ├── GlobalExceptionHandler.java           ← NEW: @RestControllerAdvice
│   └── RateLimitExceededException.java       ← NEW: carries retryAfterSeconds
├── security/
│   ├── CustomUserDetailsService.java         (unchanged)
│   ├── JwtAuthenticationFilter.java          (unchanged)
│   ├── JwtTokenProvider.java                 (unchanged)
│   ├── SecurityConfig.java                   ← MODIFIED: registers RateLimitFilter
│   └── ratelimit/                            ← NEW package
│       ├── RateLimitConfig.java              ← NEW: Bucket4j ProxyManager bean
│       ├── RateLimitFilter.java              ← NEW: core filter
│       ├── RateLimitKeyResolver.java         ← NEW: key resolution
│       └── RateLimitProperties.java          ← NEW: @ConfigurationProperties
└── (all other packages unchanged)
```

---

## 5. Filter Chain Order

```
HTTP Request
    │
    ▼
[JwtAuthenticationFilter]
    │  ← Parses Bearer token, sets SecurityContextHolder
    ▼
[RateLimitFilter]           ← NEW (after JWT, before Authorization)
    │  ← Checks whitelist → resolves key → checks Redis bucket
    │  ← HTTP 429 if limit exceeded, otherwise continue
    ▼
[UsernamePasswordAuthenticationFilter]
    │
    ▼
[AuthorizationFilter]       ← RBAC checks (hasRole etc.)
    │
    ▼
Controller
```

**Registration in SecurityConfig.java:**
```java
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
http.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
```

---

## 6. Rate Limit Behavior

### Two-Tier Strategy

| Tier | Condition | Key Format | Default Limit |
|------|-----------|-----------|---------------|
| Private | Authenticated (valid JWT) | `ratelimit:user:{username}` | 10 req/min |
| Public | Unauthenticated or anonymous | `ratelimit:ip:{clientIp}` | 100 req/min |

### Key Resolution Logic

```
Request arrives at RateLimitFilter
    │
    ├── Is SecurityContext authenticated AND principal != "anonymousUser"?
    │        YES → key = "ratelimit:user:" + authentication.getName()   [Private tier]
    │        NO  → key = "ratelimit:ip:" + extractClientIp(request)     [Public tier]
    │
    └── IP extraction:
             X-Forwarded-For header present?
                  YES → take leftmost IP (original client)
                  NO  → request.getRemoteAddr()
```

### HTTP 429 Response Format

```json
{
    "status": 429,
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "retryAfterSeconds": 30
}
```

Response headers set:
- `Retry-After: 30`
- `Content-Type: application/json`

---

## 7. Configuration Properties

```yaml
ticketbox:
  rate-limit:
    enabled: true           # Set false to disable entirely (e.g. local dev)
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
      - /swagger-ui.html
      - /v3/api-docs/**
      - /error
      - /favicon.ico
      - /static/**
```

To change limits without redeployment: update `application.yml` and restart.

---

## 8. Redis Key Strategy

| Scenario | Redis Key | TTL |
|----------|----------|-----|
| IP `203.0.113.1` | `ratelimit:ip:203.0.113.1` | ~60s (auto by Bucket4j) |
| User `john_doe` | `ratelimit:user:john_doe` | ~60s (auto by Bucket4j) |
| User `alice` (ORGANIZER) | `ratelimit:user:alice` | ~60s (auto by Bucket4j) |

Bucket4j's `LettuceBasedProxyManager` automatically sets Redis key TTL based on the bucket's refill period. Keys for inactive users/IPs expire automatically, preventing unbounded Redis memory growth.

---

## 9. Bucket4j Integration Details

### Why a Separate Lettuce Client?

`RateLimitConfig` creates a dedicated `RedisClient` and `StatefulRedisConnection<String, byte[]>` for Bucket4j instead of reusing Spring Data Redis's auto-configured `LettuceConnectionFactory`.

**Reason:** Bucket4j requires a byte-array value codec (`ByteArrayCodec`) to store bucket state. Spring Data Redis's default connection uses `StringCodec`. Mixing codecs on the same connection causes serialization conflicts. The dedicated client resolves this cleanly without modifying existing Redis infrastructure.

### Atomic Operations

Bucket4j uses Redis **Lua scripts** for atomic token consumption:
1. Check remaining tokens
2. Deduct 1 token if available
3. Return remaining count + wait time

All in a single atomic Redis call. No race conditions between concurrent requests.

### Build() Overload Disambiguation

Bucket4j 8.x has two `RemoteBucketBuilder.build()` overloads:
- `build(K key, BucketConfiguration config)`
- `build(K key, Supplier<BucketConfiguration> configSupplier)`

The filter uses the `Supplier` form with an explicit cast to avoid ambiguity:
```java
final BucketConfiguration config = bucketConfig;
BucketProxy bucket = proxyManager.builder()
    .build(bucketKey, (java.util.function.Supplier<BucketConfiguration>) () -> config);
```

---

## 10. Edge Case Handling

| Scenario | Behavior | Implemented |
|----------|---------|-------------|
| Missing JWT | Falls back to IP-based (public tier) | ✅ |
| Invalid JWT | Falls back to IP-based (public tier) | ✅ |
| Anonymous session | Falls back to IP-based (anonymous ≠ authenticated) | ✅ |
| Redis unavailable | **Fail-open**: log warning, allow request | ✅ |
| Actuator endpoints | Whitelisted — skipped entirely | ✅ |
| Swagger endpoints | Whitelisted — skipped entirely | ✅ |
| Static resources | Whitelisted — skipped entirely | ✅ |
| Async dispatch | `shouldNotFilterAsyncDispatch() = true` — skipped | ✅ |
| Error dispatch | `shouldNotFilterErrorDispatch() = true` — skipped | ✅ |
| Rate limiting disabled | `enabled: false` — skipped entirely | ✅ |
| RabbitMQ consumers | Not HTTP — filter chain not involved | N/A |
| Scheduled jobs | Not HTTP — filter chain not involved | N/A |

---

## 11. Testing Strategy

### Unit Tests (No Spring Context, No Redis)

All tests run without a running database, Redis, or Spring Application Context. They use:
- `MockHttpServletRequest` / `MockHttpServletResponse` from `spring-mock`
- Mockito mocks for `ProxyManager<String>` and `BucketProxy`
- Manual `SecurityContextHolder` manipulation

### Test Coverage

| Test Class | Tests | Scenarios Covered |
|-----------|-------|------------------|
| `RateLimitKeyResolverTest` | 7 | Authenticated user, organizer, unauthenticated, X-Forwarded-For, proxy chain, missing JWT, anonymous principal |
| `RateLimitFilterTest` | 9 | Public under limit, public over limit, private under limit, private over limit, missing JWT, invalid JWT, Redis failure (fail-open), whitelisted path, disabled globally |

---

## 12. Remaining Risks

| Risk | Severity | Status | Notes |
|------|----------|--------|-------|
| IP spoofing via X-Forwarded-For | Medium | ⚠️ Documented | Acceptable for development. For production: configure trusted proxy list with `ForwardedHeaderFilter`. |
| Auth endpoints not separately limited | Low | ⚠️ Open | `/api/auth/**` uses public tier (100/min by IP). Brute-force protection is limited. Consider a separate stricter tier in a future iteration. |
| Lettuce connection leak on shutdown | Low | ✅ Mitigated | `@Bean(destroyMethod = "shutdown")` and `@Bean(destroyMethod = "close")` ensure clean shutdown. |
| Bucket4j 8.x API changes in future | Low | ✅ Documented | Pin version in pom.xml (8.10.1). Do not upgrade without testing. |

---

## 13. What a New Agent Needs to Know

1. **Rate limiting is live** — all requests go through `RateLimitFilter` after `JwtAuthenticationFilter`
2. **Redis keys to monitor:** `ratelimit:ip:*` (public tier) and `ratelimit:user:*` (private tier)
3. **To adjust limits:** Edit `ticketbox.rate-limit.*` in `application.yml`, restart the app
4. **To disable:** Set `ticketbox.rate-limit.enabled: false`
5. **To add a new whitelist path:** Add to `ticketbox.rate-limit.whitelisted-paths` in `application.yml`
6. **Tests location:** `src/test/java/com/ticketbox/backend/security/ratelimit/`
7. **No DB call on every request** — username comes from `SecurityContextHolder`, no `UserRepository` query

---

## 14. Next Recommended Tasks

1. **Payment Protection** — Resilience4j Circuit Breaker + Bulkhead for outbound payment calls (Phase 2, next Member 4 task)
2. **Docker Compose fix** — PostgreSQL healthcheck still has empty `-U -d` flags
3. **RabbitMQ Spring AMQP configuration** — Phase 3, needed before workers
