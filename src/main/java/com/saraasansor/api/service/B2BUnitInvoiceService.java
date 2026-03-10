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
import com.saraasansor.api.model.User;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.B2BUnitInvoiceRepository;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FacilityRepository;
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
import java.util.List;

@Service
@Transactional
public class B2BUnitInvoiceService {

    private final B2BUnitInvoiceRepository invoiceRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final FacilityRepository facilityRepository;
    private final ElevatorRepository elevatorRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final B2BUnitTransactionService transactionService;

    public B2BUnitInvoiceService(B2BUnitInvoiceRepository invoiceRepository,
                                 B2BUnitRepository b2bUnitRepository,
                                 FacilityRepository facilityRepository,
                                 ElevatorRepository elevatorRepository,
                                 WarehouseRepository warehouseRepository,
                                 UserRepository userRepository,
                                 B2BUnitTransactionService transactionService) {
        this.invoiceRepository = invoiceRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.facilityRepository = facilityRepository;
        this.elevatorRepository = elevatorRepository;
        this.warehouseRepository = warehouseRepository;
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

        applyLinesAndTotals(invoice, lines);

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

        B2BUnitInvoice invoice = new B2BUnitInvoice();
        invoice.setInvoiceType(B2BUnitInvoice.InvoiceType.SALES);
        invoice.setB2bUnit(b2bUnit);
        invoice.setFacility(facility);
        invoice.setElevator(elevator);
        invoice.setWarehouse(warehouse);
        invoice.setInvoiceDate(requireInvoiceDate(request.getInvoiceDate()));
        invoice.setDescription(normalizeNullable(request.getDescription()));
        invoice.setStatus(B2BUnitInvoice.InvoiceStatus.POSTED);
        invoice.setCreatedBy(resolveCurrentUsername());

        applyLinesAndTotals(invoice, lines);

        B2BUnitInvoice saved = invoiceRepository.save(invoice);
        transactionService.onSalesInvoicePosted(
                b2bUnitId,
                saved.getInvoiceDate(),
                saved.getGrandTotal(),
                buildReferenceCode(saved),
                saved.getDescription()
        );
        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long b2bUnitId, Long invoiceId) {
        resolveB2BUnitWithAccessCheck(b2bUnitId);
        B2BUnitInvoice invoice = invoiceRepository.findByIdAndB2bUnitId(invoiceId, b2bUnitId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return InvoiceResponse.fromEntity(invoice);
    }

    private void applyLinesAndTotals(B2BUnitInvoice invoice, List<InvoiceLineRequest> lineRequests) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        int sortOrder = 0;

        for (InvoiceLineRequest lineRequest : lineRequests) {
            validateLine(lineRequest);
            B2BUnitInvoiceLine line = new B2BUnitInvoiceLine();
            line.setProductName(normalizeRequired(lineRequest.getProductName(), "productName is required"));
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

    private void validateLine(InvoiceLineRequest line) {
        if (!StringUtils.hasText(line.getProductName())) {
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
}
