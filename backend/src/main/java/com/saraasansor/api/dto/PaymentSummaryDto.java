package com.saraasansor.api.dto;

public class PaymentSummaryDto {
    private Integer totalCount;
    private Double totalAmount;

    public PaymentSummaryDto() {
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
