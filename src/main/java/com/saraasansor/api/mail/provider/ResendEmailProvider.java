package com.saraasansor.api.mail.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.mail.config.EmailProperties;
import com.saraasansor.api.mail.dto.EmailRequest;
import com.saraasansor.api.mail.service.EmailAuthenticationException;
import com.saraasansor.api.mail.service.EmailAuthorizationException;
import com.saraasansor.api.mail.service.EmailDeliveryException;
import com.saraasansor.api.mail.service.EmailProviderUnavailableException;
import com.saraasansor.api.mail.service.EmailValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("resend")
public class ResendEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);

    private final EmailProperties emailProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailProvider(EmailProperties emailProperties, ObjectMapper objectMapper) {
        this.emailProperties = emailProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(emailProperties.getResend().getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public void send(EmailRequest request) {
        String apiKey = normalizeApiKey(emailProperties.getResend().getApiKey());
        if (!StringUtils.hasText(apiKey)) {
            throw new EmailAuthenticationException("Resend API key is not configured");
        }

        try {
            String payload = buildPayload(request);
            log.info(
                    "Resend request prepared url={} apiKeyPresent={} apiKeyPrefix={} apiKeyLength={} tenantId={} to={} from={} subject={} payload={}",
                    emailProperties.getResend().getApiUrl(),
                    true,
                    safePrefix(apiKey),
                    apiKey.length(),
                    request.getTenantId(),
                    request.getTo(),
                    request.getFrom(),
                    request.getSubject(),
                    payload
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(emailProperties.getResend().getApiUrl()))
                    .timeout(Duration.ofSeconds(emailProperties.getResend().getReadTimeoutSeconds()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "asenovo-backend/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error(
                        "Resend email failed status={} category={} tenantId={} to={} subject={} body={}",
                        response.statusCode(),
                        statusCategory(response.statusCode()),
                        request.getTenantId(),
                        request.getTo(),
                        request.getSubject(),
                        truncate(response.body())
                );
                throw mapStatus(response.statusCode());
            }

            String messageId = extractMessageId(response.body());
            log.info(
                    "Resend email delivered tenantId={} to={} subject={} messageId={}",
                    request.getTenantId(),
                    request.getTo(),
                    request.getSubject(),
                    messageId
            );
        } catch (HttpTimeoutException ex) {
            log.error(
                    "Resend timeout tenantId={} to={} subject={}",
                    request.getTenantId(),
                    request.getTo(),
                    request.getSubject(),
                    ex
            );
            throw new EmailDeliveryException("Resend request timed out", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("Resend request was interrupted", ex);
        } catch (IOException ex) {
            log.error(
                    "Resend I/O failure tenantId={} to={} subject={}",
                    request.getTenantId(),
                    request.getTo(),
                    request.getSubject(),
                    ex
            );
            throw new EmailProviderUnavailableException("Resend request failed: " + ex.getMessage(), ex);
        }
    }

    private String buildPayload(EmailRequest request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", request.getFrom());
        payload.put("to", List.of(request.getTo()));
        payload.put("subject", request.getSubject());
        payload.put("html", request.getHtml());
        if (StringUtils.hasText(request.getReplyTo())) {
            payload.put("reply_to", request.getReplyTo());
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String extractMessageId(String body) {
        if (!StringUtils.hasText(body)) {
            return "unknown";
        }
        try {
            Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {
            });
            Object id = response.get("id");
            return id == null ? "unknown" : String.valueOf(id);
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private EmailDeliveryException mapStatus(int statusCode) {
        if (statusCode == 401) {
            return new EmailAuthenticationException("Resend authentication failed: missing or invalid Authorization header/API key");
        }
        if (statusCode == 403) {
            return new EmailAuthorizationException("Resend rejected the request: key/domain/recipient permission issue");
        }
        if (statusCode >= 400 && statusCode < 500) {
            return new EmailValidationException("Resend validation failed with HTTP " + statusCode);
        }
        if (statusCode >= 500) {
            return new EmailProviderUnavailableException("Resend provider unavailable with HTTP " + statusCode);
        }
        return new EmailDeliveryException("Resend returned HTTP " + statusCode);
    }

    private String statusCategory(int statusCode) {
        if (statusCode == 401) {
            return "AUTHENTICATION";
        }
        if (statusCode == 403) {
            return "AUTHORIZATION";
        }
        if (statusCode >= 400 && statusCode < 500) {
            return "VALIDATION";
        }
        if (statusCode >= 500) {
            return "PROVIDER";
        }
        return "UNKNOWN";
    }

    private String normalizeApiKey(String value) {
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
