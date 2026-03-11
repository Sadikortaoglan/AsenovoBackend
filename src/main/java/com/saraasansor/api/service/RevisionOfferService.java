package com.saraasansor.api.service;

import com.saraasansor.api.dto.RevisionOfferItemDto;
import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.model.RevisionOffer;
import com.saraasansor.api.model.RevisionOfferItem;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardAdminRepository;
import com.saraasansor.api.repository.BuildingRepository;
import com.saraasansor.api.repository.CurrentAccountRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.PartRepository;
import com.saraasansor.api.repository.RevisionOfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    @Autowired
    private RevisionStandardAdminRepository revisionStandardAdminRepository;

    @Autowired
    private PartRepository partRepository;
    
    public List<RevisionOffer> getAllRevisionOffers() {
        return revisionOfferRepository.findAll();
    }
    
    public RevisionOffer getRevisionOfferById(Long id) {
        RevisionOffer offer = revisionOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));
        initializeOfferDetails(offer);
        return offer;
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

        validateRevisionStandard(offer.getRevisionStandardId());

        replaceItems(offer, offer.getItems());
        recalculateTotals(offer);

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
        
        existing.setLaborDescription(offer.getLaborDescription());

        if (offer.getStatus() != null) {
            existing.setStatus(offer.getStatus());
        }

        if (offer.getRevisionStandardId() != null) {
            validateRevisionStandard(offer.getRevisionStandardId());
            existing.setRevisionStandardId(offer.getRevisionStandardId());
        }

        replaceItems(existing, offer.getItems());
        existing.setLaborTotal(defaultMoney(offer.getLaborTotal()));
        recalculateTotals(existing);

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

    public String getRevisionStandardCode(Long revisionStandardId) {
        if (revisionStandardId == null) {
            return null;
        }
        return revisionStandardAdminRepository.findStandardCodeById(revisionStandardId).orElse(null);
    }

    private void validateRevisionStandard(Long revisionStandardId) {
        if (revisionStandardId == null) {
            return;
        }
        revisionStandardAdminRepository.findStandardSetById(revisionStandardId)
                .orElseThrow(() -> new RuntimeException("Revision standard not found"));
    }

    public void replaceItems(RevisionOffer target, List<RevisionOfferItem> newItems) {
        target.getItems().clear();
        if (newItems == null) {
            return;
        }

        for (RevisionOfferItem item : newItems) {
            item.setRevisionOffer(target);
            target.getItems().add(item);
        }
    }

    public List<RevisionOfferItem> buildItems(List<RevisionOfferItemDto> itemDtos) {
        List<RevisionOfferItem> items = new ArrayList<>();
        if (itemDtos == null) {
            return items;
        }

        for (RevisionOfferItemDto dto : itemDtos) {
            items.add(buildItem(dto));
        }

        return items;
    }

    private RevisionOfferItem buildItem(RevisionOfferItemDto dto) {
        if (dto.getPartId() == null) {
            throw new RuntimeException("Part id is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        Part part = partRepository.findById(dto.getPartId())
                .orElseThrow(() -> new RuntimeException("Part not found: " + dto.getPartId()));

        RevisionOfferItem item = new RevisionOfferItem();
        item.setId(dto.getId());
        item.setPart(part);
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : part.getUnitPrice());
        item.setDescription(dto.getDescription());
        return item;
    }

    private void recalculateTotals(RevisionOffer offer) {
        BigDecimal partsTotal = BigDecimal.ZERO;
        for (RevisionOfferItem item : offer.getItems()) {
            BigDecimal unitPrice = BigDecimal.valueOf(item.getUnitPrice() != null ? item.getUnitPrice() : 0D);
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
            partsTotal = partsTotal.add(unitPrice.multiply(quantity));
        }

        offer.setPartsTotal(partsTotal.setScale(2, RoundingMode.HALF_UP));
        offer.setLaborTotal(defaultMoney(offer.getLaborTotal()));
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void initializeOfferDetails(RevisionOffer offer) {
        if (offer.getElevator() != null) {
            offer.getElevator().getId();
            offer.getElevator().getIdentityNumber();
        }
        if (offer.getBuilding() != null) {
            offer.getBuilding().getId();
            offer.getBuilding().getName();
        }
        if (offer.getCurrentAccount() != null) {
            offer.getCurrentAccount().getId();
            offer.getCurrentAccount().getName();
        }
        for (RevisionOfferItem item : offer.getItems()) {
            item.getId();
            item.getQuantity();
            item.getUnitPrice();
            if (item.getPart() != null) {
                item.getPart().getId();
                item.getPart().getName();
                item.getPart().getUnitPrice();
            }
        }
    }
}
