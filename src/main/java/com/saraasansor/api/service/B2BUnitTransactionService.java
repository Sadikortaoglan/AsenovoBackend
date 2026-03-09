package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.B2BUnitPaymentTransactionResponse;
import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionResponse;
import com.saraasansor.api.dto.BankCollectionCreateRequest;
import com.saraasansor.api.dto.BankPaymentCreateRequest;
import com.saraasansor.api.dto.CashCollectionCreateRequest;
import com.saraasansor.api.dto.CashPaymentCreateRequest;
import com.saraasansor.api.dto.CheckCollectionCreateRequest;
import com.saraasansor.api.dto.CheckPaymentCreateRequest;
import com.saraasansor.api.dto.CreditCardCollectionCreateRequest;
import com.saraasansor.api.dto.CreditCardPaymentCreateRequest;
import com.saraasansor.api.dto.ManualCreditCreateRequest;
import com.saraasansor.api.dto.ManualDebitCreateRequest;
import com.saraasansor.api.dto.PaytrCollectionCreateRequest;
import com.saraasansor.api.dto.PromissoryNoteCollectionCreateRequest;
import com.saraasansor.api.dto.PromissoryNotePaymentCreateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.repository.BankAccountRepository;
import com.saraasansor.api.repository.CashAccountRepository;
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
import java.util.EnumSet;
import java.util.Set;

