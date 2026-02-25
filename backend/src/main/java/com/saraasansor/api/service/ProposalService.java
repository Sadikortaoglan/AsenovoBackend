package com.saraasansor.api.service;

import com.saraasansor.api.dto.OfferDto;
import com.saraasansor.api.dto.ProposalLineItemRequest;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.Offer;
import com.saraasansor.api.model.OfferItem;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.repository.OfferItemRepository;
import com.saraasansor.api.repository.OfferRepository;
import com.saraasansor.api.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProposalService {
    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferService offerService;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private OfferItemRepository offerItemRepository;

    public Page<OfferDto> list(Pageable pageable) {
        return offerRepository.findAll(pageable).map(OfferDto::fromEntity);
    }

    public OfferDto create(OfferDto dto) {
        return offerService.createOffer(dto);
    }

    public OfferDto addLineItem(Long proposalId, ProposalLineItemRequest request) {
        Offer offer = offerRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Proposal not found"));

        Part part = partRepository.findById(request.getPartId())
                .orElseThrow(() -> new NotFoundException("Part not found"));

        OfferItem item = new OfferItem();
        item.setOffer(offer);
        item.setPart(part);
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setLineTotal(request.getQuantity() * request.getUnitPrice());

        offer.getItems().add(item);
        recalculate(offer);

        return OfferDto.fromEntity(offerRepository.save(offer));
    }

    public OfferDto removeLineItem(Long proposalId, Long lineItemId) {
        Offer offer = offerRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Proposal not found"));

        OfferItem item = offerItemRepository.findById(lineItemId)
                .orElseThrow(() -> new NotFoundException("Line item not found"));

        if (!item.getOffer().getId().equals(proposalId)) {
            throw new RuntimeException("Line item does not belong to proposal");
        }

        offer.getItems().removeIf(i -> i.getId().equals(lineItemId));
        recalculate(offer);
        offerRepository.save(offer);
        offerItemRepository.deleteById(lineItemId);
        return OfferDto.fromEntity(offer);
    }

    private void recalculate(Offer offer) {
        double subtotal = offer.getItems().stream().mapToDouble(OfferItem::getLineTotal).sum();
        offer.setSubtotal(subtotal);

        double discount = offer.getDiscountAmount() == null ? 0 : offer.getDiscountAmount();
        double vatRate = offer.getVatRate() == null ? 0 : offer.getVatRate();
        double afterDiscount = subtotal - discount;
        double total = afterDiscount + (afterDiscount * vatRate / 100.0);
        offer.setTotalAmount(total);
    }
}
