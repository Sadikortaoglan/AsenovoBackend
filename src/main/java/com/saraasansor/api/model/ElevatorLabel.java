package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "elevator_labels",
        indexes = {
                @Index(name = "idx_elevator_labels_elevator_id", columnList = "elevator_id"),
                @Index(name = "idx_elevator_labels_status", columnList = "status"),
                @Index(name = "idx_elevator_labels_qr_code_id", columnList = "qr_code_id")
        })
public class ElevatorLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "elevator_id", nullable = false)
    private Elevator elevator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id")
    private ElevatorQrCode qrCode;

    @Column(name = "label_name")
    private String labelName;

    @Column(name = "label_start_date")
    private LocalDate labelStartDate;

    @Column(name = "label_end_date")
    private LocalDate labelEndDate;

    @Column(name = "label_issue_date")
    private LocalDate labelIssueDate;

    @Column(name = "label_date")
    private LocalDate labelDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "label_type")
    private String labelType;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "attachment_name")
    private String attachmentName;

    @Column(name = "attachment_content_type")
    private String attachmentContentType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "attachment_storage_key")
    private String attachmentStorageKey;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public ElevatorQrCode getQrCode() {
        return qrCode;
    }

    public void setQrCode(ElevatorQrCode qrCode) {
        this.qrCode = qrCode;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public LocalDate getLabelStartDate() {
        return labelStartDate;
    }

    public void setLabelStartDate(LocalDate labelStartDate) {
        this.labelStartDate = labelStartDate;
    }

    public LocalDate getLabelEndDate() {
        return labelEndDate;
    }

    public void setLabelEndDate(LocalDate labelEndDate) {
        this.labelEndDate = labelEndDate;
    }

    public LocalDate getLabelIssueDate() {
        return labelIssueDate;
    }

    public void setLabelIssueDate(LocalDate labelIssueDate) {
        this.labelIssueDate = labelIssueDate;
    }

    public LocalDate getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(LocalDate labelDate) {
        this.labelDate = labelDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getLabelType() {
        return labelType;
    }

    public void setLabelType(String labelType) {
        this.labelType = labelType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public String getAttachmentStorageKey() {
        return attachmentStorageKey;
    }

    public void setAttachmentStorageKey(String attachmentStorageKey) {
        this.attachmentStorageKey = attachmentStorageKey;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
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
