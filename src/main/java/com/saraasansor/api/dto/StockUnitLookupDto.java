package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockUnit;

public class StockUnitLookupDto {

    private Long id;
    private String name;
    private String abbreviation;

    public static StockUnitLookupDto fromEntity(StockUnit stockUnit) {
        StockUnitLookupDto dto = new StockUnitLookupDto();
        dto.setId(stockUnit.getId());
        dto.setName(stockUnit.getName());
        dto.setAbbreviation(stockUnit.getAbbreviation());
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

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }
}
