package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "edm_settings")
public class EdmSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;

    private String email;

    @Column(name = "invoice_series_earchive")
    private String invoiceSeriesEarchive;

    @Column(name = "invoice_series_efatura")
    private String invoiceSeriesEfatura;

    @Column(nullable = false)
    private String mode = "PRODUCTION";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInvoiceSeriesEarchive() {
        return invoiceSeriesEarchive;
    }

    public void setInvoiceSeriesEarchive(String invoiceSeriesEarchive) {
        this.invoiceSeriesEarchive = invoiceSeriesEarchive;
    }

    public String getInvoiceSeriesEfatura() {
        return invoiceSeriesEfatura;
    }

    public void setInvoiceSeriesEfatura(String invoiceSeriesEfatura) {
        this.invoiceSeriesEfatura = invoiceSeriesEfatura;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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
