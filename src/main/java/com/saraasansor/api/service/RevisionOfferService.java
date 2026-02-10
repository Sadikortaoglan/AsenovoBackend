package com.saraasansor.api.service;

import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.RevisionOffer;
import com.saraasansor.api.repository.BuildingRepository;
import com.saraasansor.api.repository.CurrentAccountRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.RevisionOfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class RevisionOfferService {
    
    @Autowired
    private RevisionOfferRepository revisionOfferRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private BuildingRepository buildingRepository;
    
    @Autowired
    private CurrentAccountRepository currentAccountRepository;
    
    @Autowired
    private CurrentAccountService currentAccountService;
    
    public List<RevisionOffer> getAllRevisionOffers() {
        return revisionOfferRepository.findAll();
    }
    
    public RevisionOffer getRevisionOfferById(Long id) {
        return revisionOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));
    }
    
    public List<RevisionOffer> getByElevatorId(Long elevatorId) {
        return revisionOfferRepository.findByElevatorId(elevatorId);
    }
    
    public List<RevisionOffer> getByBuildingId(Long buildingId) {
        return revisionOfferRepository.findByBuildingId(buildingId);
    }
    
    public RevisionOffer createRevisionOffer(RevisionOffer offer) {
        // Validate elevator
        Elevator elevator = elevatorRepository.findById(offer.getElevator().getId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        offer.setElevator(elevator);
        
        // Validate building (optional)
        if (offer.getBuilding() != null && offer.getBuilding().getId() != null) {
            Building building = buildingRepository.findById(offer.getBuilding().getId())
                    .orElseThrow(() -> new RuntimeException("Building not found"));
            offer.setBuilding(building);
        }
        
        // Validate or auto-find current account
        if (offer.getCurrentAccount() == null || offer.getCurrentAccount().getId() == null) {
            // Try to find account by building
            if (offer.getBuilding() != null && offer.getBuilding().getId() != null) {
                CurrentAccount account = currentAccountService.getByBuildingId(offer.getBuilding().getId())
                        .orElseThrow(() -> new RuntimeException("Current account not found for building. Please create building first."));
                offer.setCurrentAccount(account);
            } else {
                throw new RuntimeException("Current account is required");
            }
        } else {
            CurrentAccount account = currentAccountRepository.findById(offer.getCurrentAccount().getId())
                    .orElseThrow(() -> new RuntimeException("Current account not found"));
            offer.setCurrentAccount(account);
        }
        
        // totalPrice is auto-calculated in @PrePersist
        return revisionOfferRepository.save(offer);
    }
    
    public RevisionOffer updateRevisionOffer(Long id, RevisionOffer offer) {
        RevisionOffer existing = revisionOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));
        
        // Only allow updates if status is DRAFT or SENT
        if (existing.getStatus() != RevisionOffer.Status.DRAFT && 
            existing.getStatus() != RevisionOffer.Status.SENT) {
            throw new RuntimeException("Cannot update offer with status: " + existing.getStatus());
        }
        
        existing.setPartsTotal(offer.getPartsTotal());
        existing.setLaborTotal(offer.getLaborTotal());
        // totalPrice is auto-calculated in @PreUpdate
        
        if (offer.getStatus() != null) {
            existing.setStatus(offer.getStatus());
        }
        
        return revisionOfferRepository.save(existing);
    }
    
    /**
     * Convert revision offer to sale
     * Updates current account debt/credit
     */
    public RevisionOffer convertToSale(Long id) {
        RevisionOffer offer = revisionOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));
        
        // Only APPROVED offers can be converted
        if (offer.getStatus() != RevisionOffer.Status.APPROVED) {
            throw new RuntimeException("Only APPROVED offers can be converted to sale. Current status: " + offer.getStatus());
        }
        
        // Update status
        offer.setStatus(RevisionOffer.Status.CONVERTED_TO_SALE);
        
        // Update current account: add to debt
        currentAccountService.updateBalance(
            offer.getCurrentAccount().getId(),
            offer.getTotalPrice(), // Add to debt
            BigDecimal.ZERO // No credit change
        );
        
        return revisionOfferRepository.save(offer);
    }
    
    public void deleteRevisionOffer(Long id) {
        if (!revisionOfferRepository.existsById(id)) {
            throw new RuntimeException("Revision offer not found");
        }
        revisionOfferRepository.deleteById(id);
    }
}
