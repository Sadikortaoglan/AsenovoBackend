package com.saraasansor.api.marketing.service;

import com.saraasansor.api.marketing.dto.ContactRequestDto;
import com.saraasansor.api.marketing.dto.DemoRequestDto;
import com.saraasansor.api.marketing.dto.PlanRequestDto;
import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingEmailService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${asenovo.marketing.support-email:support@asenovo.com}")
    private String supportEmail;

    @Value("${asenovo.marketing.from-email:no-reply@asenovo.com}")
    private String fromEmail;

    public MarketingEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean sendContactMessage(ContactRequestDto request) {
        return sendEmail(
                supportEmail,
                request.getEmail(),
                buildSubject(request),
                buildBody(request)
        );
    }

    public boolean sendDemoRequestNotification(DemoRequestDto request) {
        return sendEmail(
                supportEmail,
                request.getEmail(),
                "ASENOVO Demo Talebi - " + defaultText(request.getCompany(), request.getName()),
                """
                Yeni demo talebi alindi.

                Ad Soyad: %s
                Sirket: %s
                Telefon: %s
                E-posta: %s
                Sirket Buyuklugu: %s
                """.formatted(
                        defaultText(request.getName()),
                        defaultText(request.getCompany()),
                        defaultText(request.getPhone()),
                        defaultText(request.getEmail()),
                        defaultText(request.getCompanySize())
                )
        );
    }

    public boolean sendPlanRequestNotification(PlanRequestDto request) {
        return sendEmail(
                supportEmail,
                request.getEmail(),
                "ASENOVO Plan Talebi - " + request.getPlan(),
                """
                Yeni plan talebi alindi.

                Plan: %s
                Ad Soyad: %s
                Sirket: %s
                Telefon: %s
                E-posta: %s
                """.formatted(
                        defaultText(request.getPlan()),
                        defaultText(request.getName()),
                        defaultText(request.getCompany()),
                        defaultText(request.getPhone()),
                        defaultText(request.getEmail())
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
                email,
                null,
                existingDemo ? "ASENOVO Demo Erisim Bilgileriniz" : "ASENOVO Demo Ortaminiz Hazir",
                """
                %s

                Giris adresi: %s
                Kullanici adi: %s
                Gecici sifre: %s
                Bitis tarihi (UTC): %s
                Tenant: %s
                """.formatted(
                        existingDemo
                                ? "Bu e-posta ve sirket icin aktif bir demo ortaminiz zaten bulunuyor. Erişim bilgileriniz tekrar gonderildi."
                                : "Demo ortaminiz hazirlandi.",
                        defaultText(response.getLoginUrl()),
                        defaultText(response.getUsername()),
                        defaultText(response.getTemporaryPassword()),
                        response.getExpiresAt(),
                        defaultText(response.getTenantSlug())
                )
        );
    }

    private boolean sendEmail(String to, String replyTo, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("Marketing email skipped because JavaMailSender is not configured. to={} subject={}", to, subject);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setFrom(fromEmail);
            if (StringUtils.hasText(replyTo)) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            logger.info("Marketing email sent successfully to={} subject={}", to, subject);
            return true;
        } catch (Exception ex) {
            String reason = describeMailFailure(ex);
            logger.error("Failed to send marketing email to={} subject={} reason={}", to, subject, reason, ex);
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

    private String buildSubject(ContactRequestDto request) {
        if (StringUtils.hasText(request.getCompany())) {
            return "ASENOVO Iletisim Formu - " + request.getCompany();
        }
        return "ASENOVO Iletisim Formu - " + request.getName();
    }

    private String buildBody(ContactRequestDto request) {
        return """
                Yeni iletisim formu mesaji alindi.

                Ad Soyad: %s
                Sirket: %s
                Telefon: %s
                E-posta: %s

                Mesaj:
                %s
                """.formatted(
                defaultText(request.getName()),
                defaultText(request.getCompany()),
                defaultText(request.getPhone()),
                defaultText(request.getEmail()),
                defaultText(request.getMessage())
        );
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String defaultText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }
}
