package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "current_accounts")
public class CurrentAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false, unique = true)
    private Building building;

    @Column(nullable = false)
    private String name;

    @Column(name = "authorized_person", nullable = false)
    private String authorizedPerson;

    @Column(nullable = false)
    private String phone;

    @Column(name = "debt", nullable = false, precision = 14, scale = 2)
    private BigDecimal debt = BigDecimal.ZERO;

    @Column(name = "credit", nullable = false, precision = 14, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CurrentAccount() {
    }

    public CurrentAccount(Long id, Building building, String name, String authorizedPerson, String phone, BigDecimal debt, BigDecimal credit, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.building = building;
        this.name = name;
        this.authorizedPerson = authorizedPerson;
        this.phone = phone;
        this.debt = debt;
        this.credit = credit;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalculate balance
        balance = credit.subtract(debt);
    }

    @PrePersist
    protected void onPersist() {
        balance = credit.subtract(debt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthorizedPerson() {
        return authorizedPerson;
    }

    public void setAuthorizedPerson(String authorizedPerson) {
        this.authorizedPerson = authorizedPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getDebt() {
        return debt;
    }

    public void setDebt(BigDecimal debt) {
        this.debt = debt;
        this.balance = credit.subtract(debt);
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
        this.balance = credit.subtract(debt);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
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
