package com.saraasansor.api.mail.service;

import com.saraasansor.api.mail.config.EmailProperties;
import com.saraasansor.api.mail.dto.EmailRequest;
import com.saraasansor.api.mail.enums.EmailTemplateKey;
import com.saraasansor.api.mail.model.EmailSendResult;
import com.saraasansor.api.mail.provider.EmailProvider;
import com.saraasansor.api.mail.template.EmailTemplateContext;
import com.saraasansor.api.mail.template.EmailTemplateRenderer;
import com.saraasansor.api.mail.template.RenderedEmailTemplate;
import com.saraasansor.api.tenant.TenantContext;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Map<String, EmailProvider> providers;
    private final EmailProperties emailProperties;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final Executor emailExecutor;
    private final Environment environment;

    public EmailService(Map<String, EmailProvider> providers,
                        EmailProperties emailProperties,
                        EmailTemplateRenderer emailTemplateRenderer,
                        @Qualifier("emailExecutor") Executor emailExecutor,
                        Environment environment) {
        this.providers = providers;
        this.emailProperties = emailProperties;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.emailExecutor = emailExecutor;
        this.environment = environment;
    }

    public void sendEmail(String to, String subject, String body) {
        EmailRequest request = new EmailRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setHtml(wrapPlainText(body));
        send(request);
    }

    public void send(EmailRequest request) {
        EmailRequest normalized = normalize(request);
        EmailProvider provider = resolveProvider();

        int maxAttempts = Math.max(1, emailProperties.getRetry().getMaxAttempts());
        EmailDeliveryException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info(
                        "Email send attempt provider={} attempt={}/{} tenantId={} to={} subject={}",
                        emailProperties.getProvider(),
                        attempt,
                        maxAttempts,
                        normalized.getTenantId(),
                        normalized.getTo(),
                        normalized.getSubject()
                );
                provider.send(normalized);
                return;
            } catch (EmailDeliveryException ex) {
                lastException = ex;
                log.warn(
                        "Email send failed provider={} attempt={}/{} tenantId={} to={} subject={} reason={}",
                        emailProperties.getProvider(),
                        attempt,
                        maxAttempts,
                        normalized.getTenantId(),
                        normalized.getTo(),
                        normalized.getSubject(),
                        ex.getMessage()
                );
                if (attempt < maxAttempts) {
                    backoff(attempt);
                }
            } catch (RuntimeException ex) {
                lastException = new EmailDeliveryException("Email send failed: " + ex.getMessage(), ex);
                log.error(
                        "Unexpected email send failure provider={} tenantId={} to={} subject={}",
                        emailProperties.getProvider(),
                        normalized.getTenantId(),
                        normalized.getTo(),
                        normalized.getSubject(),
                        ex
                );
                if (attempt < maxAttempts) {
                    backoff(attempt);
                }
            }
        }

        throw Objects.requireNonNullElseGet(lastException, () -> new EmailDeliveryException("Email send failed"));
    }

    public boolean sendSafely(EmailRequest request) {
        try {
            send(request);
            return true;
        } catch (RuntimeException ex) {
            log.error(
                    "Email delivery suppressed tenantId={} to={} subject={} reason={}",
                    request == null ? null : request.getTenantId(),
                    request == null ? null : request.getTo(),
                    request == null ? null : request.getSubject(),
                    ex.getMessage(),
                    ex
            );
            return false;
        }
    }

    public CompletableFuture<Boolean> sendAsync(EmailRequest request) {
        return CompletableFuture.supplyAsync(() -> sendSafely(request), emailExecutor);
    }

    public EmailSendResult sendTemplate(EmailTemplateKey templateKey,
                                        String to,
                                        Map<String, String> variables,
                                        String tenantId) {
        return sendTemplate(templateKey, to, null, variables, tenantId);
    }

    public EmailSendResult sendTemplate(EmailTemplateKey templateKey,
                                        String to,
                                        String replyTo,
                                        Map<String, String> variables,
                                        String tenantId) {
        try {
            RenderedEmailTemplate rendered = emailTemplateRenderer.render(templateKey, EmailTemplateContext.of(variables));
            EmailRequest request = new EmailRequest();
            request.setTo(to);
            request.setReplyTo(replyTo);
            request.setSubject(rendered.getSubject());
            request.setHtml(rendered.getHtml());
            request.setTenantId(tenantId);
            send(request);
            return EmailSendResult.success();
        } catch (RuntimeException ex) {
            log.error("Templated email failed templateKey={} tenantId={} to={} reason={}",
                    templateKey, tenantId, to, ex.getMessage(), ex);
            return EmailSendResult.failure(ex.getMessage());
        }
    }

    public boolean sendMaintenanceReminder(String to,
                                           String customerName,
                                           String elevatorName,
                                           String date,
                                           String tenantId) {
        return sendTemplate(EmailTemplateKey.MAINTENANCE_REMINDER, to, variables(
                "customerName", customerName,
                "elevatorName", elevatorName,
                "maintenanceDate", date,
                "buildingName", "-"
        ), tenantId).isSuccess();
    }

    public boolean sendMaintenanceOverdue(String to, String customerName, String elevatorName, String dueDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.MAINTENANCE_OVERDUE, to, variables(
                "customerName", customerName,
                "elevatorName", elevatorName,
                "dueDate", dueDate,
                "elevatorCode", "-"
        ), tenantId).isSuccess();
    }

    public boolean sendMaintenanceCompleted(String to, String customerName, String elevatorName, String maintenanceDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.MAINTENANCE_COMPLETED, to, variables(
                "customerName", customerName,
                "elevatorName", elevatorName,
                "maintenanceDate", maintenanceDate,
                "technicianName", "-"
        ), tenantId).isSuccess();
    }

    public boolean sendInvoiceEmail(String to,
                                    String customerName,
                                    String invoiceNumber,
                                    String amount,
                                    String dueDate,
                                    String tenantId) {
        return sendInvoiceCreated(to, customerName, invoiceNumber, amount, dueDate, tenantId);
    }

    public boolean sendInvoiceCreated(String to, String customerName, String invoiceNumber, String amount, String dueDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.INVOICE_CREATED, to, variables(
                "customerName", customerName,
                "invoiceNumber", invoiceNumber,
                "amount", amount,
                "dueDate", dueDate
        ), tenantId).isSuccess();
    }

    public boolean sendInvoiceReminder(String to, String customerName, String invoiceNumber, String amount, String dueDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.INVOICE_REMINDER, to, variables(
                "customerName", customerName,
                "invoiceNumber", invoiceNumber,
                "amount", amount,
                "dueDate", dueDate
        ), tenantId).isSuccess();
    }

    public boolean sendPaymentReminder(String to,
                                       String customerName,
                                       String invoiceNumber,
                                       String amount,
                                       String dueDate,
                                       String tenantId) {
        return sendTemplate(EmailTemplateKey.PAYMENT_REMINDER, to, variables(
                "customerName", customerName,
                "invoiceNumber", invoiceNumber,
                "amount", amount,
                "dueDate", dueDate
        ), tenantId).isSuccess();
    }

    public boolean sendPaymentOverdue(String to, String customerName, String invoiceNumber, String amount, String dueDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.PAYMENT_OVERDUE, to, variables(
                "customerName", customerName,
                "invoiceNumber", invoiceNumber,
                "amount", amount,
                "dueDate", dueDate
        ), tenantId).isSuccess();
    }

    public boolean sendPaymentReceived(String to, String customerName, String amount, String paymentDate, String tenantId) {
        return sendTemplate(EmailTemplateKey.PAYMENT_RECEIVED, to, variables(
                "customerName", customerName,
                "amount", amount,
                "paymentDate", paymentDate
        ), tenantId).isSuccess();
    }

    public boolean sendOfferCreated(String to, String customerName, String offerNumber, String amount, String tenantId) {
        return sendTemplate(EmailTemplateKey.OFFER_CREATED, to, variables(
                "customerName", customerName,
                "offerNumber", offerNumber,
                "amount", amount
        ), tenantId).isSuccess();
    }

    public boolean sendOfferApproved(String to, String customerName, String offerNumber, String tenantId) {
        return sendTemplate(EmailTemplateKey.OFFER_APPROVED, to, variables(
                "customerName", customerName,
                "offerNumber", offerNumber
        ), tenantId).isSuccess();
    }

    public boolean sendOfferRejected(String to, String customerName, String offerNumber, String tenantId) {
        return sendTemplate(EmailTemplateKey.OFFER_REJECTED, to, variables(
                "customerName", customerName,
                "offerNumber", offerNumber
        ), tenantId).isSuccess();
    }

    public boolean sendWelcomeEmail(String to, String customerName, String panelUrl, String tenantId) {
        return sendTemplate(EmailTemplateKey.WELCOME, to, variables(
                "customerName", customerName,
                "panelUrl", panelUrl
        ), tenantId).isSuccess();
    }

    public boolean sendPasswordReset(String to, String customerName, String resetUrl, String tenantId) {
        return sendTemplate(EmailTemplateKey.PASSWORD_RESET, to, variables(
                "customerName", customerName,
                "resetUrl", resetUrl
        ), tenantId).isSuccess();
    }

    public boolean sendGenericNotification(String to, String customerName, String title, String message, String tenantId) {
        return sendTemplate(EmailTemplateKey.GENERIC_NOTIFICATION, to, variables(
                "customerName", customerName,
                "title", title,
                "message", message
        ), tenantId).isSuccess();
    }

    public boolean sendTestEmail(String to) {
        return sendTemplate(EmailTemplateKey.TEST_EMAIL, to, variables(
                "panelUrl", "https://app.asenovo.com"
        ), "platform").isSuccess();
    }

    private EmailProvider resolveProvider() {
        String providerKey = emailProperties.getProvider() == null
                ? "resend"
                : emailProperties.getProvider().toLowerCase(Locale.ROOT);
        EmailProvider provider = providers.get(providerKey);
        if (provider == null) {
            throw new EmailValidationException("Unsupported email provider: " + providerKey);
        }
        return provider;
    }

    private EmailRequest normalize(EmailRequest request) {
        if (request == null) {
            throw new EmailValidationException("Email request is required");
        }
        if (!StringUtils.hasText(request.getTo())
                || !StringUtils.hasText(request.getSubject())
                || !StringUtils.hasText(request.getHtml())) {
            throw new EmailValidationException("Email recipient, subject, and html are required");
        }

        InternetAddress to = validateAddress(request.getTo(), "recipient");
        if (StringUtils.hasText(request.getFrom())) {
            validateAddress(request.getFrom(), "from");
        }
        if (StringUtils.hasText(request.getReplyTo())) {
            validateAddress(request.getReplyTo(), "replyTo");
        }

        EmailRequest normalized = new EmailRequest();
        normalized.setTo(to.getAddress().trim());
        normalized.setSubject(request.getSubject().trim());
        normalized.setHtml(request.getHtml());
        normalized.setFrom(StringUtils.hasText(request.getFrom())
                ? request.getFrom().trim()
                : emailProperties.getResend().getFrom());
        normalized.setReplyTo(StringUtils.hasText(request.getReplyTo()) ? request.getReplyTo().trim() : null);
        normalized.setTenantId(resolveTenantId(request.getTenantId()));

        validateAddress(normalized.getFrom(), "from");
        validateFromDomain(normalized.getFrom());
        validateProductionContent(normalized);
        return normalized;
    }

    private String resolveTenantId(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            return tenantId.trim();
        }
        Long currentTenantId = TenantContext.getTenantId();
        return currentTenantId == null ? "platform" : String.valueOf(currentTenantId);
    }

    private InternetAddress validateAddress(String value, String field) {
        try {
            InternetAddress address = new InternetAddress(value);
            address.validate();
            return address;
        } catch (AddressException ex) {
            throw new EmailValidationException("Invalid " + field + " email address");
        }
    }

    private void validateFromDomain(String from) {
        String allowedDomain = emailProperties.getResend().getAllowedFromDomain();
        if (!StringUtils.hasText(allowedDomain)) {
            return;
        }

        InternetAddress fromAddress = validateAddress(from, "from");
        String emailAddress = fromAddress.getAddress().toLowerCase(Locale.ROOT);
        String normalizedDomain = allowedDomain.trim().toLowerCase(Locale.ROOT);
        if (!emailAddress.endsWith("@" + normalizedDomain)) {
            throw new EmailValidationException("Email from address must use verified domain: " + normalizedDomain);
        }
    }

    private void validateProductionContent(EmailRequest request) {
        if (!isProductionProfile()) {
            return;
        }

        String combined = (request.getSubject() + " " + request.getHtml()).toLowerCase(Locale.ROOT);
        if (combined.contains("hello world")
                || combined.contains("resend.dev")
                || combined.contains("placeholder")) {
            throw new EmailValidationException("Production email contains test or placeholder content");
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private void backoff(int attempt) {
        long backoffMillis = Math.max(0L, emailProperties.getRetry().getInitialBackoffMillis()) * attempt;
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String wrapPlainText(String body) {
        String safeBody = HtmlUtils.htmlEscape(defaultValue(body)).replace("\n", "<br/>");
        return "<html><body><div>" + safeBody + "</div></body></html>";
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private Map<String, String> variables(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new EmailValidationException("Email template variables must be key/value pairs");
        }
        java.util.HashMap<String, String> variables = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            variables.put(keyValues[i], defaultValue(keyValues[i + 1]));
        }
        return variables;
    }
}
