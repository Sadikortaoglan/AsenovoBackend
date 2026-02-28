package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.data.PlanSnapShot;
import com.saraasansor.api.tenant.model.Plan;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PlanService {

    private static final String CACHE_PREFIX = "tenant:plan:";

    private final SubscriptionRepository subscriptionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public PlanService(SubscriptionRepository subscriptionRepository, RedisTemplate<String, Object> redisTemplate) {
        this.subscriptionRepository = subscriptionRepository;
        this.redisTemplate = redisTemplate;
    }

    public PlanSnapShot getCurrentPlan(Long tenantId) {

        String cacheKey = CACHE_PREFIX + tenantId;

        // 1️⃣ Cache kontrolü
        PlanSnapShot cachedPlan = (PlanSnapShot) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPlan != null) {
            return cachedPlan;
        }

        // 2️⃣ DB’den çek
        Subscription subscription = subscriptionRepository
                .findActiveSubscription(tenantId)
                .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        PlanSnapShot planSnapShot = getPlanSnapShot(subscription.getPlan());

        // 3️⃣ Cache’e koy (5 dakika TTL)
        redisTemplate.opsForValue().set(
                cacheKey,
                planSnapShot,
                Duration.ofMinutes(5)
        );

        return planSnapShot;
    }

    private PlanSnapShot getPlanSnapShot(Plan plan) {
        return new PlanSnapShot(
                plan.getId(),
                plan.getCode(),
                plan.getPlanType().name(),
                plan.getMaxUsers(),
                plan.getMaxAssets(),
                plan.getApiRateLimitPerMinute(),
                plan.getMaxStorageMb(),
                plan.isPrioritySupport(),
                plan.isActive());
    }

    public int getRateLimit(Long tenantId) {
        return getCurrentPlan(tenantId).getApiRateLimitPerMinute();
    }

    public int getMaxUsers(Long tenantId) {
        return getCurrentPlan(tenantId).getMaxUsers();
    }

    public int getMaxAssets(Long tenantId) {
        return getCurrentPlan(tenantId).getMaxAssets();
    }

    /**
     * Plan değişince: CACHE INVALIDATION
     * Admin panelde plan güncellenince bunu çağırırsın.
     * @param tenantId
     */
    public void refreshPlanCache(Long tenantId) {
        redisTemplate.delete(CACHE_PREFIX + tenantId);
    }
}
