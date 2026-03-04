package com.saraasansor.api.dto;

import com.saraasansor.api.model.Facility;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateFacilityRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "b2bUnitId is required")
    private Long b2bUnitId;

    @Pattern(regexp = "^(\\d{10}|\\d{11})$", message = "Tax number must be 10 or 11 digits")
    private String taxNumber;

    private String taxOffice;

    private Facility.FacilityType type;

    private Facility.FacilityInvoiceType invoiceType;

    private String companyTitle;

    private String authorizedFirstName;

    private String authorizedLastName;

    @Email(message = "Email format is invalid")
    private String email;

    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone format is invalid")
    private String phone;

    private String facilityType;

    private String attendantFullName;

    private String managerFlatNo;

    private String doorPassword;

    @Min(value = 0, message = "Floor count must be zero or positive")
    private Integer floorCount;

    private Long cityId;

    private Long districtId;

    private Long neighborhoodId;

    private Long regionId;

    private String addressText;

    private String description;

    private Facility.FacilityStatus status;

    @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
    private BigDecimal mapLat;

    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
    private BigDecimal mapLng;

    private String mapAddressQuery;

    private String attachmentUrl;

    public CreateFacilityRequest() {
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

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public Long getNeighborhoodId() {
        return neighborhoodId;
    }

    public void setNeighborhoodId(Long neighborhoodId) {
        this.neighborhoodId = neighborhoodId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
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
}
