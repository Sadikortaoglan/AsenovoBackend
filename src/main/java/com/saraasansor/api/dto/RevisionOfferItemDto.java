package com.saraasansor.api.dto;

import com.saraasansor.api.model.RevisionOfferItem;

public class RevisionOfferItemDto {

    private Long id;
    private Long partId;
    private String partName;
    private Double unitPrice;
    private Integer quantity;
    private String description;

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

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static RevisionOfferItemDto fromEntity(RevisionOfferItem item) {
        RevisionOfferItemDto dto = new RevisionOfferItemDto();
        dto.setId(item.getId());
        if (item.getPart() != null) {
            dto.setPartId(item.getPart().getId());
            dto.setPartName(item.getPart().getName());
            dto.setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : item.getPart().getUnitPrice());
        } else {
            dto.setUnitPrice(item.getUnitPrice());
        }
        dto.setQuantity(item.getQuantity());
        dto.setDescription(item.getDescription());
        return dto;
    }
}
