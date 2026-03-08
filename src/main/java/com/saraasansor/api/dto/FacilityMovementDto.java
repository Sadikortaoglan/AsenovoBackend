package com.saraasansor.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FacilityMovementDto {

    private List<ElevatorItem> elevators = new ArrayList<>();
    private List<MaintenanceItem> maintenances = new ArrayList<>();
    private List<PaymentItem> payments = new ArrayList<>();
    private List<FaultItem> faults = new ArrayList<>();

    public List<ElevatorItem> getElevators() {
        return elevators;
    }

    public void setElevators(List<ElevatorItem> elevators) {
        this.elevators = elevators;
    }

    public List<MaintenanceItem> getMaintenances() {
        return maintenances;
    }

    public void setMaintenances(List<MaintenanceItem> maintenances) {
        this.maintenances = maintenances;
    }

    public List<PaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentItem> payments) {
        this.payments = payments;
    }

    public List<FaultItem> getFaults() {
        return faults;
    }

    public void setFaults(List<FaultItem> faults) {
        this.faults = faults;
    }

    public static class ElevatorItem {
        private Long id;
        private String identityNumber;
        private String elevatorNumber;
        private String status;
        private LocalDate expiryDate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIdentityNumber() {
            return identityNumber;
        }

        public void setIdentityNumber(String identityNumber) {
            this.identityNumber = identityNumber;
        }

        public String getElevatorNumber() {
            return elevatorNumber;
        }

        public void setElevatorNumber(String elevatorNumber) {
            this.elevatorNumber = elevatorNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
        }
    }

    public static class MaintenanceItem {
        private Long id;
        private Long elevatorId;
        private LocalDate date;
        private String labelType;
        private Double amount;
        private Boolean paid;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getElevatorId() {
            return elevatorId;
        }

        public void setElevatorId(Long elevatorId) {
            this.elevatorId = elevatorId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getLabelType() {
            return labelType;
        }

        public void setLabelType(String labelType) {
            this.labelType = labelType;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public Boolean getPaid() {
            return paid;
        }

        public void setPaid(Boolean paid) {
            this.paid = paid;
        }
    }

    public static class PaymentItem {
        private Long id;
        private Long maintenanceId;
        private Double amount;
        private String payerName;
        private LocalDate date;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getMaintenanceId() {
            return maintenanceId;
        }

        public void setMaintenanceId(Long maintenanceId) {
            this.maintenanceId = maintenanceId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getPayerName() {
            return payerName;
        }

        public void setPayerName(String payerName) {
            this.payerName = payerName;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

    public static class FaultItem {
        private Long id;
        private Long elevatorId;
        private String subject;
        private String status;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getElevatorId() {
            return elevatorId;
        }

        public void setElevatorId(Long elevatorId) {
            this.elevatorId = elevatorId;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
