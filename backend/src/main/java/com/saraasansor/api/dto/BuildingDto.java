package com.saraasansor.api.dto;

import com.saraasansor.api.model.Building;

public class BuildingDto {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String district;

    public BuildingDto() {
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public static BuildingDto fromEntity(Building building) {
        BuildingDto dto = new BuildingDto();
        dto.setId(building.getId());
        dto.setName(building.getName());
        dto.setAddress(building.getAddress());
        dto.setCity(building.getCity());
        dto.setDistrict(building.getDistrict());
        return dto;
    }
}
