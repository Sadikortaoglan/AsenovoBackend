package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ElevatorImportResultResponse {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<ElevatorImportResultItemResponse> items = new ArrayList<>();

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

    public List<ElevatorImportResultItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ElevatorImportResultItemResponse> items) {
        this.items = items;
    }

    public void addItem(ElevatorImportResultItemResponse item) {
        this.items.add(item);
    }
}
