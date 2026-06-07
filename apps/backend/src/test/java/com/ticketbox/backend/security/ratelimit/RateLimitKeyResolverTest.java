package com.ticketbox.backend.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitKeyResolver}.
 * <p>
 * Covers:
 * <ul>
 *   <li>Authenticated user → private tier (username key)</li>
 *   <li>Unauthenticated / anonymous → public tier (IP key)</li>
 *   <li>IP extraction: trustProxyHeaders=false (Security Fix 2.1)</li>
 *   <li>IP extraction: trustProxyHeaders=true (proxy-aware mode)</li>
 *   <li>resolveIpKey() for pre-auth gate and auth-tier</li>
 * </ul>
 */
class RateLimitKeyResolverTest {

    private RateLimitProperties properties;
    private RateLimitKeyResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        resolver = new RateLimitKeyResolver(properties);
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Authenticated requests → private tier (username-based key)
    // =========================================================================

    @Nested
    @DisplayName("Authenticated requests")
    class AuthenticatedRequests {

        @Test
        @DisplayName("Returns ratelimit:user: key for authenticated user")
        void authenticatedUser_returnsUserKey() {
            authenticateAs("john_doe");
            HttpServletRequest request = mockRequest("127.0.0.1", null);

            assertThat(resolver.resolve(request)).isEqualTo("ratelimit:user:john_doe");
        }

        @Test
        @DisplayName("Returns ratelimit:user: key for organizer role")
        void authenticatedOrganizer_returnsUserKey() {
            authenticateAs("organizer_user");
            HttpServletRequest request = mockRequest("10.0.0.1", null);

            assertThat(resolver.resolve(request))
                    .isEqualTo("ratelimit:user:organizer_user");
        }
    }

    // =========================================================================
    // Unauthenticated requests → public tier (IP-based key)
    // =========================================================================

    @Nested
    @DisplayName("Unauthenticated requests")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("No auth in context → returns ratelimit:ip: key")
        void noAuthentication_returnsIpKey() {
            HttpServletRequest request = mockRequest("192.168.1.100", null);

            assertThat(resolver.resolve(request)).isEqualTo("ratelimit:ip:192.168.1.100");
        }

        @Test
        @DisplayName("Anonymous principal → falls back to IP key")
        void anonymousPrincipal_returnsIpKey() {
            setAnonymousAuthentication();
            HttpServletRequest request = mockRequest("172.16.0.2", null);

            assertThat(resolver.resolve(request)).startsWith("ratelimit:ip:");
        }
    }

    // =========================================================================
    // IP Spoofing Protection — trustProxyHeaders=false (default)
    // Security Fix 2.1
    // =========================================================================

    @Nested
    @DisplayName("IP extraction — trustProxyHeaders=false (default, secure)")
    class TrustProxyHeadersFalse {

        @BeforeEach
        void disableProxyTrust() {
            properties.setTrustProxyHeaders(false);
        }

        @Test
        @DisplayName("X-Forwarded-For is IGNORED — uses remoteAddr only")
        void xForwardedFor_isIgnored_whenProxyTrustDisabled() {
            HttpServletRequest request = mockRequest("10.0.0.1", "203.0.113.195");

            // Must NOT use the X-Forwarded-For value — spoofing is prevented
            assertThat(resolver.extractClientIp(request)).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("Spoofed X-Forwarded-For with chain — still uses remoteAddr")
        void spoofedXForwardedForChain_stillUsesRemoteAddr() {
            HttpServletRequest request = mockRequest("10.0.0.1",
                    "8.8.8.8, 1.1.1.1, 203.0.113.195");

            assertThat(resolver.extractClientIp(request)).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("No X-Forwarded-For header → uses remoteAddr")
        void noXForwardedFor_usesRemoteAddr() {
            HttpServletRequest request = mockRequest("172.16.0.1", null);

            assertThat(resolver.extractClientIp(request)).isEqualTo("172.16.0.1");
        }
    }

    // =========================================================================
    // IP extraction — trustProxyHeaders=true (proxy-aware mode)
    // =========================================================================

    @Nested
    @DisplayName("IP extraction — trustProxyHeaders=true (proxy mode)")
    class TrustProxyHeadersTrue {

        @BeforeEach
        void enableProxyTrust() {
            properties.setTrustProxyHeaders(true);
        }

        @Test
        @DisplayName("X-Forwarded-For header used when proxy trust enabled")
        void xForwardedFor_usedWhenProxyTrustEnabled() {
            HttpServletRequest request = mockRequest("10.0.0.1", "203.0.113.195");

            assertThat(resolver.extractClientIp(request)).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("X-Forwarded-For chain → returns leftmost (original client) IP")
        void xForwardedForChain_returnsFirstIp() {
            HttpServletRequest request = mockRequest("10.0.0.1",
                    "203.0.113.195, 70.41.3.18, 150.172.238.178");

            assertThat(resolver.extractClientIp(request)).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("Missing X-Forwarded-For → falls back to remoteAddr")
        void missingXForwardedFor_fallsBackToRemoteAddr() {
            HttpServletRequest request = mockRequest("172.16.0.1", null);

            assertThat(resolver.extractClientIp(request)).isEqualTo("172.16.0.1");
        }
    }

    // =========================================================================
    // resolveIpKey() — Pre-auth gate and auth-tier
    // =========================================================================

    @Nested
    @DisplayName("resolveIpKey() — always returns IP key regardless of auth state")
    class ResolveIpKey {

        @Test
        @DisplayName("Authenticated user still gets IP key from resolveIpKey()")
        void authenticatedUser_resolveIpKey_returnsIp() {
            authenticateAs("alice");
            HttpServletRequest request = mockRequest("192.168.1.5", null);

            // resolveIpKey() ignores SecurityContext — used for auth-tier and pre-auth
            assertThat(resolver.resolveIpKey(request)).isEqualTo("ratelimit:ip:192.168.1.5");
        }

        @Test
        @DisplayName("Unauthenticated request resolveIpKey() returns IP key")
        void unauthenticated_resolveIpKey_returnsIp() {
            HttpServletRequest request = mockRequest("10.1.1.1", null);

            assertThat(resolver.resolveIpKey(request)).isEqualTo("ratelimit:ip:10.1.1.1");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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

    private HttpServletRequest mockRequest(String remoteAddr, String xForwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        return request;
    }
}
