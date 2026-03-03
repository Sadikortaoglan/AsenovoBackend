package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b2b_unit_id", nullable = false)
    private B2BUnit b2bUnit;

    @Column(name = "tax_number", length = 11)
    private String taxNumber;

    @Column(name = "tax_office")
    private String taxOffice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacilityType type = FacilityType.TUZEL_KISI;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private FacilityInvoiceType invoiceType = FacilityInvoiceType.TICARI_FATURA;

    @Column(name = "company_title")
    private String companyTitle;

    @Column(name = "authorized_first_name")
    private String authorizedFirstName;

    @Column(name = "authorized_last_name")
    private String authorizedLastName;

    @Column
    private String email;

    @Column
    private String phone;

    @Column(name = "facility_type")
    private String facilityType;

    @Column(name = "attendant_full_name")
    private String attendantFullName;

    @Column(name = "manager_flat_no")
    private String managerFlatNo;

    @Column(name = "door_password")
    private String doorPassword;

    @Column(name = "floor_count")
    private Integer floorCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "address_text", columnDefinition = "TEXT")
    private String addressText;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacilityStatus status = FacilityStatus.ACTIVE;

    @Column(name = "map_lat", precision = 10, scale = 7)
    private BigDecimal mapLat;

    @Column(name = "map_lng", precision = 10, scale = 7)
    private BigDecimal mapLng;

    @Column(name = "map_address_query")
    private String mapAddressQuery;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum FacilityType {
        TUZEL_KISI,
        GERCEK_KISI
    }

    public enum FacilityInvoiceType {
        TICARI_FATURA,
        E_ARSIV,
        E_FATURA
    }

    public enum FacilityStatus {
        ACTIVE,
        PASSIVE
    }

    public Facility() {
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public B2BUnit getB2bUnit() {
        return b2bUnit;
    }

    public void setB2bUnit(B2BUnit b2bUnit) {
        this.b2bUnit = b2bUnit;
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

    public FacilityType getType() {
        return type;
    }

    public void setType(FacilityType type) {
        this.type = type;
    }

    public FacilityInvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(FacilityInvoiceType invoiceType) {
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

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Neighborhood getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(Neighborhood neighborhood) {
        this.neighborhood = neighborhood;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
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

    public FacilityStatus getStatus() {
        return status;
    }

    public void setStatus(FacilityStatus status) {
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