@Service
@Transactional
public class B2BUnitTransactionService {

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2999, 12, 31);

    private static final Set<B2BUnitTransaction.TransactionType> COLLECTION_TYPES = EnumSet.of(
            B2BUnitTransaction.TransactionType.CASH_COLLECTION,
            B2BUnitTransaction.TransactionType.PAYTR_COLLECTION,
            B2BUnitTransaction.TransactionType.CREDIT_CARD_COLLECTION,
            B2BUnitTransaction.TransactionType.BANK_COLLECTION,
            B2BUnitTransaction.TransactionType.CHECK_COLLECTION,
            B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_COLLECTION
    );

    private static final Set<B2BUnitTransaction.TransactionType> PAYMENT_TYPES = EnumSet.of(
            B2BUnitTransaction.TransactionType.CASH_PAYMENT,
            B2BUnitTransaction.TransactionType.CREDIT_CARD_PAYMENT,
            B2BUnitTransaction.TransactionType.BANK_PAYMENT,
            B2BUnitTransaction.TransactionType.CHECK_PAYMENT,
            B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_PAYMENT
    );

    private final B2BUnitTransactionRepository transactionRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final FacilityRepository facilityRepository;
    private final CashAccountRepository cashAccountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public B2BUnitTransactionService(B2BUnitTransactionRepository transactionRepository,
                                     B2BUnitRepository b2bUnitRepository,
                                     FacilityRepository facilityRepository,
                                     CashAccountRepository cashAccountRepository,
                                     BankAccountRepository bankAccountRepository,
                                     UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.facilityRepository = facilityRepository;
        this.cashAccountRepository = cashAccountRepository;
        this.bankAccountRepository = bankAccountRepository;
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
        enforceCreateAccess("CARI users cannot create manual account transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.MANUAL_DEBIT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.MANUAL_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                null,
                null,
                null
        );

        return B2BUnitTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitTransactionResponse createManualCredit(Long b2bUnitId, ManualCreditCreateRequest request) {
        enforceCreateAccess("CARI users cannot create manual account transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.MANUAL_CREDIT,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.MANUAL_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                null,
                null,
                null
        );

        return B2BUnitTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createCashCollection(Long b2bUnitId, CashCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireCashAccount(request.getCashAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CASH_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                request.getCashAccountId(),
                null,
                null,
                null,
                null
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createPaytrCollection(Long b2bUnitId, PaytrCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.PAYTR_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                null,
                null,
                B2BUnitTransaction.PaymentProvider.PAYTR
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createCreditCardCollection(Long b2bUnitId, CreditCardCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireBankAccount(request.getBankAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CREDIT_CARD_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                request.getBankAccountId(),
                null,
                null,
                null
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createBankCollection(Long b2bUnitId, BankCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireBankAccount(request.getBankAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.BANK_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                request.getBankAccountId(),
                null,
                null,
                null
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createCheckCollection(Long b2bUnitId, CheckCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        validateDueDateAndSerial(request.getDueDate(), request.getSerialNumber());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CHECK_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                request.getDueDate(),
                request.getSerialNumber(),
                null
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitCollectionTransactionResponse createPromissoryNoteCollection(Long b2bUnitId,
                                                                               PromissoryNoteCollectionCreateRequest request) {
        enforceCreateAccess("CARI users cannot create collection transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        validateDueDateAndSerial(request.getDueDate(), request.getSerialNumber());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_COLLECTION,
                BigDecimal.ZERO,
                normalizeMoney(request.getAmount()),
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.COLLECTION_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                request.getDueDate(),
                request.getSerialNumber(),
                null
        );
        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitPaymentTransactionResponse createCashPayment(Long b2bUnitId, CashPaymentCreateRequest request) {
        enforceCreateAccess("CARI users cannot create payment transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireCashAccount(request.getCashAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CASH_PAYMENT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.PAYMENT_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                request.getCashAccountId(),
                null,
                null,
                null,
                null
        );
        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitPaymentTransactionResponse createCreditCardPayment(Long b2bUnitId, CreditCardPaymentCreateRequest request) {
        enforceCreateAccess("CARI users cannot create payment transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireBankAccount(request.getBankAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CREDIT_CARD_PAYMENT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.PAYMENT_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                request.getBankAccountId(),
                null,
                null,
                null
        );
        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitPaymentTransactionResponse createBankPayment(Long b2bUnitId, BankPaymentCreateRequest request) {
        enforceCreateAccess("CARI users cannot create payment transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        requireBankAccount(request.getBankAccountId());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.BANK_PAYMENT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.PAYMENT_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                request.getBankAccountId(),
                null,
                null,
                null
        );
        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitPaymentTransactionResponse createCheckPayment(Long b2bUnitId, CheckPaymentCreateRequest request) {
        enforceCreateAccess("CARI users cannot create payment transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        validateDueDateAndSerial(request.getDueDate(), request.getSerialNumber());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.CHECK_PAYMENT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.PAYMENT_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                request.getDueDate(),
                request.getSerialNumber(),
                null
        );
        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
    }

    public B2BUnitPaymentTransactionResponse createPromissoryNotePayment(Long b2bUnitId,
                                                                         PromissoryNotePaymentCreateRequest request) {
        enforceCreateAccess("CARI users cannot create payment transactions");
        validateBaseAmountAndDate(request.getTransactionDate(), request.getAmount());
        validateDueDateAndSerial(request.getDueDate(), request.getSerialNumber());

        Facility facility = resolveOptionalFacility(request.getFacilityId(), b2bUnitId);
        B2BUnitTransaction transaction = createTransactionRecord(
                b2bUnitId,
                request.getTransactionDate(),
                B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_PAYMENT,
                normalizeMoney(request.getAmount()),
                BigDecimal.ZERO,
                request.getDescription(),
                B2BUnitTransaction.ReferenceType.PAYMENT_TRANSACTION,
                null,
                null,
                facility,
                normalizeMoney(request.getAmount()),
                null,
                null,
                request.getDueDate(),
                request.getSerialNumber(),
                null
        );
        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public B2BUnitCollectionTransactionResponse getCollectionById(Long b2bUnitId, Long transactionId) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnit.getId());

        B2BUnitTransaction transaction = transactionRepository.findByIdAndB2bUnitId(transactionId, b2bUnitId)
                .orElseThrow(() -> new RuntimeException("Collection transaction not found"));

        if (!COLLECTION_TYPES.contains(transaction.getTransactionType())) {
            throw new RuntimeException("Collection transaction not found");
        }

        return B2BUnitCollectionTransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public B2BUnitPaymentTransactionResponse getPaymentById(Long b2bUnitId, Long transactionId) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnit.getId());

        B2BUnitTransaction transaction = transactionRepository.findByIdAndB2bUnitId(transactionId, b2bUnitId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        if (!PAYMENT_TYPES.contains(transaction.getTransactionType())) {
            throw new RuntimeException("Payment transaction not found");
        }

        return B2BUnitPaymentTransactionResponse.fromEntity(transaction);
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
                normalizeMoney(grandTotal),
                null,
                null,
                null,
                null,
                null
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
                normalizeMoney(grandTotal),
                null,
                null,
                null,
                null,
                null
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

    private void validateBaseAmountAndDate(LocalDate transactionDate, BigDecimal amount) {
        if (transactionDate == null) {
            throw new RuntimeException("transactionDate is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be greater than zero");
        }
    }

    private void validateDueDateAndSerial(LocalDate dueDate, String serialNumber) {
        if (dueDate == null) {
            throw new RuntimeException("dueDate is required");
        }
        if (!StringUtils.hasText(serialNumber)) {
            throw new RuntimeException("serialNumber is required");
        }
    }

    private void requireCashAccount(Long cashAccountId) {
        if (cashAccountId == null) {
            throw new RuntimeException("cashAccountId is required");
        }
        cashAccountRepository.findByIdAndActiveTrue(cashAccountId)
                .orElseThrow(() -> new RuntimeException("Cash account not found"));
    }

    private void requireBankAccount(Long bankAccountId) {
        if (bankAccountId == null) {
            throw new RuntimeException("bankAccountId is required");
        }
        bankAccountRepository.findByIdAndActiveTrue(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
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

    private void enforceCreateAccess(String message) {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException(message);
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
                                                       BigDecimal amount,
                                                       Long cashAccountId,
                                                       Long bankAccountId,
                                                       LocalDate dueDate,
                                                       String serialNumber,
                                                       B2BUnitTransaction.PaymentProvider paymentProvider) {
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
        transaction.setDescription(normalizeNullable(description));

        transaction.setCashAccountId(cashAccountId);
        transaction.setBankAccountId(bankAccountId);
        transaction.setDueDate(dueDate);
        transaction.setSerialNumber(normalizeNullable(serialNumber));
        transaction.setPaymentProvider(paymentProvider);

        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setReferenceCode(normalizeNullable(referenceCode));
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
