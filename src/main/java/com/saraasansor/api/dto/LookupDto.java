package com.saraasansor.api.dto;

public class LookupDto {

    private Long id;
    private String name;
    private Long facilityId;
    private String facilityName;

    public LookupDto() {
    }

    public LookupDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public LookupDto(Long id, String name, Long facilityId, String facilityName) {
        this.id = id;
        this.name = name;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
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

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }
}
