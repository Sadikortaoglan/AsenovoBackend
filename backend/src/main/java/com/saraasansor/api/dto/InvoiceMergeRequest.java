package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class InvoiceMergeRequest {
    @NotEmpty
    private List<Long> invoiceIds;

    public List<Long> getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(List<Long> invoiceIds) {
        this.invoiceIds = invoiceIds;
    }
}
