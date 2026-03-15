package com.saraasansor.api.marketing.dto;

public class TrialSubmissionResultDto {

    private String message;
    private TrialProvisionResponseDto response;

    public TrialSubmissionResultDto() {
    }

    public TrialSubmissionResultDto(String message, TrialProvisionResponseDto response) {
        this.message = message;
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TrialProvisionResponseDto getResponse() {
        return response;
    }

    public void setResponse(TrialProvisionResponseDto response) {
        this.response = response;
    }
}
