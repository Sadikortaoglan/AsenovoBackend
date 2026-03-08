package com.saraasansor.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class SalesInvoiceCreateRequest {

    @NotNull(message = "facilityId is required")
    private Long facilityId;

    @NotNull(message = "elevatorId is required")
    private Long elevatorId;

    @NotNull(message = "warehouseId is required")
    private Long warehouseId;

    @NotNull(message = "invoiceDate is required")
    private LocalDate invoiceDate;

    private String description;

    @NotEmpty(message = "lines must not be empty")
    @Valid
    private List<InvoiceLineRequest> lines;

    public SalesInvoiceCreateRequest() {
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<InvoiceLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineRequest> lines) {
        this.lines = lines;
    }
}
