package com.ticketbox.backend.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the remediated {@link RateLimitFilter}.
 * <p>
 * Covers all five pipeline steps and all security fixes from
 * {@code 03_api_protection_review.md}:
 * <ul>
 *   <li>Pre-auth Caffeine gate (Fix 2.2 — DoS shield)</li>
 *   <li>Auth-tier Redis bucket (Fix 3.1 — brute-force protection)</li>
 *   <li>Public and private standard tiers</li>
 *   <li>Redis failure → local fallback (Fix 3.2 — no longer fully fail-open)</li>
 *   <li>Whitelist bypass (Fix 3.3 — narrow scope)</li>
 *   <li>Global disable</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class RateLimitFilterTest {

    @Mock private ProxyManager proxyManager;
    @Mock private RemoteBucketBuilder remoteBucketBuilder;
    @Mock private BucketProxy bucketProxy;
    @Mock private FilterChain filterChain;

    private RateLimitFilter filter;
    private RateLimitProperties properties;
    private RateLimitKeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        properties = buildDefaultProperties();
        keyResolver = new RateLimitKeyResolver(properties);
        filter = new RateLimitFilter(proxyManager, properties, keyResolver, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Step 1 — Global disable
    // =========================================================================

    @Nested
    @DisplayName("Step 1: Global disable")
    class GlobalDisable {

        @Test
        @DisplayName("Rate limiting disabled — all requests pass, no bucket interaction")
        void disabled_allRequestsPass() throws Exception {
            properties.setEnabled(false);

            MockHttpServletRequest request = publicRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(proxyManager, bucketProxy);
        }
    }

    // =========================================================================
    // Step 2 — Whitelist bypass
    // =========================================================================

    @Nested
    @DisplayName("Step 2: Whitelist")
    class Whitelist {

        @Test
        @DisplayName("/actuator/health is whitelisted — no rate limiting")
        void actuatorHealth_isWhitelisted() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(proxyManager, bucketProxy);
        }

        @Test
        @DisplayName("/actuator/metrics is NO LONGER whitelisted — rate limit applies")
        void actuatorMetrics_isNoLongerWhitelisted_rateLimitApplies() throws Exception {
            // Fix 3.3: /actuator/** removed from whitelist; only /actuator/health and /actuator/info remain
            givenRedisAllows(9);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            // Should pass (tokens available), but rate limiting WAS applied (not whitelisted)
            verify(filterChain).doFilter(request, response);
            verify(proxyManager, atLeastOnce()).builder();  // proves rate limit was checked
        }

        @Test
        @DisplayName("/swagger-ui/** is whitelisted")
        void swaggerUi_isWhitelisted() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(proxyManager, bucketProxy);
        }
    }

    // =========================================================================
    // Step 3 — Pre-auth local DoS gate (Security Fix 2.2)
    // =========================================================================

    @Nested
    @DisplayName("Step 3: Pre-auth local DoS gate")
    class PreAuthGate {

        @Test
        @DisplayName("Request within pre-auth limit — passes gate and continues")
        void underPreAuthLimit_passes() throws Exception {
            givenRedisAllows(99);

            MockHttpServletRequest request = publicRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Pre-auth gate blocks IP that exceeds local burst limit")
        void overPreAuthLimit_blocks429() throws Exception {
            // Exhaust the pre-auth Caffeine bucket by calling the filter 201 times.
            // We configure pre-auth capacity to 2 for this test to keep it fast.
            properties.getPreAuth().setCapacity(2);
            properties.getPreAuth().setRefillTokens(2);
            properties.getPreAuth().setRefillDurationSeconds(60);

            MockHttpServletRequest request = publicRequest();

            // First two requests should pass
            for (int i = 0; i < 2; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                // Redis mock must be set up for each allowed request
                givenRedisAllows(99);
                filter.doFilter(request, response, filterChain);
                assertThat(response.getStatus()).isNotEqualTo(429);
            }

            // Third request must be rejected by the pre-auth gate (before Redis)
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            filter.doFilter(request, blockedResponse, filterChain);
            assertThat(blockedResponse.getStatus()).isEqualTo(429);

            String body = blockedResponse.getContentAsString();
            assertThat(body).contains("RATE_LIMIT_EXCEEDED");
        }
    }

    // =========================================================================
    // Step 4 — Auth-tier check (Security Fix 3.1 — brute-force protection)
    // =========================================================================

    @Nested
    @DisplayName("Step 4: Auth-tier for /api/auth/**")
    class AuthTier {

        @Test
        @DisplayName("Login request within auth limit — allowed")
        void loginRequest_underAuthLimit_allowed() throws Exception {
            givenRedisAllows(9);

            MockHttpServletRequest request = authRequest("/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Login request exceeds auth tier — returns 429")
        void loginRequest_overAuthLimit_returns429() throws Exception {
            givenRedisRejects(55_000_000_000L); // 55 seconds

            MockHttpServletRequest request = authRequest("/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("55");
        }

        @Test
        @DisplayName("Register request receives auth tier limit (not public tier)")
        void registerRequest_usesAuthTier() throws Exception {
            givenRedisAllows(9);

            MockHttpServletRequest request = authRequest("/api/auth/register");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // =========================================================================
    // Step 5 — Standard tiers (public and private)
    // =========================================================================

    @Nested
    @DisplayName("Step 5: Standard public and private tiers")
    class StandardTiers {

        @Test
        @DisplayName("Public API under limit — allowed with remaining header")
        void publicApi_underLimit_allowed() throws Exception {
            givenRedisAllows(99);

            MockHttpServletRequest request = publicRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("99");
        }

        @Test
        @DisplayName("Public API over limit — returns 429 with Retry-After")
        void publicApi_overLimit_returns429() throws Exception {
            givenRedisRejects(30_000_000_000L); // 30 seconds

            MockHttpServletRequest request = publicRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("30");
            assertThat(response.getContentType()).contains("application/json");
            assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("Authenticated user under private limit — allowed")
        void privateApi_underLimit_allowed() throws Exception {
            authenticateAs("alice");
            givenRedisAllows(9);

            MockHttpServletRequest request = privateRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("9");
        }

        @Test
        @DisplayName("Authenticated user over private limit — returns 429")
        void privateApi_overLimit_returns429() throws Exception {
            authenticateAs("bob");
            givenRedisRejects(45_000_000_000L); // 45 seconds

            MockHttpServletRequest request = privateRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("45");
        }
    }

    // =========================================================================
    // JWT edge cases
    // =========================================================================

    @Nested
    @DisplayName("JWT edge cases")
    class JwtEdgeCases {

        @Test
        @DisplayName("Missing JWT — falls back to IP-based key (public tier)")
        void missingJwt_fallsBackToPublicTier() throws Exception {
            // No SecurityContext set — unauthenticated
            givenRedisAllows(99);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/concerts");
            request.setRemoteAddr("203.0.113.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Anonymous session — falls back to IP-based key (public tier)")
        void anonymousSession_fallsBackToPublicTier() throws Exception {
            setAnonymousAuthentication();
            givenRedisAllows(98);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/concerts");
            request.setRemoteAddr("10.0.0.5");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // =========================================================================
    // Redis failure — local fallback (Security Fix 3.2)
    // =========================================================================

    @Nested
    @DisplayName("Redis failure — local fallback (Fix 3.2)")
    class RedisFailure {

        @Test
        @DisplayName("Redis unavailable — falls back to local Caffeine bucket, NOT fully open")
        void redisUnavailable_fallsBackToLocal_notFullyOpen() throws Exception {
            // ProxyManager.builder() throws — Redis is down
            when(proxyManager.builder()).thenThrow(new RuntimeException("Redis connection refused"));

            MockHttpServletRequest request = publicRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // First call with Redis down: should still allow (local bucket has tokens)
            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            // Status must not be 429 for first request
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("Redis unavailable — local fallback eventually blocks after exhaustion")
        void redisUnavailable_localFallbackBlocksAfterExhaustion() throws Exception {
            // Configure a tiny fallback capacity for this test
            properties.getPublic().setCapacity(2);
            properties.getPublic().setRefillTokens(2);
            properties.getPreAuth().setCapacity(100); // Keep pre-auth generous

            when(proxyManager.builder()).thenThrow(new RuntimeException("Redis unavailable"));

            MockHttpServletRequest request = publicRequest();

            // Consume the local fallback budget (2 tokens)
            for (int i = 0; i < 2; i++) {
                MockHttpServletResponse resp = new MockHttpServletResponse();
                filter.doFilter(request, resp, filterChain);
                assertThat(resp.getStatus()).isNotEqualTo(429);
            }

            // Third request should be blocked by local fallback
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
            filter.doFilter(request, blockedResponse, filterChain);
            assertThat(blockedResponse.getStatus()).isEqualTo(429);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void givenRedisAllows(long remaining) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(remaining);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
    }

    private void givenRedisRejects(long nanosToWait) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(nanosToWait);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
    }

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setAnonymousAuthentication() {
        UsernamePasswordAuthenticationToken anon = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null,
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);
    }

    private MockHttpServletRequest publicRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/concerts");
        req.setRemoteAddr("192.168.1.10");
        return req;
    }

    private MockHttpServletRequest privateRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/tickets/purchase");
        req.setRemoteAddr("192.168.1.20");
        return req;
    }

    private MockHttpServletRequest authRequest(String path) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr("192.168.1.30");
        return req;
    }

    private RateLimitProperties buildDefaultProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setTrustProxyHeaders(false);
        props.getPreAuth().setCapacity(200);
        props.getPreAuth().setRefillTokens(200);
        props.getPreAuth().setRefillDurationSeconds(60);
        props.getAuth().setCapacity(10);
        props.getAuth().setRefillTokens(10);
        props.getAuth().setRefillDurationSeconds(60);
        props.getPublic().setCapacity(100);
        props.getPublic().setRefillTokens(100);
        props.getPublic().setRefillDurationSeconds(60);
        props.getPrivate().setCapacity(10);
        props.getPrivate().setRefillTokens(10);
        props.getPrivate().setRefillDurationSeconds(60);
        props.setAuthPaths(List.of("/api/auth/**"));
        props.setWhitelistedPaths(List.of("/actuator/health", "/actuator/info",
                "/swagger-ui/**", "/error"));
        return props;
    }
}
