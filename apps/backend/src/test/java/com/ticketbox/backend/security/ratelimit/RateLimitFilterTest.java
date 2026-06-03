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
 * Unit tests for {@link RateLimitFilter}.
 * <p>
 * Uses Mockito to simulate Bucket4j's {@link ProxyManager} — no real Redis needed.
 * Covers the 9 scenarios specified in the rate limiting design document.
 * <p>
 * Implementation notes:
 * - {@link ProxyManager} and {@link RemoteBucketBuilder} are mocked with raw types
 *   to avoid generic capture issues with Mockito.
 * - The {@code build()} overload is disambiguated by casting to
 *   {@code Supplier<BucketConfiguration>} matching the filter's production call.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class RateLimitFilterTest {

    @Mock
    private ProxyManager proxyManager;

    @Mock
    private RemoteBucketBuilder remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = buildDefaultProperties();
        RateLimitKeyResolver keyResolver = new RateLimitKeyResolver();
        ObjectMapper objectMapper = new ObjectMapper();

        filter = new RateLimitFilter(proxyManager, properties, keyResolver, objectMapper);
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Test 1: Public API — under limit → allowed (HTTP 200)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. Public API under limit — request passes through")
    void publicApi_underLimit_allowed() throws Exception {
        givenBucketHasTokens(99);

        MockHttpServletRequest request = publicRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("99");
    }

    // -------------------------------------------------------------------------
    // Test 2: Public API — over limit → HTTP 429
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. Public API over limit — HTTP 429 returned")
    void publicApi_overLimit_returns429() throws Exception {
        givenBucketIsEmpty(30_000_000_000L); // 30 seconds in nanoseconds

        MockHttpServletRequest request = publicRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentType()).contains("application/json");

        String body = response.getContentAsString();
        assertThat(body).contains("RATE_LIMIT_EXCEEDED");
        assertThat(body).contains("\"status\":429");
    }

    // -------------------------------------------------------------------------
    // Test 3: Private API — under limit → allowed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. Private API (authenticated) under limit — request passes through")
    void privateApi_underLimit_allowed() throws Exception {
        authenticateAs("alice");
        givenBucketHasTokens(9);

        MockHttpServletRequest request = privateRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("9");
    }

    // -------------------------------------------------------------------------
    // Test 4: Private API — over limit → HTTP 429
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. Private API (authenticated) over limit — HTTP 429 returned")
    void privateApi_overLimit_returns429() throws Exception {
        authenticateAs("bob");
        givenBucketIsEmpty(45_000_000_000L); // 45 seconds

        MockHttpServletRequest request = privateRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("45");

        String body = response.getContentAsString();
        assertThat(body).contains("RATE_LIMIT_EXCEEDED");
    }

    // -------------------------------------------------------------------------
    // Test 5: Missing JWT → falls back to IP-based key → request allowed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. Missing JWT — falls back to IP-based key, request allowed")
    void missingJwt_fallsBackToIp_allowed() throws Exception {
        // No authentication in SecurityContextHolder
        givenBucketHasTokens(99);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/concerts");
        request.setRemoteAddr("203.0.113.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Test 6: Invalid JWT → anonymous principal → IP-based key
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. Invalid JWT (anonymous principal) — falls back to IP-based key")
    void invalidJwt_anonymousPrincipal_fallsBackToIp() throws Exception {
        setAnonymousAuthentication();
        givenBucketHasTokens(98);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Must use IP-based key (public tier) — request is allowed
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Test 7: Redis unavailable → fail-open → request allowed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. Redis unavailable — fail-open: request is allowed through")
    void redisUnavailable_failOpen_requestAllowed() throws Exception {
        // ProxyManager.builder() throws when Redis is unreachable
        when(proxyManager.builder()).thenThrow(new RuntimeException("Redis connection refused"));

        MockHttpServletRequest request = publicRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Should NOT throw — must fail open and pass request through
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    // -------------------------------------------------------------------------
    // Test 8: Whitelisted path → skips rate limiting entirely
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. Whitelisted path (/actuator/health) — skips rate limiting")
    void whitelistedPath_skipsRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Filter chain continues, ProxyManager never touched
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(proxyManager);
        verifyNoInteractions(bucketProxy);
    }

    // -------------------------------------------------------------------------
    // Test 9: Rate limiting disabled globally → all requests pass through
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("9. Rate limiting disabled globally — all requests pass through")
    void rateLimitingDisabled_allRequestsPass() throws Exception {
        properties.setEnabled(false);

        MockHttpServletRequest request = publicRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(proxyManager);
        verifyNoInteractions(bucketProxy);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Stubs the ProxyManager chain so that bucket.tryConsumeAndReturnRemaining(1)
     * returns a successful probe with {@code remaining} tokens left.
     */
    private void givenBucketHasTokens(long remaining) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(remaining);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        // Disambiguate: match the Supplier<BucketConfiguration> overload used in the filter
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
    }

    /**
     * Stubs the ProxyManager chain so that bucket.tryConsumeAndReturnRemaining(1)
     * returns a rejection probe with {@code nanosToWait} nanoseconds until refill.
     */
    private void givenBucketIsEmpty(long nanosToWait) {
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
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/public/concerts");
        req.setRemoteAddr("192.168.1.10");
        return req;
    }

    private MockHttpServletRequest privateRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/tickets/purchase");
        req.setRemoteAddr("192.168.1.20");
        return req;
    }

    private RateLimitProperties buildDefaultProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.getPublic().setCapacity(100);
        props.getPublic().setRefillTokens(100);
        props.getPublic().setRefillDurationSeconds(60);
        props.getPrivate().setCapacity(10);
        props.getPrivate().setRefillTokens(10);
        props.getPrivate().setRefillDurationSeconds(60);
        props.setWhitelistedPaths(List.of("/actuator/**", "/swagger-ui/**", "/error"));
        return props;
    }
}
