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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Autowired
    private B2BUnitInvoiceService b2BUnitInvoiceService;
    
    public List<RevisionOffer> getAllRevisionOffers() {
        List<RevisionOffer> offers = revisionOfferRepository.findAllByOrderByCreatedAtDesc();
        offers.forEach(this::initializeOfferDetails);
        return offers;
    }

    public List<RevisionOffer> getAllRevisionOffers(String status) {
        List<RevisionOffer> offers = getAllRevisionOffers();
        if (status == null || status.isBlank()) {
            return offers;
        }
        RevisionOffer.Status requestedStatus = parseRequestedStatus(status);
        return offers.stream()
                .filter(offer -> resolveEffectiveStatus(offer) == requestedStatus)
                .toList();
    }
    
    public RevisionOffer getRevisionOfferById(Long id) {
        RevisionOffer offer = revisionOfferRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));
        initializeOfferDetails(offer);
        return offer;
    }
    
    public List<RevisionOffer> getByElevatorId(Long elevatorId) {
        List<RevisionOffer> offers = revisionOfferRepository.findAllByElevatorIdOrderByCreatedAtDesc(elevatorId);
        offers.forEach(this::initializeOfferDetails);
        return offers;
    }
    
    public List<RevisionOffer> getByBuildingId(Long buildingId) {
        List<RevisionOffer> offers = revisionOfferRepository.findAllByBuildingIdOrderByCreatedAtDesc(buildingId);
        offers.forEach(this::initializeOfferDetails);
        return offers;
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

        RevisionOffer.Status currentStatus = resolveEffectiveStatus(existing);
        RevisionOffer.Status targetStatus = offer.getStatus() != null ? offer.getStatus() : currentStatus;
        validateUpdatableStatus(id, currentStatus, targetStatus);

        existing.setLaborDescription(offer.getLaborDescription());

        if (offer.getRevisionStandardId() != null) {
            validateRevisionStandard(offer.getRevisionStandardId());
            existing.setRevisionStandardId(offer.getRevisionStandardId());
        }

        if (offer.getItems() != null) {
            replaceItems(existing, offer.getItems());
        }
        if (offer.getLaborTotal() != null) {
            existing.setLaborTotal(defaultMoney(offer.getLaborTotal()));
        }
        recalculateTotals(existing);

        if (currentStatus == RevisionOffer.Status.SENT && targetStatus == RevisionOffer.Status.APPROVED) {
            return convertRevisionOfferToSale(existing);
        }

        existing.setStatus(targetStatus);

        return revisionOfferRepository.save(existing);
    }
    
    /**
     * Convert revision offer to sale
     * Updates current account debt/credit
     */
    public RevisionOffer convertToSale(Long id) {
        RevisionOffer offer = revisionOfferRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("Revision offer not found"));

        RevisionOffer.Status currentStatus = resolveEffectiveStatus(offer);
        if (currentStatus != RevisionOffer.Status.SENT && currentStatus != RevisionOffer.Status.APPROVED) {
            throw new RuntimeException("Only SENT revision offers can be approved and converted to sale.");
        }

        return convertRevisionOfferToSale(offer);
    }
    
    public void deleteRevisionOffer(Long id) {
        if (!revisionOfferRepository.existsById(id)) {
            throw new RuntimeException("Revision offer not found");
        }
        revisionOfferRepository.deleteById(id);
    }

    public byte[] generateRevisionOfferPdf(Long id) {
        RevisionOffer offer = getRevisionOfferById(id);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50f;
                float y = page.getMediaBox().getHeight() - margin;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(margin, y);
                content.showText(pdfSafe("Revizyon Teklifi #" + offer.getId()));
                content.endText();

                y -= 30;
                y = writeLine(content, margin, y, "Asansor: " + valueOf(offer.getElevator() != null ? offer.getElevator().getIdentityNumber() : null), true);
                y = writeLine(content, margin, y, "Bina: " + valueOf(resolveBuildingName(offer)), false);
                y = writeLine(content, margin, y, "Cari: " + valueOf(offer.getCurrentAccount() != null ? offer.getCurrentAccount().getName() : null), false);
                y = writeLine(content, margin, y, "Revizyon Standardi: " + valueOf(getRevisionStandardCode(offer.getRevisionStandardId())), false);
                y = writeLine(content, margin, y, "Durum: " + valueOf(offer.getStatus() != null ? offer.getStatus().name() : null), false);
                y = writeLine(content, margin, y, "Parca Toplami: " + formatMoney(offer.getPartsTotal()), false);
                y = writeLine(content, margin, y, "Iscilik: " + formatMoney(offer.getLaborTotal()), false);
                y = writeLine(content, margin, y, "Toplam: " + formatMoney(offer.getTotalPrice()), false);

                if (offer.getLaborDescription() != null && !offer.getLaborDescription().isBlank()) {
                    y -= 8;
                    y = writeParagraph(content, margin, y, "Iscilik Aciklamasi: " + offer.getLaborDescription(), 12);
                }

                y -= 12;
                y = writeLine(content, margin, y, "Kalemler", true);

                int index = 1;
                for (RevisionOfferItem item : offer.getItems()) {
                    String line = index + ". " + valueOf(item.getPart() != null ? item.getPart().getName() : null)
                            + " | Adet: " + valueOf(item.getQuantity())
                            + " | Birim: " + formatMoney(item.getUnitPrice() != null ? BigDecimal.valueOf(item.getUnitPrice()) : null);
                    y = writeLine(content, margin, y, line, false);
                    if (item.getDescription() != null && !item.getDescription().isBlank()) {
                        y = writeParagraph(content, margin + 16, y, item.getDescription(), 11);
                    }
                    y -= 6;
                    index++;
                }
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Revision offer PDF olusturulamadi", e);
        }
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

    private void validateUpdatableStatus(Long offerId,
                                         RevisionOffer.Status currentStatus,
                                         RevisionOffer.Status targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new RuntimeException("Revision offer status is required.");
        }

        boolean allowed = switch (currentStatus) {
            case DRAFT -> targetStatus == RevisionOffer.Status.DRAFT || targetStatus == RevisionOffer.Status.SENT;
            case SENT -> targetStatus == RevisionOffer.Status.SENT
                    || targetStatus == RevisionOffer.Status.APPROVED
                    || targetStatus == RevisionOffer.Status.REJECTED;
            case APPROVED, REJECTED, CONVERTED_TO_SALE -> false;
        };

        if (!allowed) {
            throw new RuntimeException(buildTransitionErrorMessage(offerId, currentStatus, targetStatus));
        }
    }

    private String buildTransitionErrorMessage(Long offerId,
                                               RevisionOffer.Status currentStatus,
                                               RevisionOffer.Status targetStatus) {
        if (currentStatus == targetStatus && currentStatus == RevisionOffer.Status.APPROVED) {
            return "Revision offer #" + offerId + " is already approved and converted to sale.";
        }
        if (currentStatus == targetStatus && currentStatus == RevisionOffer.Status.REJECTED) {
            return "Revision offer #" + offerId + " is already rejected and is read-only.";
        }
        if (currentStatus == targetStatus && currentStatus == RevisionOffer.Status.CONVERTED_TO_SALE) {
            return "Revision offer #" + offerId + " is already converted and is read-only.";
        }
        if (currentStatus == RevisionOffer.Status.DRAFT && targetStatus == RevisionOffer.Status.APPROVED) {
            return "Revision offer #" + offerId + " must be sent before it can be approved and converted to sale.";
        }
        if (currentStatus == RevisionOffer.Status.DRAFT && targetStatus == RevisionOffer.Status.REJECTED) {
            return "Revision offer #" + offerId + " must be sent before it can be rejected.";
        }
        if (currentStatus == RevisionOffer.Status.REJECTED) {
            return "Revision offer #" + offerId + " is rejected and cannot be updated.";
        }
        if (currentStatus == RevisionOffer.Status.APPROVED) {
            return "Revision offer #" + offerId + " is already approved and converted to sale.";
        }
        if (currentStatus == RevisionOffer.Status.CONVERTED_TO_SALE) {
            return "Revision offer #" + offerId + " is converted and is read-only.";
        }
        return "Invalid status transition for revision offer #" + offerId + ": "
                + toApiStatus(currentStatus) + " -> " + toApiStatus(targetStatus) + ".";
    }

    private RevisionOffer convertRevisionOfferToSale(RevisionOffer offer) {
        if (offer.getConvertedToSale() != null) {
            throw new RuntimeException("This revision offer has already been converted to sale.");
        }

        offer.setConvertedToSale(b2BUnitInvoiceService.createSalesInvoiceFromRevisionOffer(offer));
        offer.setStatus(RevisionOffer.Status.CONVERTED_TO_SALE);
        currentAccountService.updateBalance(
                offer.getCurrentAccount().getId(),
                offer.getTotalPrice(),
                BigDecimal.ZERO
        );
        return revisionOfferRepository.save(offer);
    }

    private RevisionOffer.Status resolveEffectiveStatus(RevisionOffer offer) {
        if (offer == null) {
            return null;
        }
        if (offer.getConvertedToSale() != null) {
            return RevisionOffer.Status.CONVERTED_TO_SALE;
        }
        return offer.getStatus();
    }

    private RevisionOffer.Status parseRequestedStatus(String rawStatus) {
        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "DRAFT" -> RevisionOffer.Status.DRAFT;
            case "SENT" -> RevisionOffer.Status.SENT;
            case "ACCEPTED", "APPROVED" -> RevisionOffer.Status.APPROVED;
            case "REJECTED" -> RevisionOffer.Status.REJECTED;
            case "CONVERTED", "CONVERTED_TO_SALE" -> RevisionOffer.Status.CONVERTED_TO_SALE;
            default -> throw new RuntimeException("Invalid revision offer status filter: " + rawStatus);
        };
    }

    private String toApiStatus(RevisionOffer.Status status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case APPROVED -> "ACCEPTED";
            case CONVERTED_TO_SALE -> "CONVERTED";
            default -> status.name();
        };
    }

    private void initializeOfferDetails(RevisionOffer offer) {
        if (offer.getElevator() != null) {
            offer.getElevator().getId();
            offer.getElevator().getIdentityNumber();
            offer.getElevator().getBuildingName();
            if (offer.getElevator().getFacility() != null) {
                offer.getElevator().getFacility().getId();
                offer.getElevator().getFacility().getName();
            }
        }
        if (offer.getBuilding() != null) {
            offer.getBuilding().getId();
            offer.getBuilding().getName();
        }
        if (offer.getCurrentAccount() != null) {
            offer.getCurrentAccount().getId();
            offer.getCurrentAccount().getName();
            if (offer.getCurrentAccount().getBuilding() != null) {
                offer.getCurrentAccount().getBuilding().getId();
                offer.getCurrentAccount().getBuilding().getName();
            }
        }
        if (offer.getConvertedToSale() != null) {
            offer.getConvertedToSale().getId();
            offer.getConvertedToSale().getInvoiceType();
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

    private float writeLine(PDPageContentStream content, float x, float y, String text, boolean bold) throws IOException {
        content.beginText();
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 12);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(text));
        content.endText();
        return y - 18;
    }

    private float writeParagraph(PDPageContentStream content, float x, float y, String text, int fontSize) throws IOException {
        for (String line : wrapText(text, 90)) {
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(pdfSafe(line));
            content.endText();
            y -= 15;
        }
        return y;
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }
            if (current.length() + 1 + word.length() <= maxLength) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String pdfSafe(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("ı", "i")
                .replace("İ", "I")
                .replace("ğ", "g")
                .replace("Ğ", "G")
                .replace("ü", "u")
                .replace("Ü", "U")
                .replace("ş", "s")
                .replace("Ş", "S")
                .replace("ö", "o")
                .replace("Ö", "O")
                .replace("ç", "c")
                .replace("Ç", "C")
                .replace("\n", " ");
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TL";
    }

    private String valueOf(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String resolveBuildingName(RevisionOffer offer) {
        if (offer.getBuilding() != null && offer.getBuilding().getName() != null) {
            return offer.getBuilding().getName();
        }
        if (offer.getElevator() != null && offer.getElevator().getBuildingName() != null) {
            return offer.getElevator().getBuildingName();
        }
        if (offer.getCurrentAccount() != null && offer.getCurrentAccount().getBuilding() != null) {
            return offer.getCurrentAccount().getBuilding().getName();
        }
        return null;
    }
}
