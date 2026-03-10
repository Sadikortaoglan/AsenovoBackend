package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitInvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceResponse {

    private Long id;
    private B2BUnitInvoice.InvoiceType invoiceType;
    private Long b2bUnitId;
    private Long facilityId;
    private Long elevatorId;
    private Long warehouseId;
    private LocalDate invoiceDate;
    private String description;
    private BigDecimal subTotal;
    private BigDecimal vatTotal;
    private BigDecimal grandTotal;
    private B2BUnitInvoice.InvoiceStatus status;
    private List<InvoiceLineResponse> lines = new ArrayList<>();

    public InvoiceResponse() {
    }

    public static InvoiceResponse fromEntity(B2BUnitInvoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceType(invoice.getInvoiceType());
        response.setB2bUnitId(invoice.getB2bUnit() != null ? invoice.getB2bUnit().getId() : null);
        response.setFacilityId(invoice.getFacility() != null ? invoice.getFacility().getId() : null);
        response.setElevatorId(invoice.getElevator() != null ? invoice.getElevator().getId() : null);
        response.setWarehouseId(invoice.getWarehouse() != null ? invoice.getWarehouse().getId() : null);
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDescription(invoice.getDescription());
        response.setSubTotal(invoice.getSubTotal());
        response.setVatTotal(invoice.getVatTotal());
        response.setGrandTotal(invoice.getGrandTotal());
        response.setStatus(invoice.getStatus());
        response.setLines(invoice.getLines().stream().map(InvoiceLineResponse::fromEntity).toList());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public B2BUnitInvoice.InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(B2BUnitInvoice.InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
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

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getVatTotal() {
        return vatTotal;
    }

    public void setVatTotal(BigDecimal vatTotal) {
        this.vatTotal = vatTotal;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public B2BUnitInvoice.InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(B2BUnitInvoice.InvoiceStatus status) {
        this.status = status;
    }

    public List<InvoiceLineResponse> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineResponse> lines) {
        this.lines = lines;
    }
}
