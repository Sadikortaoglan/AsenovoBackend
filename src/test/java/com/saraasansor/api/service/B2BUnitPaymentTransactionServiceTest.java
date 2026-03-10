package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitPaymentTransactionResponse;
import com.saraasansor.api.dto.BankPaymentCreateRequest;
import com.saraasansor.api.dto.CashPaymentCreateRequest;
import com.saraasansor.api.dto.CheckPaymentCreateRequest;
import com.saraasansor.api.dto.CreditCardPaymentCreateRequest;
import com.saraasansor.api.dto.PromissoryNotePaymentCreateRequest;
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
class B2BUnitPaymentTransactionServiceTest {

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
    void shouldCreateCashPaymentSuccessfully() {
        authenticateAsStaff("staff");
        when(cashAccountRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(cashAccount(10L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "100.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashPaymentCreateRequest request = new CashPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("30"));
        request.setCashAccountId(10L);

        B2BUnitPaymentTransactionResponse response = service.createCashPayment(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CASH_PAYMENT);
        assertThat(response.getDebitAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("130.00");
    }

    @Test
    void shouldCreateCreditCardPaymentSuccessfully() {
        authenticateAsStaff("staff");
        when(bankAccountRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(bankAccount(20L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "80.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardPaymentCreateRequest request = new CreditCardPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("20"));
        request.setBankAccountId(20L);

        B2BUnitPaymentTransactionResponse response = service.createCreditCardPayment(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CREDIT_CARD_PAYMENT);
        assertThat(response.getBankAccountId()).isEqualTo(20L);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldCreateBankPaymentSuccessfully() {
        authenticateAsStaff("staff");
        when(bankAccountRepository.findByIdAndActiveTrue(21L)).thenReturn(Optional.of(bankAccount(21L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "20.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankPaymentCreateRequest request = new BankPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("50"));
        request.setBankAccountId(21L);

        B2BUnitPaymentTransactionResponse response = service.createBankPayment(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.BANK_PAYMENT);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("70.00");
    }

    @Test
    void shouldCreateCheckPaymentSuccessfully() {
        authenticateAsStaff("staff");
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "10.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckPaymentCreateRequest request = new CheckPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setDueDate(LocalDate.of(2026, 4, 1));
        request.setSerialNumber("CHK-P-001");
        request.setAmount(new BigDecimal("15"));

        B2BUnitPaymentTransactionResponse response = service.createCheckPayment(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CHECK_PAYMENT);
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(response.getSerialNumber()).isEqualTo("CHK-P-001");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("25.00");
    }

    @Test
    void shouldCreatePromissoryNotePaymentSuccessfully() {
        authenticateAsStaff("staff");
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "10.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromissoryNotePaymentCreateRequest request = new PromissoryNotePaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setDueDate(LocalDate.of(2026, 5, 1));
        request.setSerialNumber("SEN-P-001");
        request.setAmount(new BigDecimal("35"));

        B2BUnitPaymentTransactionResponse response = service.createPromissoryNotePayment(5L, request);

        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.PROMISSORY_NOTE_PAYMENT);
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("45.00");
    }

    @Test
    void shouldValidateAmountForPayment() {
        authenticateAsStaff("staff");

        CashPaymentCreateRequest request = new CashPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(BigDecimal.ZERO);
        request.setCashAccountId(10L);

        assertThatThrownBy(() -> service.createCashPayment(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    void shouldValidateRequiredAccountFields() {
        authenticateAsStaff("staff");

        CashPaymentCreateRequest cashRequest = new CashPaymentCreateRequest();
        cashRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        cashRequest.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createCashPayment(5L, cashRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cashAccountId is required");

        BankPaymentCreateRequest bankRequest = new BankPaymentCreateRequest();
        bankRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        bankRequest.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createBankPayment(5L, bankRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bankAccountId is required");
    }

    @Test
    void shouldValidateDueDateAndSerialForPayment() {
        authenticateAsStaff("staff");

        CheckPaymentCreateRequest checkRequest = new CheckPaymentCreateRequest();
        checkRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        checkRequest.setAmount(new BigDecimal("10"));

        assertThatThrownBy(() -> service.createCheckPayment(5L, checkRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dueDate is required");

        PromissoryNotePaymentCreateRequest noteRequest = new PromissoryNotePaymentCreateRequest();
        noteRequest.setTransactionDate(LocalDate.of(2026, 3, 9));
        noteRequest.setAmount(new BigDecimal("10"));
        noteRequest.setDueDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> service.createPromissoryNotePayment(5L, noteRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("serialNumber is required");
    }

    @Test
    void shouldRejectInvalidFacilityB2BUnitRelationForPayment() {
        authenticateAsStaff("staff");

        B2BUnit otherB2BUnit = activeB2BUnit(55L);
        Facility facility = facility(100L, otherB2BUnit);
        when(facilityRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(facility));

        CashPaymentCreateRequest request = new CashPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("25"));
        request.setCashAccountId(10L);
        request.setFacilityId(100L);

        when(cashAccountRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(cashAccount(10L)));

        assertThatThrownBy(() -> service.createCashPayment(5L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Facility does not belong to selected B2B unit");
    }

    @Test
    void shouldForbidCariUserForPaymentCreate() {
        authenticateAs("cari");
        User cariUser = new User();
        cariUser.setUsername("cari");
        cariUser.setRole(User.Role.CARI_USER);
        when(userRepository.findByUsername("cari")).thenReturn(Optional.of(cariUser));

        CashPaymentCreateRequest request = new CashPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("10"));
        request.setCashAccountId(10L);

        assertThatThrownBy(() -> service.createCashPayment(5L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot create payment transactions");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldCalculateBalanceAsPreviousPlusAmountForPayment() {
        authenticateAsStaff("staff");
        when(cashAccountRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(cashAccount(10L)));
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(activeB2BUnit(5L)));
        when(transactionRepository.findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(5L))
                .thenReturn(Optional.of(transactionWithBalance(5L, "40.00")));
        when(transactionRepository.save(any(B2BUnitTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashPaymentCreateRequest request = new CashPaymentCreateRequest();
        request.setTransactionDate(LocalDate.of(2026, 3, 9));
        request.setAmount(new BigDecimal("10.00"));
        request.setCashAccountId(10L);

        B2BUnitPaymentTransactionResponse response = service.createCashPayment(5L, request);

        assertThat(response.getDebitAmount()).isEqualByComparingTo("10.00");
        assertThat(response.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getBalanceAfterTransaction()).isEqualByComparingTo("50.00");
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
