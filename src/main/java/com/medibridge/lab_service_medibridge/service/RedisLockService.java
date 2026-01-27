package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:task:";
    private static final long LOCK_TTL_SECONDS = 10;

    /**
     * Try to acquire lock for a specific task
     */
    public boolean acquireLock(String taskId) {
        String key = LOCK_PREFIX + taskId;
        // setIfAbsent = SETNX
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.TRUE.equals(success)) {
            log.debug("Lock acquired for task: {}", taskId);
            return true;
        }
        log.warn("Failed to acquire lock for task: {}", taskId);
        return false;
    }

    /**
     * Release lock
     */
    public void releaseLock(String taskId) {
        String key = LOCK_PREFIX + taskId;
        redisTemplate.delete(key);
        log.debug("Lock released for task: {}", taskId);
    }
}
