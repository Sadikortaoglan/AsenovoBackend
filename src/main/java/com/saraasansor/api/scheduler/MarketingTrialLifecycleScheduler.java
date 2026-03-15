package com.saraasansor.api.scheduler;

import com.saraasansor.api.marketing.service.MarketingTrialLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketingTrialLifecycleScheduler {

    private final MarketingTrialLifecycleService lifecycleService;

    public MarketingTrialLifecycleScheduler(MarketingTrialLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(cron = "${asenovo.marketing.trial-expire-cron:0 0 4 * * *}")
    public void expireReadyTrials() {
        lifecycleService.expireReadyTrials();
    }

    @Scheduled(cron = "${asenovo.marketing.trial-cleanup-cron:0 30 4 * * *}")
    public void cleanupExpiredTrials() {
        lifecycleService.cleanupExpiredTrials();
    }
}
