package com.ticketbox.backend.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbox.backend.dto.ErrorResponse;
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
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces rate limiting on all HTTP requests.
 * <p>
 * <b>Filter position:</b> Placed <em>after</em> {@code JwtAuthenticationFilter} so the
 * {@link SecurityContextHolder} is already populated when we need to resolve the
 * authenticated username for private-tier limits.
 * <p>
 * <b>Two-tier strategy:</b>
 * <ul>
 *   <li><b>Private tier</b> — authenticated requests limited by username (10 req/min default)</li>
 *   <li><b>Public tier</b> — unauthenticated requests limited by client IP (100 req/min default)</li>
 * </ul>
 * <p>
 * <b>Fail-open behavior:</b> If Redis is unavailable, the request is allowed through
 * with a warning log. This prevents a Redis outage from taking down the entire API.
 * <p>
 * <b>Whitelist:</b> Paths listed in {@link RateLimitProperties#getWhitelistedPaths()} skip
 * all rate limit checks (Actuator, Swagger, static resources, error pages).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String HEADER_RETRY_AFTER       = "Retry-After";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties  properties;
    private final RateLimitKeyResolver keyResolver;
    private final ObjectMapper         objectMapper;
    private final AntPathMatcher       pathMatcher;

    public RateLimitFilter(ProxyManager<String> proxyManager,
                           RateLimitProperties properties,
                           RateLimitKeyResolver keyResolver,
                           ObjectMapper objectMapper) {
        this.proxyManager  = proxyManager;
        this.properties    = properties;
        this.keyResolver   = keyResolver;
        this.objectMapper  = objectMapper;
        this.pathMatcher   = new AntPathMatcher();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Skip rate limiting if globally disabled
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Skip rate limiting for whitelisted paths (actuator, swagger, etc.)
        String requestPath = request.getRequestURI();
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Resolve bucket key (username or IP) and corresponding bucket configuration
        String bucketKey = keyResolver.resolve(request);
        BucketConfiguration bucketConfig = resolveBucketConfiguration(bucketKey);

        try {
            // 4. Lazily create or retrieve the distributed bucket from Redis.
            // Use explicit Supplier<BucketConfiguration> form to disambiguate the
            // two build() overloads in RemoteBucketBuilder (Bucket4j 8.x).
            final BucketConfiguration config = bucketConfig;
            BucketProxy bucket = proxyManager.builder().build(bucketKey, (java.util.function.Supplier<BucketConfiguration>) () -> config);

            // 5. Atomically try to consume 1 token
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                // Token consumed — allow request and add informational header
                response.addHeader(HEADER_RATE_LIMIT_REMAINING,
                        String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                // No tokens left — reject with HTTP 429
                long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(
                        probe.getNanosToWaitForRefill());
                // Ensure at least 1 second is shown to the client
                retryAfterSeconds = Math.max(retryAfterSeconds, 1L);

                log.warn("Rate limit exceeded for key='{}' path='{}' retryAfter={}s",
                        bucketKey, requestPath, retryAfterSeconds);

                writeRateLimitResponse(response, retryAfterSeconds);
            }

        } catch (Exception ex) {
            // 6. Fail-open: if Redis is down or Bucket4j fails, allow the request through
            log.warn("Rate limit check failed for key='{}' — failing open. Cause: {}",
                    bucketKey, ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether the given request path matches any whitelisted Ant pattern.
     */
    private boolean isWhitelisted(String requestPath) {
        for (String pattern : properties.getWhitelistedPaths()) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link BucketConfiguration} appropriate for the given bucket key.
     * <p>
     * Private-tier buckets (keys starting with {@code ratelimit:user:}) use the
     * private configuration. All other buckets use the public configuration.
     */
    private BucketConfiguration resolveBucketConfiguration(String bucketKey) {
        if (bucketKey.startsWith(RateLimitKeyResolver.KEY_PREFIX_USER)) {
            return buildBucketConfiguration(properties.getPrivate());
        }
        return buildBucketConfiguration(properties.getPublic());
    }

    /**
     * Builds a {@link BucketConfiguration} from a {@link RateLimitProperties.TierProperties}.
     * <p>
     * Uses a greedy refill so the full refill amount is added at the end of each
     * interval (not spread over time). This matches the Token Bucket semantics
     * described in the approved design.
     */
    private BucketConfiguration buildBucketConfiguration(RateLimitProperties.TierProperties tier) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(tier.getCapacity())
                        .refillGreedy(tier.getRefillTokens(),
                                java.time.Duration.ofSeconds(tier.getRefillDurationSeconds())))
                .build();
    }

    /**
     * Writes a JSON HTTP 429 response and sets the {@code Retry-After} header.
     * <p>
     * This must be done at the filter level (not via {@code @RestControllerAdvice})
     * because the filter short-circuits before reaching the dispatcher servlet.
     */
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
     * Determines whether the filter should be applied to async dispatches.
     * Rate limiting should only apply to the initial request dispatch, not
     * to async result processing.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * Determines whether the filter should apply to error dispatches.
     * We skip rate limiting on the Spring error dispatch to avoid
     * affecting the /error endpoint.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
