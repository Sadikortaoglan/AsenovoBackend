package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revision_offers")
public class RevisionOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elevator_id", nullable = false)
    private Elevator elevator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_account_id", nullable = false)
    private CurrentAccount currentAccount;

    @Column(name = "parts_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal partsTotal = BigDecimal.ZERO;

    @Column(name = "labor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal laborTotal = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        DRAFT, SENT, APPROVED, REJECTED, CONVERTED_TO_SALE
    }

    public RevisionOffer() {
    }

    public RevisionOffer(Long id, Elevator elevator, Building building, CurrentAccount currentAccount, BigDecimal partsTotal, BigDecimal laborTotal, BigDecimal totalPrice, Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.elevator = elevator;
        this.building = building;
        this.currentAccount = currentAccount;
        this.partsTotal = partsTotal;
        this.laborTotal = laborTotal;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalculate total_price
        totalPrice = partsTotal.add(laborTotal);
    }

    @PrePersist
    protected void onPersist() {
        totalPrice = partsTotal.add(laborTotal);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Elevator getElevator() {
        return elevator;
    }

    public void setElevator(Elevator elevator) {
        this.elevator = elevator;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public CurrentAccount getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(CurrentAccount currentAccount) {
        this.currentAccount = currentAccount;
    }

    public BigDecimal getPartsTotal() {
        return partsTotal;
    }

    public void setPartsTotal(BigDecimal partsTotal) {
        this.partsTotal = partsTotal;
        this.totalPrice = partsTotal.add(laborTotal);
    }

    public BigDecimal getLaborTotal() {
        return laborTotal;
    }

    public void setLaborTotal(BigDecimal laborTotal) {
        this.laborTotal = laborTotal;
        this.totalPrice = partsTotal.add(laborTotal);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
