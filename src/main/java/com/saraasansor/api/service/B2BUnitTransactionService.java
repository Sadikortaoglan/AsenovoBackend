package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionResponse;
import com.saraasansor.api.dto.ManualCreditCreateRequest;
import com.saraasansor.api.dto.ManualDebitCreateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Service
@Transactional
public class B2BUnitTransactionService {

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2999, 12, 31);

    private final B2BUnitTransactionRepository transactionRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;

    public B2BUnitTransactionService(B2BUnitTransactionRepository transactionRepository,
                                     B2BUnitRepository b2bUnitRepository,
                                     FacilityRepository facilityRepository,
                                     UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public B2BUnitTransactionPageResponse getTransactions(Long b2bUnitId,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          String search,
                                                          Pageable pageable) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnit.getId());

        LocalDate effectiveStartDate = startDate != null ? startDate : DEFAULT_START_DATE;
        LocalDate effectiveEndDate = endDate != null ? endDate : DEFAULT_END_DATE;
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new RuntimeException("startDate cannot be after endDate");
        }

        String normalizedSearch = normalizeNullable(search);
        B2BUnitTransaction.TransactionType transactionType = resolveTransactionType(normalizedSearch);

        PageRequest sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "transactionDate").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<B2BUnitTransaction> pageResult = transactionRepository.searchLedger(
                b2bUnitId,
                effectiveStartDate,
                effectiveEndDate,
                normalizedSearch,
                transactionType,
                sortedPageable
        );

        B2BUnitTransactionPageResponse response = new B2BUnitTransactionPageResponse();
        response.setContent(pageResult.map(B2BUnitTransactionResponse::fromEntity).getContent());
        response.setPage(pageResult.getNumber());
        response.setSize(pageResult.getSize());
        response.setTotalElements(pageResult.getTotalElements());
        response.setTotalPages(pageResult.getTotalPages());
        return response;
    }

    public B2BUnitTransactionResponse createManualDebit(Long b2bUnitId, ManualDebitCreateRequest request) {
        enforceCreateAccess();
        validateManualRequest(request.getTransactionDate(), request.getAmount());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.MANUAL_DEBIT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                normalizeNullable(request.getDescription()),
                B2BUnitTransaction.ReferenceType.MANUAL_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount())
        );

        return B2BUnitTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitTransactionResponse createManualCredit(Long b2bUnitId, ManualCreditCreateRequest request) {
        enforceCreateAccess();
        validateManualRequest(request.getTransactionDate(), request.getAmount());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.MANUAL_CREDIT,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                normalizeNullable(request.getDescription()),
                B2BUnitTransaction.ReferenceType.MANUAL_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount())
        );

        return B2BUnitTransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public B2BUnitTransactionResponse getTransactionById(Long b2bUnitId, Long transactionId) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnit.getId());

        B2BUnitTransaction transaction = transactionRepository.findByIdAndB2bUnitId(transactionId, b2bUnitId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return B2BUnitTransactionResponse.fromEntity(transaction);
    }

    public byte[] exportTransactionsExcel(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("Excel export is not implemented yet");
    }

    public byte[] exportTransactionsExcel() {
        throw new UnsupportedOperationException("Excel export is not implemented yet");
    }

    public byte[] exportTransactionsPdf(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("PDF export is not implemented yet");
    }

    public byte[] exportTransactionsPdf() {
        throw new UnsupportedOperationException("PDF export is not implemented yet");
    }

    public byte[] exportTransactionsCsv(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("CSV export is not implemented yet");
    }

    public byte[] exportTransactionsCsv() {
        throw new UnsupportedOperationException("CSV export is not implemented yet");
    }

    public void onPurchaseInvoicePosted(Long b2bUnitId,
                                        LocalDate invoiceDate,
                                        BigDecimal grandTotal,
                                        String referenceCode,
                                        String description) {
        createTransactionRecord(
                b2bUnitId,
                invoiceDate,
                B2BUnitTransaction.TransactionType.PURCHASE,
                normalizeMoney(grandTotal),
                BigDecimal.ZERO,
                description,
                null,
                null,
                referenceCode,
                null,
                normalizeMoney(grandTotal)
        );
    }

    public void onSalesInvoicePosted(Long b2bUnitId,
                                     LocalDate invoiceDate,
                                     BigDecimal grandTotal,
                                     String referenceCode,
                                     String description) {
        createTransactionRecord(
                b2bUnitId,
                invoiceDate,
                B2BUnitTransaction.TransactionType.SALE,
                BigDecimal.ZERO,
                normalizeMoney(grandTotal),
                description,
                null,
                null,
                referenceCode,
                null,
                normalizeMoney(grandTotal)
        );
    }

    private B2BUnitTransaction.TransactionType resolveTransactionType(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String normalized = search.trim().toUpperCase();
        for (B2BUnitTransaction.TransactionType value : B2BUnitTransaction.TransactionType.values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    private void validateManualRequest(LocalDate transactionDate, BigDecimal amount) {
        if (transactionDate == null) {
            throw new RuntimeException("transactionDate is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be greater than zero");
        }
    }

    private Facility resolveOptionalFacility(Long facilityId, Long b2bUnitId) {
        if (facilityId == null) {
            return null;
        }

        Facility facility = facilityRepository.findByIdAndActiveTrue(facilityId)
                .orElseThrow(() -> new RuntimeException("Facility not found"));
        Long facilityB2bUnitId = facility.getB2bUnit() != null ? facility.getB2bUnit().getId() : null;
        if (facilityB2bUnitId == null || !facilityB2bUnitId.equals(b2bUnitId)) {
            throw new RuntimeException("Facility does not belong to selected B2B unit");
        }
        return facility;
    }

    private void enforceCreateAccess() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException("CARI users cannot create manual account transactions");
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

    private String resolveCurrentUsername() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUsername() : null;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private B2BUnitTransaction createTransactionRecord(Long b2bUnitId,
                                                       LocalDate transactionDate,
                                                       B2BUnitTransaction.TransactionType transactionType,
                                                       BigDecimal debitAmount,
                                                       BigDecimal creditAmount,
                                                       String description,
                                                       B2BUnitTransaction.ReferenceType referenceType,
                                                       Long referenceId,
                                                       String referenceCode,
                                                       Facility facility,
                                                       BigDecimal amount) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));

        BigDecimal normalizedDebit = normalizeMoney(debitAmount);
        BigDecimal normalizedCredit = normalizeMoney(creditAmount);

        BigDecimal lastBalance = transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(b2bUnitId)
                .map(B2BUnitTransaction::getBalanceAfterTransaction)
                .orElse(BigDecimal.ZERO);

        BigDecimal balanceAfterTransaction = normalizeMoney(lastBalance
                .add(normalizedDebit)
                .subtract(normalizedCredit));

        B2BUnitTransaction transaction = new B2BUnitTransaction();
        transaction.setB2bUnit(b2bUnit);
        transaction.setFacility(facility);
        transaction.setTransactionDate(transactionDate != null ? transactionDate : LocalDate.now());
        transaction.setTransactionType(transactionType);
        transaction.setAmount(normalizeMoney(amount));
        transaction.setDebitAmount(normalizedDebit);
        transaction.setCreditAmount(normalizedCredit);
        transaction.setBalanceAfterTransaction(balanceAfterTransaction);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setReferenceCode(normalizeNullable(referenceCode));
        transaction.setDescription(normalizeNullable(description));
        transaction.setStatus(B2BUnitTransaction.TransactionStatus.POSTED);
        transaction.setCreatedBy(resolveCurrentUsername());
        return transactionRepository.save(transaction);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
