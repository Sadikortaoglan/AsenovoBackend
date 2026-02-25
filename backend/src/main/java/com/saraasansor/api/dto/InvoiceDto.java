package com.saraasansor.api.dto;

import com.saraasansor.api.model.InvoiceRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceDto {
    private Long id;
    private String invoiceNo;

    @NotNull
    private LocalDate invoiceDate;

    @NotNull
    private String direction;

    private String profile;
    private String status;
    private String senderName;
    private String senderVknTckn;
    private String receiverName;
    private String receiverVknTckn;
    private String currency = "TRY";

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    private String note;
    private String source;
    private Long maintenancePlanId;

    public static InvoiceDto fromEntity(InvoiceRecord entity) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(entity.getId());
        dto.setInvoiceNo(entity.getInvoiceNo());
        dto.setInvoiceDate(entity.getInvoiceDate());
        dto.setDirection(entity.getDirection().name());
        dto.setProfile(entity.getProfile());
        dto.setStatus(entity.getStatus());
        dto.setSenderName(entity.getSenderName());
        dto.setSenderVknTckn(entity.getSenderVknTckn());
        dto.setReceiverName(entity.getReceiverName());
        dto.setReceiverVknTckn(entity.getReceiverVknTckn());
        dto.setCurrency(entity.getCurrency());
        dto.setAmount(entity.getAmount());
        dto.setNote(entity.getNote());
        dto.setSource(entity.getSource());
        dto.setMaintenancePlanId(entity.getMaintenancePlan() != null ? entity.getMaintenancePlan().getId() : null);
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderVknTckn() { return senderVknTckn; }
    public void setSenderVknTckn(String senderVknTckn) { this.senderVknTckn = senderVknTckn; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverVknTckn() { return receiverVknTckn; }
    public void setReceiverVknTckn(String receiverVknTckn) { this.receiverVknTckn = receiverVknTckn; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getMaintenancePlanId() { return maintenancePlanId; }
    public void setMaintenancePlanId(Long maintenancePlanId) { this.maintenancePlanId = maintenancePlanId; }
}
