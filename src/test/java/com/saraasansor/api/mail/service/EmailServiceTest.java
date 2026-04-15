package com.saraasansor.api.mail.service;

import com.saraasansor.api.mail.config.EmailProperties;
import com.saraasansor.api.mail.dto.EmailRequest;
import com.saraasansor.api.mail.provider.EmailProvider;
import com.saraasansor.api.mail.template.EmailTemplateRegistry;
import com.saraasansor.api.mail.template.EmailTemplateRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailServiceTest {

    private final EmailTemplateRenderer templateRenderer = new EmailTemplateRenderer(new EmailTemplateRegistry());
    private final Executor directExecutor = Runnable::run;

    @Test
    void shouldApplyDefaultFromAndRetryBeforeSuccess() {
        EmailProperties properties = new EmailProperties();
        properties.getResend().setFrom("noreply@mail.asenovo.com");
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialBackoffMillis(0);

        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<EmailRequest> delivered = new AtomicReference<>();
        EmailProvider provider = request -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new EmailDeliveryException("temporary failure");
            }
            delivered.set(request);
        };

        EmailService emailService = new EmailService(
                Map.of("resend", provider),
                properties,
                templateRenderer,
                directExecutor,
                new MockEnvironment()
        );

        EmailRequest request = new EmailRequest();
        request.setTo("customer@example.com");
        request.setSubject("Subject");
        request.setHtml("<p>Hello</p>");

        emailService.send(request);

        assertThat(attempts.get()).isEqualTo(3);
        assertThat(delivered.get().getFrom()).isEqualTo("noreply@mail.asenovo.com");
        assertThat(delivered.get().getTenantId()).isEqualTo("platform");
    }

    @Test
    void shouldRejectInvalidRecipient() {
        EmailProperties properties = new EmailProperties();
        EmailProvider provider = request -> {
        };

        EmailService emailService = new EmailService(
                Map.of("resend", provider),
                properties,
                templateRenderer,
                directExecutor,
                new MockEnvironment()
        );

        EmailRequest request = new EmailRequest();
        request.setTo("not-an-email");
        request.setSubject("Subject");
        request.setHtml("<p>Hello</p>");

        assertThatThrownBy(() -> emailService.send(request))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("Invalid recipient");
    }

    @Test
    void shouldRejectProductionPlaceholderContent() {
        EmailProperties properties = new EmailProperties();
        EmailProvider provider = request -> {
        };

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        EmailService emailService = new EmailService(
                Map.of("resend", provider),
                properties,
                templateRenderer,
                directExecutor,
                environment
        );

        EmailRequest request = new EmailRequest();
        request.setTo("customer@example.com");
        request.setSubject("Hello World");
        request.setHtml("<p>placeholder</p>");

        assertThatThrownBy(() -> emailService.send(request))
                .isInstanceOf(EmailValidationException.class)
                .hasMessageContaining("Production email contains test");
    }
}
