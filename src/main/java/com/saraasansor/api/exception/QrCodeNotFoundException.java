package com.saraasansor.api.exception;

public class QrCodeNotFoundException extends RuntimeException {
    public QrCodeNotFoundException(String message) {
        super(message);
    }
}
