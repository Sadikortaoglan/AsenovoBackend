package com.saraasansor.api.dto;

import com.saraasansor.api.model.Facility;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FacilityDto {

    private Long id;
    private String name;
    private Long b2bUnitId;
    private String b2bUnitName;
    private String taxNumber;
    private String taxOffice;
    private Facility.FacilityType type;
    private Facility.FacilityInvoiceType invoiceType;
    private String companyTitle;
    private String authorizedFirstName;
    private String authorizedLastName;
    private String email;
    private String phone;
    private String facilityType;
    private String attendantFullName;
    private String managerFlatNo;
    private String doorPassword;
    private Integer floorCount;
    private Long cityId;
    private String cityName;
    private Long districtId;
    private String districtName;
    private Long neighborhoodId;
    private String neighborhoodName;
    private Long regionId;
    private String regionName;
    private String addressText;
    private String description;
    private Facility.FacilityStatus status;
    private BigDecimal mapLat;
    private BigDecimal mapLng;
    private String mapAddressQuery;
    private String attachmentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FacilityDto() {
    }

    public static FacilityDto fromEntity(Facility entity, boolean includeDoorPassword) {
        FacilityDto dto = new FacilityDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTaxNumber(entity.getTaxNumber());
        dto.setTaxOffice(entity.getTaxOffice());
        dto.setType(entity.getType());
        dto.setInvoiceType(entity.getInvoiceType());
        dto.setCompanyTitle(entity.getCompanyTitle());
        dto.setAuthorizedFirstName(entity.getAuthorizedFirstName());
        dto.setAuthorizedLastName(entity.getAuthorizedLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setFacilityType(entity.getFacilityType());
        dto.setAttendantFullName(entity.getAttendantFullName());
        dto.setManagerFlatNo(entity.getManagerFlatNo());
        dto.setDoorPassword(includeDoorPassword ? entity.getDoorPassword() : null);
        dto.setFloorCount(entity.getFloorCount());
        dto.setAddressText(entity.getAddressText());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setMapLat(entity.getMapLat());
        dto.setMapLng(entity.getMapLng());
        dto.setMapAddressQuery(entity.getMapAddressQuery());
        dto.setAttachmentUrl(entity.getAttachmentUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getB2bUnit() != null) {
            dto.setB2bUnitId(entity.getB2bUnit().getId());
            dto.setB2bUnitName(entity.getB2bUnit().getName());
        }

        if (entity.getCity() != null) {
            dto.setCityId(entity.getCity().getId());
            dto.setCityName(entity.getCity().getName());
        }

        if (entity.getDistrict() != null) {
            dto.setDistrictId(entity.getDistrict().getId());
            dto.setDistrictName(entity.getDistrict().getName());
        }

        if (entity.getNeighborhood() != null) {
            dto.setNeighborhoodId(entity.getNeighborhood().getId());
            dto.setNeighborhoodName(entity.getNeighborhood().getName());
        }

        if (entity.getRegion() != null) {
            dto.setRegionId(entity.getRegion().getId());
            dto.setRegionName(entity.getRegion().getName());
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

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public String getB2bUnitName() {
        return b2bUnitName;
    }

    public void setB2bUnitName(String b2bUnitName) {
        this.b2bUnitName = b2bUnitName;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getTaxOffice() {
        return taxOffice;
    }

    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public Facility.FacilityType getType() {
        return type;
    }

    public void setType(Facility.FacilityType type) {
        this.type = type;
    }

    public Facility.FacilityInvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(Facility.FacilityInvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getCompanyTitle() {
        return companyTitle;
    }

    public void setCompanyTitle(String companyTitle) {
        this.companyTitle = companyTitle;
    }

    public String getAuthorizedFirstName() {
        return authorizedFirstName;
    }

    public void setAuthorizedFirstName(String authorizedFirstName) {
        this.authorizedFirstName = authorizedFirstName;
    }

    public String getAuthorizedLastName() {
        return authorizedLastName;
    }

    public void setAuthorizedLastName(String authorizedLastName) {
        this.authorizedLastName = authorizedLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public String getAttendantFullName() {
        return attendantFullName;
    }

    public void setAttendantFullName(String attendantFullName) {
        this.attendantFullName = attendantFullName;
    }

    public String getManagerFlatNo() {
        return managerFlatNo;
    }

    public void setManagerFlatNo(String managerFlatNo) {
        this.managerFlatNo = managerFlatNo;
    }

    public String getDoorPassword() {
        return doorPassword;
    }

    public void setDoorPassword(String doorPassword) {
        this.doorPassword = doorPassword;
    }

    public Integer getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public Long getNeighborhoodId() {
        return neighborhoodId;
    }

    public void setNeighborhoodId(Long neighborhoodId) {
        this.neighborhoodId = neighborhoodId;
    }

    public String getNeighborhoodName() {
        return neighborhoodName;
    }

    public void setNeighborhoodName(String neighborhoodName) {
        this.neighborhoodName = neighborhoodName;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Facility.FacilityStatus getStatus() {
        return status;
    }

    public void setStatus(Facility.FacilityStatus status) {
        this.status = status;
    }

    public BigDecimal getMapLat() {
        return mapLat;
    }

    public void setMapLat(BigDecimal mapLat) {
        this.mapLat = mapLat;
    }

    public BigDecimal getMapLng() {
        return mapLng;
    }

    public void setMapLng(BigDecimal mapLng) {
        this.mapLng = mapLng;
    }

    public String getMapAddressQuery() {
        return mapAddressQuery;
    }

    public void setMapAddressQuery(String mapAddressQuery) {
        this.mapAddressQuery = mapAddressQuery;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
