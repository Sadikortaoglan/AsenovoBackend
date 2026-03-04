package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnit;

public class B2BUnitLookupDto {

    private Long id;
    private String name;

    public B2BUnitLookupDto() {
    }

    public static B2BUnitLookupDto fromEntity(B2BUnit entity) {
        B2BUnitLookupDto dto = new B2BUnitLookupDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
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
