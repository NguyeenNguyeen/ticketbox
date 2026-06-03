package com.ticketbox.backend.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitKeyResolver}.
 * <p>
 * Verifies that the correct bucket key is produced for authenticated users
 * (private tier: username-based) and unauthenticated requests (public tier: IP-based).
 */
class RateLimitKeyResolverTest {

    private RateLimitKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RateLimitKeyResolver();
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Authenticated requests → private tier (username-based key)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Authenticated user returns ratelimit:user: key")
    void authenticatedUser_returnsUserKey() {
        // Arrange
        authenticateAs("john_doe");
        HttpServletRequest request = mockRequest("127.0.0.1", null);

        // Act
        String key = resolver.resolve(request);

        // Assert
        assertThat(key).isEqualTo("ratelimit:user:john_doe");
    }

    @Test
    @DisplayName("Authenticated user with organizer role returns ratelimit:user: key")
    void authenticatedOrganizer_returnsUserKey() {
        authenticateAs("organizer_user");
        HttpServletRequest request = mockRequest("10.0.0.1", null);

        String key = resolver.resolve(request);

        assertThat(key).startsWith(RateLimitKeyResolver.KEY_PREFIX_USER);
        assertThat(key).isEqualTo("ratelimit:user:organizer_user");
    }

    // -------------------------------------------------------------------------
    // Unauthenticated requests → public tier (IP-based key)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated request returns ratelimit:ip: key with remoteAddr")
    void unauthenticatedRequest_returnsIpKey() {
        // No authentication in SecurityContext
        HttpServletRequest request = mockRequest("192.168.1.100", null);

        String key = resolver.resolve(request);

        assertThat(key).isEqualTo("ratelimit:ip:192.168.1.100");
    }

    @Test
    @DisplayName("X-Forwarded-For header takes priority over remoteAddr")
    void xForwardedFor_takePriorityOverRemoteAddr() {
        HttpServletRequest request = mockRequest("10.0.0.1", "203.0.113.195");

        String ip = resolver.extractClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("X-Forwarded-For with proxy chain returns leftmost (original client) IP")
    void xForwardedForChain_returnsFirstIp() {
        HttpServletRequest request = mockRequest("10.0.0.1", "203.0.113.195, 70.41.3.18, 150.172.238.178");

        String ip = resolver.extractClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Missing JWT falls back to IP-based key")
    void missingJwt_returnsIpKey() {
        // SecurityContext empty (no JWT processed)
        HttpServletRequest request = mockRequest("172.16.0.1", null);

        String key = resolver.resolve(request);

        assertThat(key).isEqualTo("ratelimit:ip:172.16.0.1");
    }

    @Test
    @DisplayName("Invalid JWT (anonymous principal) falls back to IP-based key")
    void invalidJwt_anonymousPrincipal_returnsIpKey() {
        // Simulate anonymous session set by Spring Security
        UsernamePasswordAuthenticationToken anon = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        HttpServletRequest request = mockRequest("172.16.0.2", null);

        String key = resolver.resolve(request);

        assertThat(key).startsWith(RateLimitKeyResolver.KEY_PREFIX_IP);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private HttpServletRequest mockRequest(String remoteAddr, String xForwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        return request;
    }
}
