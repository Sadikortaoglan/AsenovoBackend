package com.saraasansor.api.marketing.service;

import com.saraasansor.api.marketing.dto.ContactRequestDto;
import com.saraasansor.api.marketing.dto.DemoRequestDto;
import com.saraasansor.api.marketing.dto.PlanRequestDto;
import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import com.saraasansor.api.marketing.dto.TrialSubmissionResultDto;
import com.saraasansor.api.marketing.repository.MarketingLeadRepository;
import com.saraasansor.api.marketing.repository.TrialRequestProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;

@Service
public class MarketingLeadService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingLeadService.class);

    private final MarketingLeadRepository repository;
    private final MarketingEmailService emailService;
    private final DemoTenantProvisioningService demoTenantProvisioningService;
    private final Executor marketingOnboardingExecutor;

    public MarketingLeadService(MarketingLeadRepository repository,
                                MarketingEmailService emailService,
                                DemoTenantProvisioningService demoTenantProvisioningService,
                                @Qualifier("marketingOnboardingExecutor") Executor marketingOnboardingExecutor) {
        this.repository = repository;
        this.emailService = emailService;
        this.demoTenantProvisioningService = demoTenantProvisioningService;
        this.marketingOnboardingExecutor = marketingOnboardingExecutor;
    }

    @Transactional
    public String submitDemoRequest(DemoRequestDto request) {
        long requestId = repository.saveDemoRequest(request);
        logger.info("Demo request saved for email={}", request.getEmail());
        try {
            boolean emailed = emailService.sendDemoRequestNotification(request);
            if (emailed) {
                repository.markDemoRequestEmailed(requestId);
            } else {
                repository.markDemoRequestEmailFailed(requestId, "Mail sender is not configured");
            }
        } catch (RuntimeException ex) {
            repository.markDemoRequestEmailFailed(requestId, ex.getMessage());
        }
        return "Demo talebiniz alindi. Ekibimiz sizinle en kisa surede iletisime gececek.";
    }

    @Transactional
    public TrialSubmissionResultDto submitTrialRequest(TrialRequestDto request) {
        TrialRequestProjection existingRequest = repository.findOpenTrialRequest(
                request.getEmail(),
                request.getCompany(),
                request.getPhone()
        );
        if (existingRequest != null) {
            return handleExistingTrialRequest(existingRequest);
        }

        TrialRequestProjection requestRow = repository.saveTrialRequest(request);
        logger.info("Trial request saved for email={}", request.getEmail());

        registerAfterCommit(() -> marketingOnboardingExecutor.execute(() -> provisionTrialAsync(requestRow, request)));

        TrialProvisionResponseDto response = new TrialProvisionResponseDto();
        response.setRequestToken(requestRow.requestToken());
        response.setStatus("PENDING");
        response.setExistingDemo(false);
        response.setAccessEmailSent(false);
        return new TrialSubmissionResultDto("Demo ortaminiz hazirlaniyor.", response);
    }

    @Transactional(readOnly = true)
    public TrialProvisionResponseDto getTrialRequestStatus(String requestToken) {
        TrialRequestProjection request = repository.findTrialRequestByToken(requestToken);
        return toResponse(request);
    }

    @Transactional
    public String submitPlanRequest(PlanRequestDto request) {
        long requestId = repository.savePlanRequest(request);
        logger.info("Plan request saved for email={} plan={}", request.getEmail(), request.getPlan());
        try {
            boolean emailed = emailService.sendPlanRequestNotification(request);
            if (emailed) {
                repository.markPlanRequestNotified(requestId);
                return "Plan talebiniz ekibimize iletildi.";
            }
            repository.markPlanRequestNotificationFailed(requestId, "Mail sender is not configured");
            return "Plan talebiniz alindi. Ekibimiz sizinle en kisa surede iletisime gececek.";
        } catch (RuntimeException ex) {
            repository.markPlanRequestNotificationFailed(requestId, ex.getMessage());
            return "Plan talebiniz alindi. Bildirim tarafinda gecici bir sorun yasandi.";
        }
    }

    @Transactional
    public String submitContactRequest(ContactRequestDto request) {
        long messageId = repository.saveContactMessage(request);
        logger.info("Contact message saved with id={} for email={}", messageId, request.getEmail());

        try {
            boolean emailed = emailService.sendContactMessage(request);
            if (emailed) {
                repository.markContactEmailed(messageId);
                return "Mesajiniz alindi ve ekibimize iletildi.";
            }

            repository.markContactEmailFailed(messageId, "Mail sender is not configured");
            return "Mesajiniz alindi. Ekibimiz en kisa surede size donus yapacak.";
        } catch (RuntimeException ex) {
            repository.markContactEmailFailed(messageId, ex.getMessage());
            return "Mesajiniz alindi. E-posta iletiminde gecici bir sorun yasandi, ekibimiz kaydinizi kontrol edecek.";
        }
    }

    private void provisionTrialAsync(TrialRequestProjection requestRow, TrialRequestDto request) {
        repository.markTrialProvisioning(requestRow.id());
        try {
            TrialProvisionResponseDto response = demoTenantProvisioningService.provisionDemoTenant(request);
            response.setRequestToken(requestRow.requestToken());
            repository.markTrialProvisioned(requestRow.id(), response);

            try {
                boolean emailed = emailService.sendTrialReadyEmail(request, response);
                if (emailed) {
                    repository.markTrialEmailed(requestRow.id());
                } else {
                    repository.markTrialEmailFailed(requestRow.id(), "Mail sender is not configured");
                }
            } catch (RuntimeException ex) {
                repository.markTrialEmailFailed(requestRow.id(), ex.getMessage());
            }
        } catch (RuntimeException ex) {
            repository.markTrialProvisionFailed(requestRow.id(), ex.getMessage());
            logger.error("Trial provisioning failed for token={}: {}", requestRow.requestToken(), ex.getMessage(), ex);
        }
    }

    private TrialSubmissionResultDto handleExistingTrialRequest(TrialRequestProjection existingRequest) {
        TrialProvisionResponseDto response = toResponse(existingRequest);
        response.setExistingDemo(true);

        if ("READY".equals(existingRequest.provisioningStatus())) {
            try {
                String temporaryPassword = demoTenantProvisioningService.rotateExistingDemoPassword(existingRequest);
                repository.updateTrialTemporaryPassword(existingRequest.id(), temporaryPassword);
                response.setTemporaryPassword(temporaryPassword);
                boolean emailed = emailService.sendTrialAccessReminderEmail(existingRequest.email(), response);
                response.setAccessEmailSent(emailed);
                if (emailed) {
                    repository.markTrialEmailed(existingRequest.id());
                } else {
                    repository.markTrialEmailFailed(existingRequest.id(), "Mail sender is not configured");
                }
            } catch (RuntimeException ex) {
                repository.markTrialEmailFailed(existingRequest.id(), ex.getMessage());
                response.setAccessEmailSent(false);
                logger.warn("Existing demo access refresh failed for token={}: {}", existingRequest.requestToken(), ex.getMessage());
            }

            return new TrialSubmissionResultDto(buildExistingReadyMessage(response.getAccessEmailSent()), response);
        }

        response.setAccessEmailSent(false);
        return new TrialSubmissionResultDto(
                "Bu e-posta ve sirket icin zaten hazirlanmakta olan bir demo talebiniz var. Yeni ortam acilmadi.",
                response
        );
    }

    private TrialProvisionResponseDto toResponse(TrialRequestProjection request) {
        TrialProvisionResponseDto response = new TrialProvisionResponseDto();
        response.setRequestToken(request.requestToken());
        response.setTenantSlug(request.tenantSlug());
        response.setTenantDatabase(request.tenantDatabase());
        response.setLoginUrl(request.loginUrl());
        response.setExpiresAt(request.expiresAt());
        response.setStatus(request.provisioningStatus());
        response.setUsername(StringUtils.hasText(request.username()) ? request.username() : "system_admin");
        response.setTemporaryPassword(request.temporaryPassword());
        response.setProvisioningError(request.provisioningError());
        response.setExistingDemo(false);
        response.setAccessEmailSent(request.emailed());
        response.setEmailError(request.emailError());
        return response;
    }

    private String buildExistingReadyMessage(Boolean accessEmailSent) {
        if (Boolean.TRUE.equals(accessEmailSent)) {
            return "Bu e-posta ve sirket icin aktif bir demo ortaminiz zaten bulunuyor. Yeni demo acilmadi, erisim bilgileriniz e-posta adresinize tekrar gonderildi.";
        }
        return "Bu e-posta ve sirket icin aktif bir demo ortaminiz zaten bulunuyor. Yeni demo acilmadi, mevcut demo hesabinizi kullanabilirsiniz.";
    }

    private void registerAfterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

}
