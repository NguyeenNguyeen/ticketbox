package com.ticketbox.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Tries to acquire a distributed lock.
     * @param key the lock key
     * @param timeout lock timeout duration
     * @return true if acquired, false otherwise
     */
    public boolean acquireLock(String key, Duration timeout) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", timeout);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases a distributed lock.
     * @param key the lock key
     */
    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Stores an idempotency key if it doesn't exist.
     * @param key the idempotency key
     * @param timeout how long the key should be kept
     * @return true if it was absent and successfully set, false if it already exists
     */
    public boolean setIfAbsentIdempotencyKey(String key, Duration timeout) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSED", timeout);
        return Boolean.TRUE.equals(success);
    }
}
