package com.saraasansor.api.mail.service;

public class EmailAuthorizationException extends EmailDeliveryException {

    public EmailAuthorizationException(String message) {
        super(message);
    }

    public EmailAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
