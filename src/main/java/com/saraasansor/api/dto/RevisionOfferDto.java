package com.saraasansor.api.dto;

import com.saraasansor.api.model.RevisionOffer;

import java.math.BigDecimal;

public class RevisionOfferDto {
    private Long id;
    private Long elevatorId;
    private String elevatorIdentityNumber;
    private Long buildingId;
    private String buildingName;
    private Long currentAccountId;
    private String currentAccountName;
    private BigDecimal partsTotal;
    private BigDecimal laborTotal;
    private BigDecimal totalPrice;
    private String status; // DRAFT, SENT, APPROVED, REJECTED, CONVERTED_TO_SALE

    public RevisionOfferDto() {
    }

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

    public String getElevatorIdentityNumber() {
        return elevatorIdentityNumber;
    }

    public void setElevatorIdentityNumber(String elevatorIdentityNumber) {
        this.elevatorIdentityNumber = elevatorIdentityNumber;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Long getCurrentAccountId() {
        return currentAccountId;
    }

    public void setCurrentAccountId(Long currentAccountId) {
        this.currentAccountId = currentAccountId;
    }

    public String getCurrentAccountName() {
        return currentAccountName;
    }

    public void setCurrentAccountName(String currentAccountName) {
        this.currentAccountName = currentAccountName;
    }

    public BigDecimal getPartsTotal() {
        return partsTotal;
    }

    public void setPartsTotal(BigDecimal partsTotal) {
        this.partsTotal = partsTotal;
    }

    public BigDecimal getLaborTotal() {
        return laborTotal;
    }

    public void setLaborTotal(BigDecimal laborTotal) {
        this.laborTotal = laborTotal;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static RevisionOfferDto fromEntity(RevisionOffer offer) {
        RevisionOfferDto dto = new RevisionOfferDto();
        dto.setId(offer.getId());
        if (offer.getElevator() != null) {
            dto.setElevatorId(offer.getElevator().getId());
            dto.setElevatorIdentityNumber(offer.getElevator().getIdentityNumber());
        }
        if (offer.getBuilding() != null) {
            dto.setBuildingId(offer.getBuilding().getId());
            dto.setBuildingName(offer.getBuilding().getName());
        }
        if (offer.getCurrentAccount() != null) {
            dto.setCurrentAccountId(offer.getCurrentAccount().getId());
            dto.setCurrentAccountName(offer.getCurrentAccount().getName());
        }
        dto.setPartsTotal(offer.getPartsTotal());
        dto.setLaborTotal(offer.getLaborTotal());
        dto.setTotalPrice(offer.getTotalPrice());
        dto.setStatus(offer.getStatus() != null ? offer.getStatus().name() : null);
        return dto;
    }
}
