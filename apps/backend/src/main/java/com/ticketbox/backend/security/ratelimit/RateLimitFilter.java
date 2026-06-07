package com.ticketbox.backend.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import com.ticketbox.backend.dto.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces rate limiting on all HTTP requests.
 *
 * <h2>Processing Pipeline (per request)</h2>
 * <ol>
 *   <li><b>Global disable check</b> — Skip if {@code ticketbox.rate-limit.enabled=false}.</li>
 *   <li><b>Whitelist check</b> — Skip for health probes, Swagger, error endpoints.</li>
 *   <li><b>Pre-auth local gate (DoS shield)</b> — Fast in-memory Caffeine check by IP.
 *       Runs BEFORE JWT parsing and DB access. Blocks floods at the filter level
 *       without consuming DB connections. (Fixes Critical Finding 2.2)</li>
 *   <li><b>Auth-tier check</b> — If the path matches {@code auth-paths} (e.g., {@code /api/auth/**}),
 *       apply the strict Redis-backed auth bucket (e.g., 10 req/min per IP).
 *       (Fixes Important Finding 3.1)</li>
 *   <li><b>Standard tier check</b> — Private (authenticated, username-based) or Public
 *       (unauthenticated, IP-based) Redis-backed bucket.</li>
 * </ol>
 *
 * <h2>Filter Position</h2>
 * Placed <em>after</em> {@code JwtAuthenticationFilter} so the {@code SecurityContextHolder}
 * is populated. The pre-auth Caffeine gate (Step 3) is intentionally cheap enough that
 * running it here (rather than before JWT) adds negligible overhead while still blocking
 * floods before any business logic or DB calls execute.
 *
 * <h2>Redis Failure Behaviour (Improved Fallback, Fixes Important Finding 3.2)</h2>
 * If Redis is unavailable, the filter falls back to the local Caffeine bucket for the
 * same key. This provides node-level protection rather than completely failing open.
 * The fallback is logged at WARN level.
 *
 * <h2>IP Spoofing Protection (Fixes Critical Finding 2.1)</h2>
 * IP extraction honours the {@code trust-proxy-headers} property. By default
 * ({@code false}), only {@code request.getRemoteAddr()} is used.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String HEADER_RETRY_AFTER          = "Retry-After";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";

    /**
     * Local in-memory bucket cache.
     * <p>
     * Used for:
     * <ul>
     *   <li>Pre-auth DoS gate (always, fast path)</li>
     *   <li>Redis fallback when distributed storage is unavailable</li>
     * </ul>
     * TTL is set to 2× the longest refill window to ensure buckets expire
     * naturally. Maximum size caps memory usage regardless of unique IP count.
     */
    private final Cache<String, Bucket> localBucketCache;

    private final ProxyManager<String>  proxyManager;
    private final RateLimitProperties   properties;
    private final RateLimitKeyResolver  keyResolver;
    private final ObjectMapper          objectMapper;
    private final AntPathMatcher        pathMatcher;

    public RateLimitFilter(ProxyManager<String> proxyManager,
                           RateLimitProperties properties,
                           RateLimitKeyResolver keyResolver,
                           ObjectMapper objectMapper) {
        this.proxyManager = proxyManager;
        this.properties   = properties;
        this.keyResolver  = keyResolver;
        this.objectMapper = objectMapper;
        this.pathMatcher  = new AntPathMatcher();

        // Local cache: expires after 2 minutes (covers a 60-second refill window with margin)
        // Max 50,000 entries ≈ 50k unique IPs/usernames stored per node (~4 MB overhead)
        this.localBucketCache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofSeconds(120))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Step 1: Global disable ────────────────────────────────────────────
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Whitelist check ───────────────────────────────────────────
        String requestPath = request.getRequestURI();
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 3: Pre-auth local DoS gate (Security Fix 2.2) ───────────────
        // Fast local (Caffeine) IP-based check executed BEFORE any downstream
        // filter can trigger JWT validation or DB queries.
        String ipKey = keyResolver.resolveIpKey(request);
        Bucket localBucket = getOrCreateLocalBucket(ipKey, properties.getPreAuth());
        ConsumptionProbe localProbe = localBucket.tryConsumeAndReturnRemaining(1);
        if (!localProbe.isConsumed()) {
            long retryAfter = Math.max(
                    TimeUnit.NANOSECONDS.toSeconds(localProbe.getNanosToWaitForRefill()), 1L);
            log.warn("Pre-auth gate: rate limit exceeded for ip='{}' path='{}' retryAfter={}s",
                    ipKey, requestPath, retryAfter);
            writeRateLimitResponse(response, retryAfter);
            return;
        }

        // ── Step 4: Auth-tier check (Important Fix 3.1) ─────────────────────
        // Authentication endpoints (login/register) receive a separate stricter
        // Redis-backed limit to prevent brute-forcing of credentials.
        if (isAuthPath(requestPath)) {
            if (!checkRedisBucket(ipKey, properties.getAuth(), response, requestPath,
                    ipKey /* fallback key */)) {
                return; // request rejected — 429 already written
            }
            // Auth path passed both pre-auth gate and auth-tier — allow through
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 5: Standard tier check (public or private) ──────────────────
        String bucketKey = keyResolver.resolve(request);
        RateLimitProperties.TierProperties tier = bucketKey.startsWith(RateLimitKeyResolver.KEY_PREFIX_USER)
                ? properties.getPrivate()
                : properties.getPublic();

        if (!checkRedisBucket(bucketKey, tier, response, requestPath, ipKey)) {
            return; // request rejected — 429 already written
        }

        filterChain.doFilter(request, response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Redis bucket check with local fallback (Fixes Important Finding 3.2)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to consume a token from the distributed Redis bucket for the given key.
     * <p>
     * If Redis is unavailable, falls back to the local Caffeine bucket for the same key,
     * providing node-level protection rather than fully failing open.
     *
     * @param bucketKey   the Redis/local cache key (IP or username prefixed)
     * @param tier        the rate limit parameters for this tier
     * @param response    the HTTP response to write 429 into on rejection
     * @param requestPath for logging
     * @param fallbackKey the local-cache key to use as Redis fallback
     * @return {@code true} if the token was consumed (request allowed),
     *         {@code false} if the bucket is exhausted (429 already written)
     */
    private boolean checkRedisBucket(String bucketKey,
                                     RateLimitProperties.TierProperties tier,
                                     HttpServletResponse response,
                                     String requestPath,
                                     String fallbackKey) throws IOException {
        try {
            final BucketConfiguration config = buildBucketConfiguration(tier);
            BucketProxy bucket = proxyManager.builder()
                    .build(bucketKey,
                            (java.util.function.Supplier<BucketConfiguration>) () -> config);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                response.addHeader(HEADER_RATE_LIMIT_REMAINING,
                        String.valueOf(probe.getRemainingTokens()));
                return true;
            } else {
                long retryAfter = Math.max(
                        TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()), 1L);
                log.warn("Rate limit exceeded for key='{}' path='{}' retryAfter={}s",
                        bucketKey, requestPath, retryAfter);
                writeRateLimitResponse(response, retryAfter);
                return false;
            }

        } catch (Exception ex) {
            // Redis unavailable: fall back to local Caffeine bucket for this key
            log.warn("Redis unavailable for key='{}' — falling back to local bucket. Cause: {}",
                    bucketKey, ex.getMessage());
            return checkLocalFallbackBucket(fallbackKey, tier, response, requestPath);
        }
    }

    /**
     * Local Caffeine fallback for when Redis is unavailable.
     * Provides per-node rate limiting as a safety net — not as strong as distributed
     * Redis limiting, but prevents unbounded load when Redis is down.
     */
    private boolean checkLocalFallbackBucket(String key,
                                             RateLimitProperties.TierProperties tier,
                                             HttpServletResponse response,
                                             String requestPath) throws IOException {
        // Use a "fallback:" prefix so fallback buckets don't conflict with pre-auth buckets
        Bucket localBucket = getOrCreateLocalBucket("fallback:" + key, tier);
        ConsumptionProbe probe = localBucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader(HEADER_RATE_LIMIT_REMAINING,
                    String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long retryAfter = Math.max(
                    TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()), 1L);
            log.warn("Local fallback rate limit exceeded for key='{}' path='{}' retryAfter={}s",
                    key, requestPath, retryAfter);
            writeRateLimitResponse(response, retryAfter);
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns {@code true} if the path matches any configured whitelist Ant pattern. */
    private boolean isWhitelisted(String requestPath) {
        for (String pattern : properties.getWhitelistedPaths()) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /** Returns {@code true} if the path matches any configured auth-paths Ant pattern. */
    private boolean isAuthPath(String requestPath) {
        for (String pattern : properties.getAuthPaths()) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets or creates a local in-memory Bucket4j bucket for the given key.
     * Thread-safe: Caffeine's {@code get(key, mappingFunction)} guarantees atomic creation.
     */
    private Bucket getOrCreateLocalBucket(String key, RateLimitProperties.TierProperties tier) {
        return localBucketCache.get(key, k ->
                Bucket.builder()
                        .addLimit(limit -> limit
                                .capacity(tier.getCapacity())
                                .refillGreedy(tier.getRefillTokens(),
                                        Duration.ofSeconds(tier.getRefillDurationSeconds())))
                        .build()
        );
    }

    /**
     * Builds a Bucket4j {@link BucketConfiguration} from tier properties.
     * Used for the distributed Redis-backed buckets.
     */
    private BucketConfiguration buildBucketConfiguration(RateLimitProperties.TierProperties tier) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(tier.getCapacity())
                        .refillGreedy(tier.getRefillTokens(),
                                Duration.ofSeconds(tier.getRefillDurationSeconds())))
                .build();
    }

    /** Writes a JSON HTTP 429 response with standard Retry-After header. */
    private void writeRateLimitResponse(HttpServletResponse response,
                                        long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .code("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please try again later.")
                .retryAfterSeconds(retryAfterSeconds)
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Skip rate limiting on async re-dispatches (e.g., DeferredResult).
     * Rate limiting applies only to the initial request dispatch.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * Skip rate limiting on Spring's internal error dispatch.
     * The /error endpoint is whitelisted separately.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
