package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspections")
public class Inspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elevator_id", nullable = false)
    private Elevator elevator;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_color", nullable = false)
    private InspectionColor inspectionColor;

    @Column(name = "contacted_person_name")
    private String contactedPersonName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Inspection() {
    }

    public Inspection(Long id, Elevator elevator, LocalDate date, String result, InspectionColor inspectionColor, String contactedPersonName, String description, LocalDateTime createdAt) {
        this.id = id;
        this.elevator = elevator;
        this.date = date;
        this.result = result;
        this.inspectionColor = inspectionColor;
        this.contactedPersonName = contactedPersonName;
        this.description = description;
        this.createdAt = createdAt;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public InspectionColor getInspectionColor() {
        return inspectionColor;
    }

    public void setInspectionColor(InspectionColor inspectionColor) {
        this.inspectionColor = inspectionColor;
    }

    public String getContactedPersonName() {
        return contactedPersonName;
    }

    public void setContactedPersonName(String contactedPersonName) {
        this.contactedPersonName = contactedPersonName;
    }
}
