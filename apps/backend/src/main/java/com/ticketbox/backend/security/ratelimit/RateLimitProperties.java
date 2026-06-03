package com.ticketbox.backend.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized rate limit configuration bound from {@code application.yml}.
 * <p>
 * Four rate-limit tiers are defined:
 * <ul>
 *   <li><b>pre-auth</b> — Fast in-memory IP gate applied BEFORE JWT/DB auth. Prevents DoS floods
 *       from exhausting DB connections. Uses a Caffeine local cache (node-level, not distributed).</li>
 *   <li><b>auth</b> — Strict Redis-backed limit for authentication endpoints (login/register).
 *       Prevents credential brute-forcing. Applied per IP.</li>
 *   <li><b>public</b> — Redis-backed limit for unauthenticated requests (non-auth). Applied per IP.</li>
 *   <li><b>private</b> — Redis-backed limit for authenticated requests. Applied per username.</li>
 * </ul>
 * <p>
 * Example YAML:
 * <pre>
 * ticketbox:
 *   rate-limit:
 *     enabled: true
 *     trust-proxy-headers: false
 *     pre-auth:
 *       capacity: 200
 *       refill-tokens: 200
 *       refill-duration-seconds: 60
 *     auth:
 *       capacity: 10
 *       refill-tokens: 10
 *       refill-duration-seconds: 60
 *     public:
 *       capacity: 100
 *       refill-tokens: 100
 *       refill-duration-seconds: 60
 *     private:
 *       capacity: 10
 *       refill-tokens: 10
 *       refill-duration-seconds: 60
 *     auth-paths:
 *       - /api/auth/**
 *     whitelisted-paths:
 *       - /actuator/health
 *       - /actuator/info
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ticketbox.rate-limit")
public class RateLimitProperties {

    /**
     * Global switch. Set to {@code false} to disable rate limiting entirely.
     */
    private boolean enabled = true;

    /**
     * Whether to trust proxy headers (e.g., {@code X-Forwarded-For}) for IP resolution.
     * <p>
     * <b>Security note:</b> Only set to {@code true} when the service runs behind a trusted
     * reverse proxy or load balancer (e.g., Nginx, AWS ALB). When {@code false} (default),
     * {@code HttpServletRequest#getRemoteAddr()} is used exclusively, preventing IP spoofing.
     * <p>
     * When set to {@code true}, also configure {@code server.forward-headers-strategy=framework}
     * so that Tomcat's RemoteIpValve resolves the client IP before this filter runs.
     */
    private boolean trustProxyHeaders = false;

    /**
     * Pre-authentication IP gate. Cheap local in-memory check (Caffeine) executed BEFORE
     * JWT validation and database access. Purpose: prevent floods from exhausting DB connections.
     */
    private TierProperties preAuth = new TierProperties(200, 200, 60);

    /**
     * Authentication endpoint tier. Strict Redis-backed limit for login/register endpoints.
     * Prevents brute-forcing of credentials.
     */
    private TierProperties auth = new TierProperties(10, 10, 60);

    /**
     * Public (unauthenticated, non-auth) tier. Applied per client IP via Redis.
     */
    private TierProperties publicTier = new TierProperties(100, 100, 60);

    /**
     * Private (authenticated) tier. Applied per username via Redis.
     */
    private TierProperties privateTier = new TierProperties(10, 10, 60);

    /**
     * Ant-style path patterns for authentication endpoints.
     * Requests matching these patterns receive the stricter {@link #auth} tier limit.
     */
    private List<String> authPaths = new ArrayList<>();

    /**
     * URI path patterns that completely skip rate limit checks.
     * Should be scoped narrowly (e.g., Docker health probes only).
     */
    private List<String> whitelistedPaths = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Getters / Setters — required by @ConfigurationProperties binding
    // -------------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustProxyHeaders() {
        return trustProxyHeaders;
    }

    public void setTrustProxyHeaders(boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    public TierProperties getPreAuth() {
        return preAuth;
    }

    public void setPreAuth(TierProperties preAuth) {
        this.preAuth = preAuth;
    }

    public TierProperties getAuth() {
        return auth;
    }

    public void setAuth(TierProperties auth) {
        this.auth = auth;
    }

    public TierProperties getPublic() {
        return publicTier;
    }

    public void setPublic(TierProperties publicTier) {
        this.publicTier = publicTier;
    }

    public TierProperties getPrivate() {
        return privateTier;
    }

    public void setPrivate(TierProperties privateTier) {
        this.privateTier = privateTier;
    }

    public List<String> getAuthPaths() {
        return authPaths;
    }

    public void setAuthPaths(List<String> authPaths) {
        this.authPaths = authPaths;
    }

    public List<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }

    public void setWhitelistedPaths(List<String> whitelistedPaths) {
        this.whitelistedPaths = whitelistedPaths;
    }

    // -------------------------------------------------------------------------
    // Nested configuration class
    // -------------------------------------------------------------------------

    /**
     * Holds the token bucket capacity and refill parameters for a single rate limit tier.
     */
    public static class TierProperties {

        /** Maximum number of tokens the bucket can hold. */
        private long capacity;

        /** Number of tokens added to the bucket on each refill cycle. */
        private long refillTokens;

        /** Duration of one refill cycle in seconds. */
        private long refillDurationSeconds;

        public TierProperties() {
        }

        public TierProperties(long capacity, long refillTokens, long refillDurationSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDurationSeconds = refillDurationSeconds;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillDurationSeconds() {
            return refillDurationSeconds;
        }

        public void setRefillDurationSeconds(long refillDurationSeconds) {
            this.refillDurationSeconds = refillDurationSeconds;
        }
    }
}
