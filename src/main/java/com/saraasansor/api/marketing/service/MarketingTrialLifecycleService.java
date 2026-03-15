package com.saraasansor.api.marketing.service;

import com.saraasansor.api.marketing.repository.MarketingLeadRepository;
import com.saraasansor.api.marketing.repository.TrialRequestProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketingTrialLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingTrialLifecycleService.class);

    private final MarketingLeadRepository repository;
    private final DemoTenantProvisioningService demoTenantProvisioningService;

    @Value("${asenovo.marketing.trial-cleanup-grace-days:7}")
    private int cleanupGraceDays;

    public MarketingTrialLifecycleService(MarketingLeadRepository repository,
                                          DemoTenantProvisioningService demoTenantProvisioningService) {
        this.repository = repository;
        this.demoTenantProvisioningService = demoTenantProvisioningService;
    }

    @Transactional
    public void expireReadyTrials() {
        List<TrialRequestProjection> expiredTrials = repository.findTrialsReadyToExpire();
        for (TrialRequestProjection request : expiredTrials) {
            try {
                demoTenantProvisioningService.expireDemoTenant(request);
                repository.markTrialExpired(request.id());
                logger.info("Expired marketing trial token={} tenant={}", request.requestToken(), request.tenantSlug());
            } catch (RuntimeException ex) {
                logger.warn("Could not expire marketing trial token={}: {}", request.requestToken(), ex.getMessage());
            }
        }
    }

    @Transactional
    public void cleanupExpiredTrials() {
        List<TrialRequestProjection> cleanupCandidates = repository.findTrialsReadyForCleanup(cleanupGraceDays);
        for (TrialRequestProjection request : cleanupCandidates) {
            try {
                demoTenantProvisioningService.cleanupExpiredDemoTenant(request);
                repository.markTrialCleanedUp(request.id());
                logger.info("Cleaned up marketing trial token={} tenant={} database={}",
                        request.requestToken(), request.tenantSlug(), request.tenantDatabase());
            } catch (RuntimeException ex) {
                logger.warn("Could not clean up marketing trial token={}: {}", request.requestToken(), ex.getMessage());
            }
        }
    }
}
