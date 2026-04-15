package com.saraasansor.api.mail.template;

import com.saraasansor.api.mail.enums.EmailTemplateKey;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class EmailTemplateRegistry {

    private final Map<EmailTemplateKey, EmailTemplateDefinition> definitions = new EnumMap<>(EmailTemplateKey.class);

    public EmailTemplateRegistry() {
        register(EmailTemplateKey.MAINTENANCE_REMINDER, "Bakim hatirlatmasi - {{elevatorName}}", "maintenance/maintenance-reminder.html",
                Set.of("customerName", "elevatorName", "maintenanceDate"));
        register(EmailTemplateKey.MAINTENANCE_OVERDUE, "Geciken bakim bildirimi - {{elevatorName}}", "maintenance/maintenance-overdue.html",
                Set.of("customerName", "elevatorName", "dueDate"));
        register(EmailTemplateKey.MAINTENANCE_COMPLETED, "Bakim tamamlandi - {{elevatorName}}", "maintenance/maintenance-completed.html",
                Set.of("customerName", "elevatorName", "maintenanceDate"));
        register(EmailTemplateKey.PAYMENT_REMINDER, "Odeme hatirlatmasi - {{invoiceNumber}}", "payment/payment-reminder.html",
                Set.of("customerName", "invoiceNumber", "amount", "dueDate"));
        register(EmailTemplateKey.PAYMENT_OVERDUE, "Geciken odeme bildirimi - {{invoiceNumber}}", "payment/payment-overdue.html",
                Set.of("customerName", "invoiceNumber", "amount", "dueDate"));
        register(EmailTemplateKey.PAYMENT_RECEIVED, "Odemeniz alindi", "payment/payment-received.html",
                Set.of("customerName", "amount", "paymentDate"));
        register(EmailTemplateKey.INVOICE_CREATED, "Faturanız hazir - {{invoiceNumber}}", "invoice/invoice-created.html",
                Set.of("customerName", "invoiceNumber", "amount", "dueDate"));
        register(EmailTemplateKey.INVOICE_REMINDER, "Fatura hatirlatmasi - {{invoiceNumber}}", "invoice/invoice-reminder.html",
                Set.of("customerName", "invoiceNumber", "amount", "dueDate"));
        register(EmailTemplateKey.OFFER_CREATED, "Teklifiniz hazir - {{offerNumber}}", "offer/offer-created.html",
                Set.of("customerName", "offerNumber", "amount"));
        register(EmailTemplateKey.OFFER_APPROVED, "Teklif onaylandi - {{offerNumber}}", "offer/offer-approved.html",
                Set.of("customerName", "offerNumber"));
        register(EmailTemplateKey.OFFER_REJECTED, "Teklif sonucu - {{offerNumber}}", "offer/offer-rejected.html",
                Set.of("customerName", "offerNumber"));
        register(EmailTemplateKey.WELCOME, "Asenovo'ya hos geldiniz", "auth/welcome.html",
                Set.of("customerName", "panelUrl"));
        register(EmailTemplateKey.PASSWORD_RESET, "Sifre sifirlama talebi", "auth/password-reset.html",
                Set.of("customerName", "resetUrl"));
        register(EmailTemplateKey.GENERIC_NOTIFICATION, "{{title}}", "system/generic-notification.html",
                Set.of("title", "message"));
        register(EmailTemplateKey.TEST_EMAIL, "Asenovo email provider testi", "system/test-email.html",
                Set.of("panelUrl"));
        register(EmailTemplateKey.MARKETING_CONTACT_MESSAGE, "ASENOVO Iletisim Formu - {{companyOrName}}", "system/marketing-contact-message.html",
                Set.of("name", "company", "phone", "email", "message"));
        register(EmailTemplateKey.MARKETING_DEMO_REQUEST, "ASENOVO Demo Talebi - {{companyOrName}}", "system/marketing-demo-request.html",
                Set.of("name", "company", "phone", "email", "companySize"));
        register(EmailTemplateKey.MARKETING_PLAN_REQUEST, "ASENOVO Plan Talebi - {{plan}}", "system/marketing-plan-request.html",
                Set.of("plan", "name", "company", "phone", "email"));
        register(EmailTemplateKey.MARKETING_TRIAL_READY, "ASENOVO Demo Ortaminiz Hazir", "system/marketing-trial-ready.html",
                Set.of("loginUrl", "username", "temporaryPassword", "expiresAt", "tenantSlug"));
        register(EmailTemplateKey.MARKETING_TRIAL_REMINDER, "ASENOVO Demo Erisim Bilgileriniz", "system/marketing-trial-reminder.html",
                Set.of("loginUrl", "username", "temporaryPassword", "expiresAt", "tenantSlug"));
    }

    public EmailTemplateDefinition get(EmailTemplateKey key) {
        EmailTemplateDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown email template key: " + key);
        }
        return definition;
    }

    private void register(EmailTemplateKey key, String subjectPattern, String templatePath, Set<String> requiredVariables) {
        definitions.put(key, new EmailTemplateDefinition(key, subjectPattern, templatePath, requiredVariables));
    }
}
