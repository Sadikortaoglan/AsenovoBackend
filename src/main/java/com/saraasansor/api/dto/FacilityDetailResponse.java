package com.saraasansor.api.dto;

import com.saraasansor.api.model.Facility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FacilityDetailResponse {

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
    private String city;
    private String district;
    private String neighborhood;
    private String region;
    private String addressText;
    private String description;
    private Facility.FacilityStatus status;
    private BigDecimal mapLat;
    private BigDecimal mapLng;
    private String attachmentName;
    private String attachmentPreviewUrl;
    private String reportUrl;
    private List<FacilityElevatorSummaryResponse> elevators = new ArrayList<>();

    public FacilityDetailResponse() {
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

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getAttachmentPreviewUrl() {
        return attachmentPreviewUrl;
    }

    public void setAttachmentPreviewUrl(String attachmentPreviewUrl) {
        this.attachmentPreviewUrl = attachmentPreviewUrl;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public java.util.List<FacilityElevatorSummaryResponse> getElevators() {
        return elevators;
    }

    public void setElevators(java.util.List<FacilityElevatorSummaryResponse> elevators) {
        this.elevators = elevators;
    }
}
