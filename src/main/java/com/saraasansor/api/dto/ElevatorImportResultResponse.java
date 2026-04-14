package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ElevatorImportResultResponse {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<ElevatorImportResultItemResponse> items = new ArrayList<>();
    private List<ErrorDetail> errorDetails = new ArrayList<>();

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getFailedCount() {
        return failureCount;
    }

    public void setFailedCount(int failedCount) {
        this.failureCount = failedCount;
    }

    public List<ElevatorImportResultItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ElevatorImportResultItemResponse> items) {
        this.items = items;
    }

    public void addItem(ElevatorImportResultItemResponse item) {
        this.items.add(item);
    }

    public List<ErrorDetail> getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(List<ErrorDetail> errorDetails) {
        this.errorDetails = errorDetails;
    }

    public void addErrorDetail(int row, String reason) {
        this.errorDetails.add(new ErrorDetail(row, reason));
    }

    public static class ErrorDetail {
        private int row;
        private String reason;

        public ErrorDetail() {
        }

        public ErrorDetail(int row, String reason) {
            this.row = row;
            this.reason = reason;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
