package com.saraasansor.api.dto;

import com.saraasansor.api.model.RevisionOffer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RevisionOfferDto {
    private Long id;
    private Long elevatorId;
    private String elevatorIdentityNumber;
    private Long buildingId;
    private String buildingName;
    private Long currentAccountId;
    private String currentAccountName;
    private CurrentAccountDto currentAccount;
    private Long revisionStandardId;
    private String revisionStandardCode;
    private RevisionStandardReferenceDto revisionStandard;
    private BigDecimal labor;
    private String laborDescription;
    private BigDecimal partsTotal;
    private BigDecimal laborTotal;
    private BigDecimal totalPrice;
    private String status; // DRAFT, SENT, APPROVED, REJECTED, CONVERTED_TO_SALE
    private List<RevisionOfferItemDto> parts = new ArrayList<>();
    private List<RevisionOfferItemDto> items = new ArrayList<>();
    private List<RevisionOfferItemDto> offerItems = new ArrayList<>();

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

    public CurrentAccountDto getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(CurrentAccountDto currentAccount) {
        this.currentAccount = currentAccount;
    }

    public Long getRevisionStandardId() {
        return revisionStandardId;
    }

    public void setRevisionStandardId(Long revisionStandardId) {
        this.revisionStandardId = revisionStandardId;
    }

    public String getRevisionStandardCode() {
        return revisionStandardCode;
    }

    public void setRevisionStandardCode(String revisionStandardCode) {
        this.revisionStandardCode = revisionStandardCode;
    }

    public RevisionStandardReferenceDto getRevisionStandard() {
        return revisionStandard;
    }

    public void setRevisionStandard(RevisionStandardReferenceDto revisionStandard) {
        this.revisionStandard = revisionStandard;
    }

    public BigDecimal getLabor() {
        return labor;
    }

    public void setLabor(BigDecimal labor) {
        this.labor = labor;
    }

    public String getLaborDescription() {
        return laborDescription;
    }

    public void setLaborDescription(String laborDescription) {
        this.laborDescription = laborDescription;
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

    public List<RevisionOfferItemDto> getParts() {
        return parts;
    }

    public void setParts(List<RevisionOfferItemDto> parts) {
        this.parts = parts == null ? new ArrayList<>() : parts;
    }

    public List<RevisionOfferItemDto> getItems() {
        return items;
    }

    public void setItems(List<RevisionOfferItemDto> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<RevisionOfferItemDto> getOfferItems() {
        return offerItems;
    }

    public void setOfferItems(List<RevisionOfferItemDto> offerItems) {
        this.offerItems = offerItems == null ? new ArrayList<>() : offerItems;
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
            dto.setCurrentAccount(CurrentAccountDto.fromEntity(offer.getCurrentAccount()));
        }
        dto.setRevisionStandardId(offer.getRevisionStandardId());
        dto.setLabor(offer.getLaborTotal());
        dto.setLaborDescription(offer.getLaborDescription());
        dto.setPartsTotal(offer.getPartsTotal());
        dto.setLaborTotal(offer.getLaborTotal());
        dto.setTotalPrice(offer.getTotalPrice());
        dto.setStatus(offer.getStatus() != null ? offer.getStatus().name() : null);
        if (offer.getItems() != null && !offer.getItems().isEmpty()) {
            List<RevisionOfferItemDto> itemDtos = offer.getItems().stream()
                    .map(RevisionOfferItemDto::fromEntity)
                    .toList();
            dto.setItems(itemDtos);
            dto.setParts(itemDtos);
            dto.setOfferItems(itemDtos);
        }
        return dto;
    }
}
