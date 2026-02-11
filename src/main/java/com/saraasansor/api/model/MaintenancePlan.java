package com.saraasansor.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_plans")
public class MaintenancePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elevator_id", nullable = false)
    private Elevator elevator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MaintenanceTemplate template;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.NOT_PLANNED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_proof_id")
    private com.saraasansor.api.model.QrProof qrProof;
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "price", precision = 14, scale = 2)
    private java.math.BigDecimal price;

    public enum PlanStatus {
        NOT_PLANNED, PLANNED, IN_PROGRESS, COMPLETED
    }

    public MaintenancePlan() {
    }

    // Getters and Setters
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

    public MaintenanceTemplate getTemplate() {
        return template;
    }

    public void setTemplate(MaintenanceTemplate template) {
        this.template = template;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public User getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(User assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getCancelledBy() {
        return cancelledBy;
    }
    
    public void setCancelledBy(User cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public com.saraasansor.api.model.QrProof getQrProof() {
        return qrProof;
    }
    
    public void setQrProof(com.saraasansor.api.model.QrProof qrProof) {
        this.qrProof = qrProof;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public java.math.BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }
}
