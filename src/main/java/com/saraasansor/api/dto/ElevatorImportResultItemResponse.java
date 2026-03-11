package com.saraasansor.api.dto;

public class ElevatorImportResultItemResponse {

    public enum Status {
        SUCCESS,
        FAILED
    }

    private int rowNumber;
    private String status;
    private String message;
    private String elevatorName;
    private String facilityName;

    public ElevatorImportResultItemResponse() {
    }

    public ElevatorImportResultItemResponse(int rowNumber,
                                            String status,
                                            String message,
                                            String elevatorName,
                                            String facilityName) {
        this.rowNumber = rowNumber;
        this.status = status;
        this.message = message;
        this.elevatorName = elevatorName;
        this.facilityName = facilityName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getElevatorName() {
        return elevatorName;
    }

    public void setElevatorName(String elevatorName) {
        this.elevatorName = elevatorName;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }
}
