package com.saraasansor.api.tenant.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class EnterpriseRateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public EnterpriseRateLimiterService(StringRedisTemplate redisTemplate,
                                 DefaultRedisScript<Long> rateLimitScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
    }

    public boolean allow(Long tenantId, String endpoint, int limit, int windowSeconds) {

        // redis key format -> rl:{tenantId}:{endpoint}
        String key = "rl:" + tenantId + ":" + endpoint;

        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowSeconds * 1000),
                String.valueOf(limit)
        );

        return result != null && result == 1;
    }
}
