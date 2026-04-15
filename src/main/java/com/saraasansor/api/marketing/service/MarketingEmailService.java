package com.saraasansor.api.marketing.service;

import com.saraasansor.api.marketing.dto.ContactRequestDto;
import com.saraasansor.api.marketing.dto.DemoRequestDto;
import com.saraasansor.api.marketing.dto.PlanRequestDto;
import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import com.saraasansor.api.mail.enums.EmailTemplateKey;
import com.saraasansor.api.mail.model.EmailSendResult;
import com.saraasansor.api.mail.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class MarketingEmailService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingEmailService.class);

    private final EmailService emailService;

    @Value("${asenovo.marketing.support-email:support@asenovo.com}")
    private String supportEmail;

    @Value("${asenovo.marketing.from-email:no-reply@asenovo.com}")
    private String fromEmail;

    @Value("${asenovo.marketing.mail-enabled:true}")
    private boolean mailEnabled;

    @Value("${asenovo.marketing.environment:local}")
    private String marketingEnvironment;

    public MarketingEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public boolean sendContactMessage(ContactRequestDto request) {
        return sendEmail(
                EmailTemplateKey.MARKETING_CONTACT_MESSAGE,
                supportEmail,
                request.getEmail(),
                variables(
                        "companyOrName", defaultText(request.getCompany(), request.getName()),
                        "name", request.getName(),
                        "company", request.getCompany(),
                        "phone", request.getPhone(),
                        "email", request.getEmail(),
                        "message", request.getMessage()
                )
        );
    }

    public boolean sendDemoRequestNotification(DemoRequestDto request) {
        return sendEmail(
                EmailTemplateKey.MARKETING_DEMO_REQUEST,
                supportEmail,
                request.getEmail(),
                variables(
                        "companyOrName", defaultText(request.getCompany(), request.getName()),
                        "name", request.getName(),
                        "company", request.getCompany(),
                        "phone", request.getPhone(),
                        "email", request.getEmail(),
                        "companySize", request.getCompanySize()
                )
        );
    }

    public boolean sendPlanRequestNotification(PlanRequestDto request) {
        return sendEmail(
                EmailTemplateKey.MARKETING_PLAN_REQUEST,
                supportEmail,
                request.getEmail(),
                variables(
                        "plan", request.getPlan(),
                        "name", request.getName(),
                        "company", request.getCompany(),
                        "phone", request.getPhone(),
                        "email", request.getEmail()
                )
        );
    }

    public boolean sendTrialReadyEmail(TrialRequestDto request, TrialProvisionResponseDto response) {
        return sendTrialAccessEmail(request.getEmail(), response, false);
    }

    public boolean sendTrialAccessReminderEmail(String email, TrialProvisionResponseDto response) {
        return sendTrialAccessEmail(email, response, true);
    }

    private boolean sendTrialAccessEmail(String email, TrialProvisionResponseDto response, boolean existingDemo) {
        return sendEmail(
                existingDemo ? EmailTemplateKey.MARKETING_TRIAL_REMINDER : EmailTemplateKey.MARKETING_TRIAL_READY,
                email,
                null,
                variables(
                        "loginUrl", response.getLoginUrl(),
                        "username", response.getUsername(),
                        "temporaryPassword", response.getTemporaryPassword(),
                        "expiresAt", String.valueOf(response.getExpiresAt()),
                        "tenantSlug", response.getTenantSlug()
                )
        );
    }

    private boolean sendEmail(EmailTemplateKey templateKey, String to, String replyTo, Map<String, String> variables) {
        if (!mailEnabled) {
            logger.info(
                    "Marketing email skipped because mail is disabled for environment={}. to={} templateKey={}",
                    marketingEnvironment,
                    to,
                    templateKey
            );
            return false;
        }

        try {
            if (!isVerifiedFromAddress(fromEmail)) {
                logger.warn("Marketing from email is not on verified Resend domain, falling back to global email.from. configuredFrom={}", fromEmail);
            }
            EmailSendResult result = emailService.sendTemplate(templateKey, to, replyTo, variables, "marketing");
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getMessage());
            }
            logger.info("Marketing email sent successfully to={} templateKey={}", to, templateKey);
            return result.isSuccess();
        } catch (RuntimeException ex) {
            String reason = describeMailFailure(ex);
            logger.error("Failed to send marketing email to={} templateKey={} reason={}", to, templateKey, reason, ex);
            throw new RuntimeException(reason);
        }
    }

    private String describeMailFailure(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        String lowered = message.toLowerCase();

        if (lowered.contains("authentication") || lowered.contains("auth")) {
            return "SMTP authentication failed";
        }
        if (lowered.contains("timed out") || lowered.contains("timeout")) {
            return "SMTP connection timed out";
        }
        if (lowered.contains("connection refused") || lowered.contains("could not connect")) {
            return "SMTP server connection failed";
        }
        if (lowered.contains("unknownhost") || lowered.contains("unknown host")) {
            return "SMTP host could not be resolved";
        }
        if (lowered.contains("sendasdenied") || lowered.contains("sender address rejected")) {
            return "SMTP sender address was rejected";
        }
        if (!message.isBlank()) {
            return "Marketing email could not be sent: " + message;
        }
        return "Marketing email could not be sent";
    }

    private boolean isVerifiedFromAddress(String value) {
        return StringUtils.hasText(value) && value.trim().toLowerCase().endsWith("@mail.asenovo.com");
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String defaultText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private Map<String, String> variables(String... keyValues) {
        Map<String, String> variables = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            variables.put(keyValues[i], defaultText(keyValues[i + 1]));
        }
        return variables;
    }
}
