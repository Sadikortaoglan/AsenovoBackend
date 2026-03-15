package com.saraasansor.api.marketing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name is too long")
    private String name;

    @Size(max = 255, message = "Company is too long")
    private String company;

    @Size(max = 50, message = "Phone is too long")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email is too long")
    private String email;

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message is too long")
    private String message;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = normalize(message);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
