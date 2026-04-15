package com.saraasansor.api.mail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
public class EmailConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailConfigurationValidator.class);

    private final EmailProperties emailProperties;
    private final Environment environment;

    public EmailConfigurationValidator(EmailProperties emailProperties, Environment environment) {
        this.emailProperties = emailProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String apiKey = normalize(emailProperties.getResend().getApiKey());
        emailProperties.getResend().setApiKey(apiKey);

        boolean present = StringUtils.hasText(apiKey);
        log.info(
                "Resend configuration loaded provider={} url={} from={} apiKeyPresent={} apiKeyPrefix={} apiKeyLength={}",
                emailProperties.getProvider(),
                emailProperties.getResend().getApiUrl(),
                emailProperties.getResend().getFrom(),
                present,
                safePrefix(apiKey),
                present ? apiKey.length() : 0
        );

        if (!present && isStrictProfile()) {
            throw new IllegalStateException("RESEND_API_KEY is required for non-local email provider configuration");
        }
        if (!present) {
            log.warn("RESEND_API_KEY is missing. Email sending is disabled until a key is configured.");
        }
    }

    private boolean isStrictProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String safePrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "none";
        }
        return value.length() <= 5 ? value : value.substring(0, 5);
    }
}
