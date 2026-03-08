package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.saraasansor.api.model.B2BCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class CreateB2BUnitRequest {

    @NotBlank(message = "Name is required")
    @JsonAlias({"cariAdi"})
    private String name;

    @Pattern(regexp = "^(\\d{10}|\\d{11})$", message = "Tax number must be 10 or 11 digits")
    private String taxNumber;

    private String taxOffice;

    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone format is invalid")
    private String phone;

    @Email(message = "Email format is invalid")
    private String email;

    private Long groupId;

    private B2BCurrency currency;

    @DecimalMin(value = "0.0", inclusive = true, message = "Risk limit must be zero or positive")
    private BigDecimal riskLimit;

    private String address;
    private String description;
    private String portalUsername;
    private String portalPassword;

    public CreateB2BUnitRequest() {
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

    public String getPortalPassword() {
        return portalPassword;
    }

    public void setPortalPassword(String portalPassword) {
        this.portalPassword = portalPassword;
    }
}
