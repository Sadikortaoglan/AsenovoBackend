package com.saraasansor.api.dto;

import com.saraasansor.api.model.Offer;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OfferDto {
    private Long id;
    
    // Elevator ID can be null (elevator is optional in entity)
    private Long elevatorId;
    
    private String elevatorBuildingName;
    private String elevatorIdentityNumber;
    
    @NotNull(message = "Date cannot be empty")
    private LocalDate date;
    
    private Double vatRate = 20.0;
    private Double discountAmount = 0.0;
    private Double subtotal = 0.0;
    private Double totalAmount = 0.0;
    private String status = "PENDING";
    
    private List<OfferItemDto> items = new ArrayList<>();
    
    private LocalDateTime createdAt;

    public OfferDto() {
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

    public String getElevatorBuildingName() {
        return elevatorBuildingName;
    }

    public void setElevatorBuildingName(String elevatorBuildingName) {
        this.elevatorBuildingName = elevatorBuildingName;
    }

    public String getElevatorIdentityNumber() {
        return elevatorIdentityNumber;
    }

    public void setElevatorIdentityNumber(String elevatorIdentityNumber) {
        this.elevatorIdentityNumber = elevatorIdentityNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getVatRate() {
        return vatRate;
    }

    public void setVatRate(Double vatRate) {
        this.vatRate = vatRate;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OfferItemDto> getItems() {
        return items;
    }

    public void setItems(List<OfferItemDto> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static OfferDto fromEntity(Offer offer) {
        OfferDto dto = new OfferDto();
        dto.setId(offer.getId());
        if (offer.getElevator() != null) {
            dto.setElevatorId(offer.getElevator().getId());
            dto.setElevatorBuildingName(offer.getElevator().getBuildingName());
            dto.setElevatorIdentityNumber(offer.getElevator().getIdentityNumber());
        }
        dto.setDate(offer.getDate());
        dto.setVatRate(offer.getVatRate());
        dto.setDiscountAmount(offer.getDiscountAmount());
        dto.setSubtotal(offer.getSubtotal());
        dto.setTotalAmount(offer.getTotalAmount());
        dto.setStatus(offer.getStatus() != null ? offer.getStatus().name() : "PENDING");
        dto.setCreatedAt(offer.getCreatedAt());
        
        // Map items
        if (offer.getItems() != null) {
            dto.setItems(offer.getItems().stream()
                    .map(OfferItemDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
}
