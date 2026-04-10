package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockModel;

public class ModelLookupDto {

    private Long id;
    private String name;
    private Long brandId;
    private String brandName;

    public static ModelLookupDto fromEntity(StockModel model) {
        ModelLookupDto dto = new ModelLookupDto();
        dto.setId(model.getId());
        dto.setName(model.getName());
        if (model.getBrand() != null) {
            dto.setBrandId(model.getBrand().getId());
            dto.setBrandName(model.getBrand().getName());
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
}
