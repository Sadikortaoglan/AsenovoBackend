package com.saraasansor.api.mail.service;

public class EmailProviderUnavailableException extends EmailDeliveryException {

    public EmailProviderUnavailableException(String message) {
        super(message);
    }

    public EmailProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
