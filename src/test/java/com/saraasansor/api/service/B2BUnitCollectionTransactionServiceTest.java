package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.BankCollectionCreateRequest;
import com.saraasansor.api.dto.CashCollectionCreateRequest;
import com.saraasansor.api.dto.CheckCollectionCreateRequest;
import com.saraasansor.api.dto.CreditCardCollectionCreateRequest;
import com.saraasansor.api.dto.PaytrCollectionCreateRequest;
import com.saraasansor.api.dto.PromissoryNoteCollectionCreateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.model.BankAccount;
import com.saraasansor.api.model.CashAccount;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.repository.BankAccountRepository;
import com.saraasansor.api.repository.CashAccountRepository;
import com.saraasansor.api.repository.FacilityRepository;
import com.saraasansor.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitCollectionTransactionServiceTest {

    @Mock
    private B2BUnitTransactionRepository transactionRepository;
    @Mock
    private B2BUnitRepository b2bUnitRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private CashAccountRepository cashAccountRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private UserRepository userRepository;

    private B2BUnitTransactionService service;

    @BeforeEach
    void setUp() {
        service = new B2BUnitTransactionService(
                transactionRepository,
                b2bUnitRepository,
                facilityRepository,
                cashAccountRepository,
                bankAccountRepository,
                userRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateCashCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(cashAccountRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(cashAccount(10L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "150.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        CashCollectionCreateRequest request = new CashCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("50"));
        request.setCashAccountId(10L);
        request.setDescription("cash collection");

        B2BUnitCollectionTransactionResponse response = service.createCashCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        assertThat(response.getCreditAmount()).isEqualByComparingTo("50.00");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("100.00");
        assertThat(response.getCashAccountId()).isEqualTo(10L);
    }

    @Test
    void shouldCreatePaytrCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "120.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        PaytrCollectionCreateRequest request = new PaytrCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("20"));

        B2BUnitCollectionTransactionResponse response = service.createPaytrCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.PAYTR_COLLECTION);
        assertThat(response.getPaymentProvider()).isEqualTo(B2BUnitTransaction.PaymentProvider.PAYTR);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldCreateCreditCardCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(bankAccountRepository.findByIdAndActiveTrue(30L)).thenReturn(Optional.of(bankAccount(30L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "200.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(3L);
            return t;
        });

        CreditCardCollectionCreateRequest request = new CreditCardCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("80"));
        request.setBankAccountId(30L);

        B2BUnitCollectionTransactionResponse response = service.createCreditCardCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CREDIT_CARD_COLLECTION);
        assertThat(response.getBankAccountId()).isEqualTo(30L);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("120.00");
    }

    @Test
    void shouldCreateBankCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(bankAccountRepository.findByIdAndActiveTrue(31L)).thenReturn(Optional.of(bankAccount(31L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "90.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(4L);
            return t;
        });

        BankCollectionCreateRequest request = new BankCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("40"));
        request.setBankAccountId(31L);

        B2BUnitCollectionTransactionResponse response = service.createBankCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.BANK_COLLECTION);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldCreateCheckCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "300.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(5L);
            return t;
        });

        CheckCollectionCreateRequest request = new CheckCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setDueDate(LocalDate.of(2026, 4, 1));
        request.setSerialNumber("CHK-100");
        request.setAmount(new BigDecimal("100"));

        B2BUnitCollectionTransactionResponse response = service.createCheckCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CHECK_COLLECTION);
        assertThat(response.getSerialNumber()).isEqualTo("CHK-100");
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldCreatePromissoryNoteCollectionSuccessfully() {
        authenticateAsStaff("staff");
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "300.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> {
            B2BUnitTransaction t = invocation.getArgument(0);
            t.setId(6L);
            return t;
        });

        PromissoryNoteCollectionCreateRequest request = new PromissoryNoteCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setDueDate(LocalDate.of(2026, 5, 1));
        request.setSerialNumber("SNT-200");
        request.setAmount(new BigDecimal("120"));

        B2BUnitCollectionTransactionResponse response = service.createPromissoryNoteCollection(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_COLLECTION);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("180.00");
    }

    @Test
    void shouldValidateAmountForCollection() {
        authenticateAsStaff("staff");

        CashCollectionCreateRequest request = new CashCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(BigDecimal.ZERO);
        request.setCashAccountId(1L);

        assertThatThrownBy(() -> service.createCashCollection(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void shouldValidateRequiredAccountFields() {
        authenticateAsStaff("staff");

        CashCollectionCreateRequest cashRequest = new CashCollectionCreateRequest();
        cashRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        cashRequest.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createCashCollection(5L, cashRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cashAccountId is required");

        BankCollectionCreateRequest request = new BankCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createBankCollection(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bankAccountId is required");
    }

    @Test
    void shouldRejectInvalidBankIdForCollectionFlow() {
        authenticateAsStaff("staff");
        when(bankAccountRepository.findByIdAndActiveTrue(998L)).thenReturn(Optional.empty());

        BankCollectionCreateRequest request = new BankCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("40"));
        request.setBankAccountId(998L);

        assertThatThrownBy(() -> service.createBankCollection(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bank account not found");
    }

    @Test
    void shouldRejectInvalidCashboxIdForCollectionFlow() {
        authenticateAsStaff("staff");
        when(cashAccountRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        CashCollectionCreateRequest request = new CashCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("10"));
        request.setCashAccountId(999L);

        assertThatThrownBy(() -> service.createCashCollection(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cash account not found");
    }

    @Test
    void shouldValidateDueDateAndSerialForCheckAndPromissory() {
        authenticateAsStaff("staff");

        CheckCollectionCreateRequest checkRequest = new CheckCollectionCreateRequest();
        checkRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        checkRequest.setAmount(new BigDecimal("10"));
        checkRequest.setSerialNumber("   ");

        assertThatThrownBy(() -> service.createCheckCollection(5L, checkRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dueDate is required");

        PromissoryNoteCollectionCreateRequest noteRequest = new PromissoryNoteCollectionCreateRequest();
        noteRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        noteRequest.setAmount(new BigDecimal("10"));
        noteRequest.setDueDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> service.createPromissoryNoteCollection(5L, noteRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("serialNumber is required");
    }

    @Test
    void shouldRejectInvalidFacilityB2BUnitRelation() {
        authenticateAsStaff("staff");

        B2BUnit otherB2BUnit = activeB2BUnit(55L);
        Facility facility = facility(100L, otherB2BUnit);
        when(facilityRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(facility));

        PaytrCollectionCreateRequest request = new PaytrCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("25"));
        request.setFacilityId(100L);

        assertThatThrownBy(() -> service.createPaytrCollection(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Facility does not belong to selected B2B unit");
    }

    @Test
    void shouldForbidCariUserForCollectionCreate() {
        authenticateAs("cari");
        User cariUser = new User();
        cariUser.setUsername("cari");
        cariUser.setRole(User.Role.CARI_USER);
        when(userRepository.findByUsername("cari")).thenReturn(Optional.of(cariUser));

        PaytrCollectionCreateRequest request = new PaytrCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createPaytrCollection(5L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot create collection transactions");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldCalculateBalanceAsPreviousMinusAmountForCollection() {
        authenticateAsStaff("staff");
        when(cashAccountRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(cashAccount(2L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "100.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashCollectionCreateRequest request = new CashCollectionCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("30.00"));
        request.setCashAccountId(2L);

        B2BUnitCollectionTransactionResponse response = service.createCashCollection(5L, request);

        assertThat(response.getDebitAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getCreditAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("70.00");
    }

    private void authenticateAsStaff(String username) {
        authenticateAs(username);
        User user = new User();
        user.setUsername(username);
        user.setRole(User.Role.STAFF_USER);
        user.setUserType(User.UserType.STAFF);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private B2BUnit activeB2BUnit(Long id) {
        B2BUnit unit = new B2BUnit();
        unit.setId(id);
        unit.setActive(true);
        return unit;
    }

    private Facility facility(Long id, B2BUnit b2bUnit) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setB2bUnit(b2bUnit);
        facility.setActive(true);
        return facility;
    }

    private CashAccount cashAccount(Long id) {
        CashAccount account = new CashAccount();
        account.setId(id);
        account.setActive(true);
        account.setName("Cash Account");
        return account;
    }

    private BankAccount bankAccount(Long id) {
        BankAccount account = new BankAccount();
        account.setId(id);
        account.setActive(true);
        account.setName("Bank Account");
        return account;
    }

    private B2BUnitTransaction transactionWithBalance(Long b2bUnitId, String balance) {
        B2BUnitTransaction transaction = new B2BUnitTransaction();
        transaction.setB2bUnit(activeB2BUnit(b2bUnitId));
        transaction.setBalanceAfterTransaction(new BigDecimal(balance));
        return transaction;
    }
}
