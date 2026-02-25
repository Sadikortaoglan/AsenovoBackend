package com.saraasansor.api.tenant;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class TenantAwareRedisTemplate {

    private final RedisTemplate<String, Object> redisTemplate;

    public TenantAwareRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(String domain, String key, Object value, Duration ttl) {
        String fullKey = TenantCacheKeyBuilder.buildKey(domain, key);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(fullKey, value, ttl);
        } else {
            redisTemplate.opsForValue().set(fullKey, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String domain, String key, Class<T> type) {
        String fullKey = TenantCacheKeyBuilder.buildKey(domain, key);
        Object value = redisTemplate.opsForValue().get(fullKey);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public void evict(String domain, String key) {
        String fullKey = TenantCacheKeyBuilder.buildKey(domain, key);
        redisTemplate.delete(fullKey);
    }
}

