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
 *   <li><b>Public/Auth tier</b>: Unauthenticated request → key = {@code ratelimit:ip:{clientIp}}</li>
 * </ul>
 * <p>
 * <b>IP Spoofing Protection (Security Fix 2.1):</b><br>
 * The {@code X-Forwarded-For} header is ONLY trusted when
 * {@link RateLimitProperties#isTrustProxyHeaders()} is {@code true}.
 * By default ({@code trust-proxy-headers: false}), only {@code request.getRemoteAddr()}
 * is used, which cannot be spoofed at the application level.
 * <p>
 * When {@code trust-proxy-headers: true}, ensure the service is deployed behind a
 * trusted reverse proxy and configure {@code server.forward-headers-strategy=framework}
 * in {@code application.yml} so Tomcat's {@code RemoteIpValve} validates the source
 * before this filter runs.
 */
@Component
public class RateLimitKeyResolver {

    static final String KEY_PREFIX_USER = "ratelimit:user:";
    static final String KEY_PREFIX_IP   = "ratelimit:ip:";

    private final RateLimitProperties properties;

    public RateLimitKeyResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

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
     * Resolves a raw IP-based key regardless of authentication state.
     * Used for the pre-auth local DoS gate and auth-tier checks (both IP-based).
     */
    public String resolveIpKey(HttpServletRequest request) {
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
     * <b>Behaviour depends on {@code trust-proxy-headers}:</b>
     * <ul>
     *   <li>{@code false} (default, secure): Uses {@code request.getRemoteAddr()} only.
     *       Cannot be spoofed. Correct for direct connections or when using
     *       {@code server.forward-headers-strategy=framework} (Tomcat resolves the IP).</li>
     *   <li>{@code true}: Checks {@code X-Forwarded-For} first, then falls back to
     *       {@code getRemoteAddr()}. Only use behind a trusted proxy.</li>
     * </ul>
     */
    String extractClientIp(HttpServletRequest request) {
        if (properties.isTrustProxyHeaders()) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                // X-Forwarded-For may contain a comma-separated list: "client, proxy1, proxy2"
                // The leftmost value is the original client IP.
                return xForwardedFor.split(",")[0].trim();
            }
        }
        // Default: use the direct connection address (cannot be spoofed)
        return request.getRemoteAddr();
    }
}
