package com.ticketbox.backend.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized rate limit configuration bound from {@code application.yml}.
 * <p>
 * Example YAML:
 * <pre>
 * ticketbox:
 *   rate-limit:
 *     enabled: true
 *     public:
 *       capacity: 100
 *       refill-tokens: 100
 *       refill-duration-seconds: 60
 *     private:
 *       capacity: 10
 *       refill-tokens: 10
 *       refill-duration-seconds: 60
 *     whitelisted-paths:
 *       - /actuator/**
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ticketbox.rate-limit")
public class RateLimitProperties {

    /**
     * Global switch. Set to {@code false} to disable rate limiting entirely.
     */
    private boolean enabled = true;

    private TierProperties publicTier = new TierProperties(100, 100, 60);
    private TierProperties privateTier = new TierProperties(10, 10, 60);

    /**
     * URI path patterns that completely skip rate limit checks.
     * Supports Ant-style patterns (e.g. {@code /actuator/**}).
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

    /**
     * Returns the configuration for the public (unauthenticated, IP-based) tier.
     */
    public TierProperties getPublic() {
        return publicTier;
    }

    public void setPublic(TierProperties publicTier) {
        this.publicTier = publicTier;
    }

    /**
     * Returns the configuration for the private (authenticated, username-based) tier.
     */
    public TierProperties getPrivate() {
        return privateTier;
    }

    public void setPrivate(TierProperties privateTier) {
        this.privateTier = privateTier;
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
     * Holds the bucket capacity and refill parameters for a single rate limit tier.
     */
    public static class TierProperties {

        /**
         * Maximum number of tokens the bucket can hold.
         */
        private long capacity;

        /**
         * Number of tokens added to the bucket on each refill cycle.
         */
        private long refillTokens;

        /**
         * Duration of one refill cycle in seconds.
         */
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
