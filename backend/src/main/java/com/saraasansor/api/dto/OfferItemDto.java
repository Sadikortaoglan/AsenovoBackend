package com.saraasansor.api.dto;

import com.saraasansor.api.model.OfferItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OfferItemDto {
    private Long id;
    
    @NotNull(message = "Part ID cannot be empty")
    private Long partId;
    
    private String partName;
    
    @NotNull(message = "Quantity cannot be empty")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @NotNull(message = "Unit price cannot be empty")
    @Positive(message = "Unit price must be positive")
    private Double unitPrice;
    
    private Double lineTotal;

    public OfferItemDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(Double lineTotal) {
        this.lineTotal = lineTotal;
    }

    public static OfferItemDto fromEntity(OfferItem item) {
        OfferItemDto dto = new OfferItemDto();
        dto.setId(item.getId());
        if (item.getPart() != null) {
            dto.setPartId(item.getPart().getId());
            dto.setPartName(item.getPart().getName());
        }
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }
}
