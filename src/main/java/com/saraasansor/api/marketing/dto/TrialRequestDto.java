package com.saraasansor.api.marketing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TrialRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name is too long")
    private String name;

    @Size(max = 255, message = "Company is too long")
    private String company;

    @NotBlank(message = "Phone is required")
    @Size(max = 50, message = "Phone is too long")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email is too long")
    private String email;

    @Size(max = 100, message = "Company size is too long")
    private String companySize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalize(name);
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = normalize(company);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalize(email);
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = normalize(companySize);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
