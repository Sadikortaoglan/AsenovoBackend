package com.saraasansor.api.service;

import com.saraasansor.api.dto.OfferDto;
import com.saraasansor.api.dto.OfferItemDto;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Offer;
import com.saraasansor.api.model.OfferItem;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.OfferRepository;
import com.saraasansor.api.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OfferService {
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private PartRepository partRepository;
    
    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(OfferDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public OfferDto getOfferById(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found with id: " + id));
        
        // Force initialization of lazy collections within transaction
        if (offer.getItems() != null) {
            offer.getItems().size();
        }
        if (offer.getElevator() != null) {
            offer.getElevator().getId();
        }
        
        return OfferDto.fromEntity(offer);
    }
    
    public List<OfferDto> getOffersByElevatorId(Long elevatorId) {
        return offerRepository.findByElevatorId(elevatorId).stream()
                .map(OfferDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public OfferDto createOffer(OfferDto dto) {
        Elevator elevator = null;
        if (dto.getElevatorId() != null) {
            elevator = elevatorRepository.findById(dto.getElevatorId())
                    .orElseThrow(() -> new RuntimeException("Elevator not found"));
        }
        
        Offer offer = new Offer();
        offer.setElevator(elevator);
        offer.setDate(dto.getDate());
        offer.setVatRate(dto.getVatRate() != null ? dto.getVatRate() : 20.0);
        offer.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : 0.0);
        offer.setSubtotal(dto.getSubtotal() != null ? dto.getSubtotal() : 0.0);
        offer.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0);
        
        // Set status
        if (dto.getStatus() != null) {
            try {
                offer.setStatus(Offer.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                offer.setStatus(Offer.Status.PENDING);
            }
        } else {
            offer.setStatus(Offer.Status.PENDING);
        }
        
        // Save offer first (to get ID)
        Offer savedOffer = offerRepository.save(offer);
        
        // Process items if provided
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            double subtotal = 0.0;
            
            for (OfferItemDto itemDto : dto.getItems()) {
                Part part = partRepository.findById(itemDto.getPartId())
                        .orElseThrow(() -> new RuntimeException("Part not found: " + itemDto.getPartId()));
                
                OfferItem item = new OfferItem();
                item.setOffer(savedOffer);
                item.setPart(part);
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setLineTotal(itemDto.getQuantity() * itemDto.getUnitPrice());
                
                savedOffer.getItems().add(item);
                subtotal += item.getLineTotal();
            }
            
            // Recalculate totals
            savedOffer.setSubtotal(subtotal);
            double discount = savedOffer.getDiscountAmount() != null ? savedOffer.getDiscountAmount() : 0.0;
            double vatRate = savedOffer.getVatRate() != null ? savedOffer.getVatRate() : 20.0;
            double afterDiscount = subtotal - discount;
            double vatAmount = afterDiscount * (vatRate / 100.0);
            savedOffer.setTotalAmount(afterDiscount + vatAmount);
        }
        
        Offer finalOffer = offerRepository.save(savedOffer);
        return OfferDto.fromEntity(finalOffer);
    }
    
    public OfferDto updateOffer(Long id, OfferDto dto) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        
        if (dto.getElevatorId() != null) {
            Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                    .orElseThrow(() -> new RuntimeException("Elevator not found"));
            offer.setElevator(elevator);
        }
        
        if (dto.getDate() != null) {
            offer.setDate(dto.getDate());
        }
        
        if (dto.getVatRate() != null) {
            offer.setVatRate(dto.getVatRate());
        }
        
        if (dto.getDiscountAmount() != null) {
            offer.setDiscountAmount(dto.getDiscountAmount());
        }
        
        if (dto.getStatus() != null) {
            try {
                offer.setStatus(Offer.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + dto.getStatus());
            }
        }
        
        // Update items if provided
        if (dto.getItems() != null) {
            // Remove existing items
            offer.getItems().clear();
            
            double subtotal = 0.0;
            
            for (OfferItemDto itemDto : dto.getItems()) {
                Part part = partRepository.findById(itemDto.getPartId())
                        .orElseThrow(() -> new RuntimeException("Part not found: " + itemDto.getPartId()));
                
                OfferItem item = new OfferItem();
                item.setOffer(offer);
                item.setPart(part);
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setLineTotal(itemDto.getQuantity() * itemDto.getUnitPrice());
                
                offer.getItems().add(item);
                subtotal += item.getLineTotal();
            }
            
            // Recalculate totals
            offer.setSubtotal(subtotal);
            double discount = offer.getDiscountAmount() != null ? offer.getDiscountAmount() : 0.0;
            double vatRate = offer.getVatRate() != null ? offer.getVatRate() : 20.0;
            double afterDiscount = subtotal - discount;
            double vatAmount = afterDiscount * (vatRate / 100.0);
            offer.setTotalAmount(afterDiscount + vatAmount);
        }
        
        Offer saved = offerRepository.save(offer);
        return OfferDto.fromEntity(saved);
    }
    
    public void deleteOffer(Long id) {
        if (!offerRepository.existsById(id)) {
            throw new RuntimeException("Offer not found");
        }
        offerRepository.deleteById(id);
    }
}
