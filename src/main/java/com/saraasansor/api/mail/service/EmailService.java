package com.saraasansor.api.mail.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${MAIL_FROM:support@asenovo.com}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String configuredHost;

    @Value("${spring.mail.port:0}")
    private int configuredPort;

    @Value("${spring.mail.username:}")
    private String configuredUsername;

    @Value("${spring.mail.password:}")
    private String configuredPassword;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendEmail(String to, String subject, String body) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
            throw new EmailDeliveryException("Email recipient, subject, and body are required");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new EmailDeliveryException("JavaMailSender is not configured");
        }

        try {
            log.info(
                    "SMTP send attempt host={} port={} username={} from={} passwordLength={} passwordFingerprint={} to={} subject={}",
                    configuredHost,
                    configuredPort,
                    configuredUsername,
                    mailFrom,
                    configuredPassword == null ? 0 : configuredPassword.length(),
                    fingerprint(configuredPassword),
                    to,
                    subject
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("Transactional email sent to={} subject={}", to, subject);
        } catch (MailAuthenticationException ex) {
            log.error(
                    "SMTP authentication failed host={} port={} username={} from={} passwordLength={} passwordFingerprint={} recipient={} subject={} cause={}",
                    configuredHost,
                    configuredPort,
                    configuredUsername,
                    mailFrom,
                    configuredPassword == null ? 0 : configuredPassword.length(),
                    fingerprint(configuredPassword),
                    to,
                    subject,
                    rootCauseMessage(ex),
                    ex
            );
            throw new EmailDeliveryException("SMTP authentication failed: " + rootCauseMessage(ex), ex);
        } catch (MailSendException ex) {
            log.error("SMTP send failed recipient={} subject={} cause={}", to, subject, rootCauseMessage(ex), ex);
            throw new EmailDeliveryException("SMTP send failed: " + rootCauseMessage(ex), ex);
        } catch (MailException ex) {
            log.error("Email delivery failed recipient={} subject={} cause={}", to, subject, rootCauseMessage(ex), ex);
            throw new EmailDeliveryException("Email delivery failed: " + rootCauseMessage(ex), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to={} subject={} cause={}", to, subject, rootCauseMessage(ex), ex);
            throw new EmailDeliveryException("Unexpected error while sending email: " + rootCauseMessage(ex), ex);
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String fingerprint(String secret) {
        if (!StringUtils.hasText(secret)) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            return "sha256-unavailable";
        }
    }
}
