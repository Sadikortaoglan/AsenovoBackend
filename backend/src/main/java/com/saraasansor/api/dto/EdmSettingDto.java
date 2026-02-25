package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EdmSettingDto {
    private Long id;

    @NotBlank
    private String username;

    @Size(min = 6)
    private String password;

    private String email;

    @Size(max = 3)
    private String invoiceSeriesEarchive;

    @Size(max = 3)
    private String invoiceSeriesEfatura;

    private String mode;
    private boolean passwordConfigured;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInvoiceSeriesEarchive() { return invoiceSeriesEarchive; }
    public void setInvoiceSeriesEarchive(String invoiceSeriesEarchive) { this.invoiceSeriesEarchive = invoiceSeriesEarchive; }
    public String getInvoiceSeriesEfatura() { return invoiceSeriesEfatura; }
    public void setInvoiceSeriesEfatura(String invoiceSeriesEfatura) { this.invoiceSeriesEfatura = invoiceSeriesEfatura; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isPasswordConfigured() { return passwordConfigured; }
    public void setPasswordConfigured(boolean passwordConfigured) { this.passwordConfigured = passwordConfigured; }
}
