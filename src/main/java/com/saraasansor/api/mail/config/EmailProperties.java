package com.saraasansor.api.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    private String provider = "resend";
    private String testRecipient;
    private Retry retry = new Retry();
    private Resend resend = new Resend();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTestRecipient() {
        return testRecipient;
    }

    public void setTestRecipient(String testRecipient) {
        this.testRecipient = testRecipient;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Resend getResend() {
        return resend;
    }

    public void setResend(Resend resend) {
        this.resend = resend;
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialBackoffMillis = 250;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMillis() {
            return initialBackoffMillis;
        }

        public void setInitialBackoffMillis(long initialBackoffMillis) {
            this.initialBackoffMillis = initialBackoffMillis;
        }
    }

    public static class Resend {
        private String apiKey;
        private String apiUrl = "https://api.resend.com/emails";
        private String from = "noreply@mail.asenovo.com";
        private String allowedFromDomain = "mail.asenovo.com";
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 15;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getAllowedFromDomain() {
            return allowedFromDomain;
        }

        public void setAllowedFromDomain(String allowedFromDomain) {
            this.allowedFromDomain = allowedFromDomain;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
        }
    }
}
