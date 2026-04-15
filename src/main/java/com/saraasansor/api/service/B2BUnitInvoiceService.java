package com.saraasansor.api.service;

import com.saraasansor.api.dto.InvoiceLineRequest;
import com.saraasansor.api.dto.InvoiceResponse;
import com.saraasansor.api.dto.PurchaseInvoiceCreateRequest;
import com.saraasansor.api.dto.SalesInvoiceCreateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitInvoice;
import com.saraasansor.api.model.B2BUnitInvoiceLine;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.model.RevisionOffer;
import com.saraasansor.api.model.RevisionOfferItem;
import com.saraasansor.api.model.User;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.B2BUnitInvoiceRepository;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.PartRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.repository.WarehouseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class B2BUnitInvoiceService {

    private final B2BUnitInvoiceRepository invoiceRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final FacilityRepository facilityRepository;
    private final ElevatorRepository elevatorRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final B2BUnitTransactionService transactionService;

    public B2BUnitInvoiceService(B2BUnitInvoiceRepository invoiceRepository,
                                 B2BUnitRepository b2bUnitRepository,
                                 FacilityRepository facilityRepository,
                                 ElevatorRepository elevatorRepository,
                                 WarehouseRepository warehouseRepository,
                                 PartRepository partRepository,
                                 UserRepository userRepository,
                                 B2BUnitTransactionService transactionService) {
        this.invoiceRepository = invoiceRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.facilityRepository = facilityRepository;
        this.elevatorRepository = elevatorRepository;
        this.warehouseRepository = warehouseRepository;
        this.partRepository = partRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    public InvoiceResponse createPurchaseInvoice(Long b2bUnitId, PurchaseInvoiceCreateRequest request) {
        enforceCreateAccess();
        B2BUnit b2bUnit = resolveB2BUnitWithAccessCheck(b2bUnitId);
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        List<InvoiceLineRequest> lines = validateAndGetLines(request.getLines());

        B2BUnitInvoice invoice = new B2BUnitInvoice();
        invoice.setInvoiceType(B2BUnitInvoice.InvoiceType.PURCHASE);
        invoice.setB2bUnit(b2bUnit);
        invoice.setWarehouse(warehouse);
        invoice.setFacility(null);
        invoice.setElevator(null);
        invoice.setInvoiceDate(requireInvoiceDate(request.getInvoiceDate()));
        invoice.setDescription(normalizeNullable(request.getDescription()));
        invoice.setStatus(B2BUnitInvoice.InvoiceStatus.POSTED);
        invoice.setCreatedBy(resolveCurrentUsername());

        applyLinesAndTotals(invoice, lines, true);

        B2BUnitInvoice saved = invoiceRepository.save(invoice);
        transactionService.onPurchaseInvoicePosted(
                b2bUnitId,
                saved.getInvoiceDate(),
                saved.getGrandTotal(),
                buildReferenceCode(saved),
                saved.getDescription()
        );
        return InvoiceResponse.fromEntity(saved);
    }

    public InvoiceResponse createSalesInvoice(Long b2bUnitId, SalesInvoiceCreateRequest request) {
        enforceCreateAccess();
        B2BUnit b2bUnit = resolveB2BUnitWithAccessCheck(b2bUnitId);
        Facility facility = resolveFacilityForB2BUnit(request.getFacilityId(), b2bUnitId);
        Elevator elevator = resolveElevator(request.getElevatorId());
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        validateElevatorFacilityConsistency(elevator, facility);
        List<InvoiceLineRequest> lines = validateAndGetLines(request.getLines());

        B2BUnitInvoice saved = createPostedSalesInvoice(
                b2bUnit,
                facility,
                elevator,
                warehouse,
                request.getInvoiceDate(),
                request.getDescription(),
                lines,
                true
        );
        return InvoiceResponse.fromEntity(saved);
    }

    public B2BUnitInvoice createSalesInvoiceFromRevisionOffer(RevisionOffer offer) {
        enforceCreateAccess();
        if (offer == null) {
            throw new RuntimeException("Revision offer is required");
        }
        Elevator elevator = offer.getElevator();
        if (elevator == null) {
            throw new RuntimeException("Revision offer elevator is missing.");
        }
        SaleContext saleContext = resolveSaleContextFromRevisionOffer(offer);

        List<InvoiceLineRequest> lines = buildRevisionOfferInvoiceLines(offer);
        String description = buildRevisionOfferInvoiceDescription(offer);
        return createPostedSalesInvoice(
                saleContext.b2bUnit(),
                saleContext.facility(),
                elevator,
                null,
                LocalDate.now(),
                description,
                lines,
                false
        );
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long b2bUnitId, Long invoiceId) {
        resolveB2BUnitWithAccessCheck(b2bUnitId);
        B2BUnitInvoice invoice = invoiceRepository.findByIdAndB2bUnitId(invoiceId, b2bUnitId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return InvoiceResponse.fromEntity(invoice);
    }

    private void applyLinesAndTotals(B2BUnitInvoice invoice,
                                     List<InvoiceLineRequest> lineRequests,
                                     boolean requireStockReference) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        int sortOrder = 0;

        for (InvoiceLineRequest lineRequest : lineRequests) {
            validateLine(lineRequest, requireStockReference);
            Part stock = resolveStock(lineRequest.getStockId(), requireStockReference);
            B2BUnitInvoiceLine line = new B2BUnitInvoiceLine();
            line.setStock(stock);
            line.setProductName(resolveLineProductName(lineRequest, stock, requireStockReference));
            line.setQuantity(scale(lineRequest.getQuantity()));
            line.setUnitPrice(scale(lineRequest.getUnitPrice()));
            line.setVatRate(scale(lineRequest.getVatRate()));

            BigDecimal lineSubTotal = scale(line.getQuantity().multiply(line.getUnitPrice()));
            BigDecimal lineVatTotal = scale(lineSubTotal.multiply(line.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            BigDecimal lineGrandTotal = scale(lineSubTotal.add(lineVatTotal));

            line.setLineSubTotal(lineSubTotal);
            line.setLineVatTotal(lineVatTotal);
            line.setLineGrandTotal(lineGrandTotal);
            line.setSortOrder(sortOrder++);

            subTotal = subTotal.add(lineSubTotal);
            vatTotal = vatTotal.add(lineVatTotal);
            grandTotal = grandTotal.add(lineGrandTotal);
            invoice.addLine(line);
        }

        invoice.setSubTotal(scale(subTotal));
        invoice.setVatTotal(scale(vatTotal));
        invoice.setGrandTotal(scale(grandTotal));
    }

    private B2BUnit resolveB2BUnitWithAccessCheck(Long b2bUnitId) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnitId);
        return b2bUnit;
    }

    private Facility resolveFacilityForB2BUnit(Long facilityId, Long b2bUnitId) {
        Facility facility = facilityRepository.findByIdAndActiveTrue(facilityId)
                .orElseThrow(() -> new RuntimeException("Facility not found"));
        Long facilityB2BUnitId = facility.getB2bUnit() != null ? facility.getB2bUnit().getId() : null;
        if (facilityB2BUnitId == null || !facilityB2BUnitId.equals(b2bUnitId)) {
            throw new RuntimeException("Facility does not belong to selected B2B unit");
        }
        return facility;
    }

    private Elevator resolveElevator(Long elevatorId) {
        return elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
    }

    private Warehouse resolveWarehouse(Long warehouseId) {
        return warehouseRepository.findByIdAndActiveTrue(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
    }

    private void validateElevatorFacilityConsistency(Elevator elevator, Facility facility) {
        if (elevator == null || facility == null) {
            return;
        }
        if (StringUtils.hasText(elevator.getBuildingName()) && StringUtils.hasText(facility.getName())
                && !elevator.getBuildingName().trim().equalsIgnoreCase(facility.getName().trim())) {
            throw new RuntimeException("Selected elevator does not belong to selected facility");
        }
    }

    private List<InvoiceLineRequest> validateAndGetLines(List<InvoiceLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("lines must not be empty");
        }
        return lines;
    }

    private void validateLine(InvoiceLineRequest line, boolean requireStockReference) {
        if (requireStockReference && line.getStockId() == null) {
            throw new RuntimeException("stockId is required");
        }
        if (!requireStockReference && line.getStockId() == null && !StringUtils.hasText(line.getProductName())) {
            throw new RuntimeException("productName is required");
        }
        if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("quantity must be greater than zero");
        }
        if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("unitPrice must be zero or positive");
        }
        if (line.getVatRate() == null || line.getVatRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("vatRate must be zero or positive");
        }
    }

    private Part resolveStock(Long stockId, boolean required) {
        if (stockId == null) {
            return null;
        }
        if (required) {
            return partRepository.findByIdAndActiveTrue(stockId)
                    .orElseThrow(() -> new RuntimeException("Stock not found"));
        }
        return partRepository.findByIdAndActiveTrue(stockId).orElse(null);
    }

    private String resolveLineProductName(InvoiceLineRequest lineRequest, Part stock, boolean requireStockReference) {
        if (stock != null) {
            return normalizeRequired(stock.getName(), "stock name is required");
        }
        if (requireStockReference) {
            throw new RuntimeException("stockId is required");
        }
        return normalizeRequired(lineRequest.getProductName(), "productName is required");
    }

    private LocalDate requireInvoiceDate(LocalDate invoiceDate) {
        if (invoiceDate == null) {
            throw new RuntimeException("invoiceDate is required");
        }
        return invoiceDate;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void enforceCreateAccess() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException("CARI users cannot create invoices");
        }
    }

    private void enforceObjectAccess(Long b2bUnitId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }
        if (currentUser.getRole() == User.Role.CARI_USER) {
            Long ownB2bUnitId = currentUser.getB2bUnit() != null ? currentUser.getB2bUnit().getId() : null;
            if (ownB2bUnitId == null || !ownB2bUnitId.equals(b2bUnitId)) {
                throw new AccessDeniedException("CARI user can only access own B2B unit");
            }
        }
    }

    private String resolveCurrentUsername() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUsername() : null;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
            username = principalName;
        }

        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private String buildReferenceCode(B2BUnitInvoice invoice) {
        if (invoice.getId() == null) {
            return null;
        }
        return (invoice.getInvoiceType() == B2BUnitInvoice.InvoiceType.PURCHASE ? "PUR-" : "SAL-") + invoice.getId();
    }

    public String buildSaleNumber(B2BUnitInvoice invoice) {
        if (invoice == null || invoice.getInvoiceType() != B2BUnitInvoice.InvoiceType.SALES) {
            return null;
        }
        return buildReferenceCode(invoice);
    }

    private B2BUnitInvoice createPostedSalesInvoice(B2BUnit b2bUnit,
                                                    Facility facility,
                                                    Elevator elevator,
                                                    Warehouse warehouse,
                                                    LocalDate invoiceDate,
                                                    String description,
                                                    List<InvoiceLineRequest> lines,
                                                    boolean requireStockReference) {
        validateElevatorFacilityConsistency(elevator, facility);

        B2BUnitInvoice invoice = new B2BUnitInvoice();
        invoice.setInvoiceType(B2BUnitInvoice.InvoiceType.SALES);
        invoice.setB2bUnit(b2bUnit);
        invoice.setFacility(facility);
        invoice.setElevator(elevator);
        invoice.setWarehouse(warehouse);
        invoice.setInvoiceDate(requireInvoiceDate(invoiceDate));
        invoice.setDescription(normalizeNullable(description));
        invoice.setStatus(B2BUnitInvoice.InvoiceStatus.POSTED);
        invoice.setCreatedBy(resolveCurrentUsername());

        applyLinesAndTotals(invoice, validateAndGetLines(lines), requireStockReference);

        B2BUnitInvoice saved = invoiceRepository.save(invoice);
        transactionService.onSalesInvoicePosted(
                b2bUnit.getId(),
                saved.getInvoiceDate(),
                saved.getGrandTotal(),
                buildReferenceCode(saved),
                saved.getDescription()
        );
        return saved;
    }

    private List<InvoiceLineRequest> buildRevisionOfferInvoiceLines(RevisionOffer offer) {
        List<InvoiceLineRequest> lines = new ArrayList<>();
        if (offer.getItems() != null) {
            for (RevisionOfferItem item : offer.getItems()) {
                lines.add(newInvoiceLine(
                        item.getPart() != null ? item.getPart().getId() : null,
                        item.getPart() != null ? item.getPart().getName() : "Revision item",
                        BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0),
                        BigDecimal.valueOf(item.getUnitPrice() != null ? item.getUnitPrice() : 0D),
                        BigDecimal.ZERO
                ));
            }
        }

        BigDecimal laborTotal = offer.getLaborTotal() != null ? offer.getLaborTotal() : BigDecimal.ZERO;
        if (laborTotal.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(newInvoiceLine(null, "Revision labor", BigDecimal.ONE, laborTotal, BigDecimal.ZERO));
        }

        return lines;
    }

    private InvoiceLineRequest newInvoiceLine(Long stockId,
                                              String productName,
                                              BigDecimal quantity,
                                              BigDecimal unitPrice,
                                              BigDecimal vatRate) {
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setStockId(stockId);
        line.setProductName(productName);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setVatRate(vatRate);
        return line;
    }

    private String buildRevisionOfferInvoiceDescription(RevisionOffer offer) {
        String baseDescription = "Converted from revision offer #" + offer.getId();
        String laborDescription = normalizeNullable(offer.getLaborDescription());
        if (!StringUtils.hasText(laborDescription)) {
            return baseDescription;
        }
        return baseDescription + " - " + laborDescription;
    }

    private SaleContext resolveSaleContextFromRevisionOffer(RevisionOffer offer) {
        Elevator elevator = offer.getElevator();
        Facility directFacility = elevator.getFacility();
        if (directFacility != null && directFacility.getB2bUnit() != null) {
            return new SaleContext(directFacility.getB2bUnit(), directFacility);
        }

        Facility resolvedFacility = resolveFacilityByRevisionOfferContext(offer);
        if (resolvedFacility != null && resolvedFacility.getB2bUnit() != null) {
            return new SaleContext(resolvedFacility.getB2bUnit(), resolvedFacility);
        }

        B2BUnit fallbackB2BUnit = resolveSingleActiveB2BUnit();
        if (fallbackB2BUnit != null) {
            return new SaleContext(fallbackB2BUnit, null);
        }

        throw new RuntimeException(
                "This revision offer cannot be converted to sale because no facility or B2B unit mapping could be resolved."
        );
    }

    private Facility resolveFacilityByRevisionOfferContext(RevisionOffer offer) {
        CurrentAccount currentAccount = offer.getCurrentAccount();
        if (currentAccount != null && currentAccount.getBuilding() != null) {
            Facility byBuilding = findActiveFacilityByName(currentAccount.getBuilding().getName());
            if (byBuilding != null) {
                return byBuilding;
            }
        }

        if (currentAccount != null) {
            Facility byAccountName = findActiveFacilityByName(currentAccount.getName());
            if (byAccountName != null) {
                return byAccountName;
            }
        }

        if (offer.getBuilding() != null) {
            Facility byOfferBuilding = findActiveFacilityByName(offer.getBuilding().getName());
            if (byOfferBuilding != null) {
                return byOfferBuilding;
            }
        }

        Elevator elevator = offer.getElevator();
        if (elevator != null) {
            return findActiveFacilityByName(elevator.getBuildingName());
        }

        return null;
    }

    private Facility findActiveFacilityByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return facilityRepository.findFirstByNameIgnoreCaseAndActiveTrue(name.trim()).orElse(null);
    }

    private B2BUnit resolveSingleActiveB2BUnit() {
        if (b2bUnitRepository.countByActiveTrue() != 1) {
            return null;
        }
        return b2bUnitRepository.findFirstByActiveTrueOrderByIdAsc().orElse(null);
    }

    private record SaleContext(B2BUnit b2bUnit, Facility facility) {
    }
}
