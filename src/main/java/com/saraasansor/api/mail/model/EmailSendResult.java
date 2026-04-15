package com.saraasansor.api.mail.model;

public class EmailSendResult {

    private final boolean success;
    private final String message;

    private EmailSendResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static EmailSendResult success() {
        return new EmailSendResult(true, "sent");
    }

    public static EmailSendResult failure(String message) {
        return new EmailSendResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
