package com.saraasansor.api.dto;

import com.saraasansor.api.model.Brand;

public class BrandLookupDto {

    private Long id;
    private String name;

    public static BrandLookupDto fromEntity(Brand brand) {
        BrandLookupDto dto = new BrandLookupDto();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
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
}
