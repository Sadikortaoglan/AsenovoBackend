package com.saraasansor.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "parts")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String code;

    @Column(length = 100)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id")
    private VatRate vatRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_group_id")
    private StockGroup stockGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private StockUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private StockModel model;

    @Column(name = "purchase_price")
    private Double purchasePrice;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "stock_entry", nullable = false)
    private Integer stockEntry = 0;

    @Column(name = "stock_exit", nullable = false)
    private Integer stockExit = 0;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public Part() {
    }

    public Part(Long id, String name, String description, Double unitPrice, Integer stock, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (stock == null) {
            stock = 0;
        }
        if (stockEntry == null) {
            stockEntry = 0;
        }
        if (stockExit == null) {
            stockExit = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public VatRate getVatRate() {
        return vatRate;
    }

    public void setVatRate(VatRate vatRate) {
        this.vatRate = vatRate;
    }

    public StockGroup getStockGroup() {
        return stockGroup;
    }

    public void setStockGroup(StockGroup stockGroup) {
        this.stockGroup = stockGroup;
    }

    public StockUnit getUnit() {
        return unit;
    }

    public void setUnit(StockUnit unit) {
        this.unit = unit;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public StockModel getModel() {
        return model;
    }

    public void setModel(StockModel model) {
        this.model = model;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getSalePrice() {
        return unitPrice;
    }

    public void setSalePrice(Double salePrice) {
        this.unitPrice = salePrice;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockEntry() {
        return stockEntry;
    }

    public void setStockEntry(Integer stockEntry) {
        this.stockEntry = stockEntry;
    }

    public Integer getStockExit() {
        return stockExit;
    }

    public void setStockExit(Integer stockExit) {
        this.stockExit = stockExit;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
