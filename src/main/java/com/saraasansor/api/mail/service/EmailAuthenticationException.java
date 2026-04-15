package com.saraasansor.api.mail.service;

public class EmailAuthenticationException extends EmailDeliveryException {

    public EmailAuthenticationException(String message) {
        super(message);
    }

    public EmailAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
