package com.saraasansor.api.mail.service;

public class EmailValidationException extends EmailDeliveryException {

    public EmailValidationException(String message) {
        super(message);
    }

    public EmailValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
