package com.saraasansor.api.dto;

public class VknValidationResponse {
    private boolean valid;
    private String type;
    private String message;

    public VknValidationResponse(boolean valid, String type, String message) {
        this.valid = valid;
        this.type = type;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
