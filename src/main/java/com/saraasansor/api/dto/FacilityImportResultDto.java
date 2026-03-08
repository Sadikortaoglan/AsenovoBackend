package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class FacilityImportResultDto {

    private int readRows;
    private int successRows;
    private int failedRows;
    private List<RowErrorDto> rowErrors = new ArrayList<>();

    public int getReadRows() {
        return readRows;
    }

    public void setReadRows(int readRows) {
        this.readRows = readRows;
    }

    public int getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(int successRows) {
        this.successRows = successRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public List<RowErrorDto> getRowErrors() {
        return rowErrors;
    }

    public void setRowErrors(List<RowErrorDto> rowErrors) {
        this.rowErrors = rowErrors;
    }

    public void addRowError(int rowNumber, String reason) {
        this.rowErrors.add(new RowErrorDto(rowNumber, reason));
    }

    public static class RowErrorDto {
        private int rowNumber;
        private String reason;

        public RowErrorDto() {
        }

        public RowErrorDto(int rowNumber, String reason) {
            this.rowNumber = rowNumber;
            this.reason = reason;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
