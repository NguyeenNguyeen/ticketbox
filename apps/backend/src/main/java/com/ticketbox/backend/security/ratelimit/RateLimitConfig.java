package com.ticketbox.backend.security.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures a Bucket4j {@link ProxyManager} backed by Redis via the Lettuce driver.
 * <p>
 * The {@code ProxyManager<String>} is the central object that manages rate limit
 * buckets distributed across Redis. It is thread-safe and designed to be used as
 * a singleton bean.
 * <p>
 * Why a direct {@link RedisClient} instead of Spring Data Redis?
 * Bucket4j-Redis requires a {@link StatefulRedisConnection<byte[], byte[]>} (byte-array codec),
 * whereas Spring's auto-configured {@code LettuceConnectionFactory} uses a String codec.
 * We create a minimal dedicated Lettuce client for Bucket4j to avoid interfering
 * with the Spring Data Redis connection pool used by {@code RedisTemplate} and caching.
 */
@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Creates a dedicated Lettuce Redis client for Bucket4j.
     * <p>
     * This is separate from the Spring Data Redis connection factory to avoid
     * codec conflicts between the byte-array codec Bucket4j requires and the
     * String codec used by {@code StringRedisTemplate}.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient() {
        return RedisClient.create("redis://" + redisHost + ":" + redisPort);
    }

    /**
     * Creates a {@link StatefulRedisConnection} with byte-array codec for Bucket4j.
     * <p>
     * Bucket4j serializes bucket state as raw bytes, so the connection must use
     * {@code RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)} — String keys
     * (for human-readable Redis key inspection) and byte[] values (for Bucket4j state).
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        return bucket4jRedisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
    }

    /**
     * Creates the Bucket4j {@link ProxyManager} backed by Redis via Lettuce.
     * <p>
     * The {@code ProxyManager<String>} is used by {@link RateLimitFilter} to:
     * <ul>
     *   <li>Create or retrieve a bucket for a given string key (IP or username)</li>
     *   <li>Atomically consume tokens from the bucket (via Redis Lua scripts)</li>
     *   <li>Automatically persist bucket state in Redis with TTL</li>
     * </ul>
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(
            StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
        return LettuceBasedProxyManager.builderFor(bucket4jRedisConnection)
                .build();
    }
}
