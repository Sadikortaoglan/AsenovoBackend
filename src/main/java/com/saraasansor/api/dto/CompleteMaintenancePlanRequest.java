package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class CompleteMaintenancePlanRequest {
    @NotNull(message = "QR proof ID is required")
    private Long qrProofId;
    
    @NotNull(message = "Photos are required")
    @Size(min = 4, message = "At least 4 photos are required")
    private List<String> photoUrls;
    
    private String note;
    
    private BigDecimal price;
    
    public Long getQrProofId() {
        return qrProofId;
    }
    
    public void setQrProofId(Long qrProofId) {
        this.qrProofId = qrProofId;
    }
    
    public List<String> getPhotoUrls() {
        return photoUrls;
    }
    
    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
