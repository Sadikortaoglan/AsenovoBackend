package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faults")
public class Fault {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elevator_id", nullable = false)
    private Elevator elevator;

    @Column(name = "fault_subject", nullable = false)
    private String faultSubject;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Column(name = "building_authorized_message", columnDefinition = "TEXT")
    private String buildingAuthorizedMessage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        OPEN, COMPLETED
    }

    public Fault() {
    }

    public Fault(Long id, Elevator elevator, String faultSubject, String contactPerson, String buildingAuthorizedMessage, String description, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.elevator = elevator;
        this.faultSubject = faultSubject;
        this.contactPerson = contactPerson;
        this.buildingAuthorizedMessage = buildingAuthorizedMessage;
        this.description = description;
        this.status = status;
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

    public String getFaultSubject() {
        return faultSubject;
    }

    public void setFaultSubject(String faultSubject) {
        this.faultSubject = faultSubject;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getBuildingAuthorizedMessage() {
        return buildingAuthorizedMessage;
    }

    public void setBuildingAuthorizedMessage(String buildingAuthorizedMessage) {
        this.buildingAuthorizedMessage = buildingAuthorizedMessage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
