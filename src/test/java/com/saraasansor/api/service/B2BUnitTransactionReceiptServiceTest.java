package com.saraasansor.api.service;

import com.saraasansor.api.dto.CollectionReceiptPageResponse;
import com.saraasansor.api.dto.CollectionReceiptPrintResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitTransactionReceiptServiceTest {

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
    void shouldScopeCollectionReceiptsToCariUsersOwnB2bUnit() {
        authenticateAs("cari_user");
        User cari = new User();
        cari.setUsername("cari_user");
        cari.setRole(User.Role.CARI_USER);
        cari.setB2bUnit(b2bUnit(90L, "Cari Unit"));
        when(userRepository.findByUsername("cari_user")).thenReturn(Optional.of(cari));

        B2BUnitTransaction cashCollection = collectionTransaction(
                1L,
                90L,
                "Cari Unit",
                B2BUnitTransaction.TransactionType.CASH_COLLECTION,
                "100.00"
        );
        B2BUnitTransaction paytrCollection = collectionTransaction(
                2L,
                90L,
                "Cari Unit",
                B2BUnitTransaction.TransactionType.PAYTR_COLLECTION,
                "50.00"
        );
        when(transactionRepository.searchCollectionReceipts(
                any(Set.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(),
                any(),
                eq(90L),
                any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(cashCollection, paytrCollection)));

        CollectionReceiptPageResponse response = service.getCollectionReceipts(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "",
                PageRequest.of(0, 25)
        );

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getCollectionType()).isEqualTo(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        assertThat(response.getContent().get(1).getCollectionType()).isEqualTo(B2BUnitTransaction.TransactionType.PAYTR_COLLECTION);
        verify(transactionRepository).searchCollectionReceipts(
                any(Set.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(),
                any(),
                eq(90L),
                any(PageRequest.class)
        );
    }

    @Test
    void shouldReturnPrintableCollectionReceiptPayload() {
        B2BUnitTransaction transaction = collectionTransaction(
                5L,
                8L,
                "Beta B2B",
                B2BUnitTransaction.TransactionType.BANK_COLLECTION,
                "340.00"
        );
        transaction.setBankAccountId(66L);
        transaction.setDescription("bank collection");
        transaction.getB2bUnit().setAddress("B2B HQ");
        Facility facility = new Facility();
        facility.setId(3L);
        facility.setName("Facility A");
        facility.setAddressText("Facility Address");
        transaction.setFacility(facility);

        when(transactionRepository.findWithDetailsById(5L)).thenReturn(Optional.of(transaction));

        CollectionReceiptPrintResponse response = service.getCollectionReceiptPrint(5L);

        assertThat(response.getReceiptNumber()).isEqualTo("COL-00000005");
        assertThat(response.getB2bUnitName()).isEqualTo("Beta B2B");
        assertThat(response.getFacilityName()).isEqualTo("Facility A");
        assertThat(response.getBankAccountId()).isEqualTo(66L);
    }

    @Test
    void shouldRejectPrintForNonCollectionTransaction() {
        B2BUnitTransaction transaction = collectionTransaction(
                7L,
                2L,
                "Unit X",
                B2BUnitTransaction.TransactionType.MANUAL_DEBIT,
                "10.00"
        );
        when(transactionRepository.findWithDetailsById(7L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.getCollectionReceiptPrint(7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Collection receipt not found");
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "password", List.of())
        );
    }

    private B2BUnit b2bUnit(Long id, String name) {
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(id);
        b2bUnit.setName(name);
        return b2bUnit;
    }

    private B2BUnitTransaction collectionTransaction(Long id,
                                                     Long b2bUnitId,
                                                     String b2bUnitName,
                                                     B2BUnitTransaction.TransactionType type,
                                                     String amount) {
        B2BUnitTransaction transaction = new B2BUnitTransaction();
        transaction.setId(id);
        transaction.setB2bUnit(b2bUnit(b2bUnitId, b2bUnitName));
        transaction.setTransactionDate(LocalDate.of(2026, 4, 1));
        transaction.setTransactionType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCreditAmount(new BigDecimal(amount));
        transaction.setDebitAmount(BigDecimal.ZERO);
        transaction.setBalanceAfterTransaction(new BigDecimal("500.00"));
        transaction.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transaction.setCreatedBy("staff");
        return transaction;
    }
}
