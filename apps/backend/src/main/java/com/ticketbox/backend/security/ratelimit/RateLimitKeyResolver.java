package com.ticketbox.backend.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the rate limit bucket key for a given HTTP request.
 * <p>
 * Two strategies are applied depending on whether the request is authenticated:
 * <ul>
 *   <li><b>Private tier</b>: Authenticated request → key = {@code ratelimit:user:{username}}</li>
 *   <li><b>Public tier</b>: Unauthenticated request → key = {@code ratelimit:ip:{clientIp}}</li>
 * </ul>
 * <p>
 * The username is read directly from {@link SecurityContextHolder} — populated earlier
 * in the filter chain by {@code JwtAuthenticationFilter}. This avoids a database
 * round-trip on every request.
 */
@Component
public class RateLimitKeyResolver {

    static final String KEY_PREFIX_USER = "ratelimit:user:";
    static final String KEY_PREFIX_IP   = "ratelimit:ip:";

    /**
     * Determines the rate limit key for the given request.
     *
     * @param request the current HTTP request
     * @return a non-null, non-empty string key for the rate limit bucket
     */
    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAuthenticated(authentication)) {
            return KEY_PREFIX_USER + authentication.getName();
        }

        return KEY_PREFIX_IP + extractClientIp(request);
    }

    /**
     * Returns {@code true} if the authentication object represents a fully
     * authenticated user (not an anonymous session).
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Extracts the real client IP address.
     * <p>
     * Checks the {@code X-Forwarded-For} header first (set by reverse proxies /
     * load balancers), falling back to {@link HttpServletRequest#getRemoteAddr()}.
     * <p>
     * <b>Security note:</b> {@code X-Forwarded-For} can be spoofed if there is no
     * trusted proxy in front of this service. In production, only trust this header
     * from known proxy IP ranges. For the current development environment (direct
     * connections), {@code getRemoteAddr()} is used as the primary source.
     *
     * @param request the current HTTP request
     * @return the resolved client IP address, never null
     */
    String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For may contain a comma-separated list: "client, proxy1, proxy2"
            // The leftmost value is the original client IP.
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
