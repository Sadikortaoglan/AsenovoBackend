package com.saraasansor.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "revision_standard_id")
    private Long revisionStandardId;

    @Column(name = "parts_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal partsTotal = BigDecimal.ZERO;

    @Column(name = "labor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal laborTotal = BigDecimal.ZERO;

    @Column(name = "labor_description", columnDefinition = "TEXT")
    private String laborDescription;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "revision_offer_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Status status = Status.DRAFT;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_to_sale_id")
    private B2BUnitInvoice convertedToSale;

    @OneToMany(mappedBy = "revisionOffer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RevisionOfferItem> items = new ArrayList<>();

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

    public Long getRevisionStandardId() {
        return revisionStandardId;
    }

    public void setRevisionStandardId(Long revisionStandardId) {
        this.revisionStandardId = revisionStandardId;
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

    public String getLaborDescription() {
        return laborDescription;
    }

    public void setLaborDescription(String laborDescription) {
        this.laborDescription = laborDescription;
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

    public B2BUnitInvoice getConvertedToSale() {
        return convertedToSale;
    }

    public void setConvertedToSale(B2BUnitInvoice convertedToSale) {
        this.convertedToSale = convertedToSale;
    }

    public List<RevisionOfferItem> getItems() {
        return items;
    }

    public void setItems(List<RevisionOfferItem> items) {
        this.items = items;
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
