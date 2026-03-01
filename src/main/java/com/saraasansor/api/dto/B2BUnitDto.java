package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.B2BUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class B2BUnitDto {

    private Long id;
    private String name;
    private String taxNumber;
    private String taxOffice;
    private String phone;
    private String email;
    private Long groupId;
    private String groupName;
    private B2BCurrency currency;
    private BigDecimal riskLimit;
    private String address;
    private String description;
    private String portalUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public B2BUnitDto() {
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public B2BCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(B2BCurrency currency) {
        this.currency = currency;
    }

    public BigDecimal getRiskLimit() {
        return riskLimit;
    }

    public void setRiskLimit(BigDecimal riskLimit) {
        this.riskLimit = riskLimit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPortalUsername() {
        return portalUsername;
    }

    public void setPortalUsername(String portalUsername) {
        this.portalUsername = portalUsername;
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

    public static B2BUnitDto fromEntity(B2BUnit entity) {
        B2BUnitDto dto = new B2BUnitDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTaxNumber(entity.getTaxNumber());
        dto.setTaxOffice(entity.getTaxOffice());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setCurrency(entity.getCurrency());
        dto.setRiskLimit(entity.getRiskLimit());
        dto.setAddress(entity.getAddress());
        dto.setDescription(entity.getDescription());
        dto.setPortalUsername(entity.getPortalUsername());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getGroup() != null) {
            dto.setGroupId(entity.getGroup().getId());
            dto.setGroupName(entity.getGroup().getName());
        }

        return dto;
    }
}
