package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitTransactionServiceTest {

    @Mock
    private B2BUnitTransactionRepository transactionRepository;

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private UserRepository userRepository;

    private B2BUnitTransactionService service;

    @BeforeEach
    void setUp() {
        service = new B2BUnitTransactionService(transactionRepository, b2bUnitRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFilterByDateRangeAndReturnTransactions() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));

        B2BUnit b2bUnit = activeB2BUnit(5L);
        when(b2bUnitRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(b2bUnit));

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        B2BUnitTransaction tx = tx(5L, LocalDate.of(2025, 2, 1), B2BUnitTransaction.TransactionType.SALE);
        Page<B2BUnitTransaction> txPage = new PageImpl<>(
                List.of(tx),
                PageRequest.of(0, 25),
                1
        );
        when(transactionRepository.searchLedger(eq(5L), eq(startDate), eq(endDate), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(txPage);

        B2BUnitTransactionPageResponse response = service.getTransactions(5L, startDate, endDate, null, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.SALE);
        verify(transactionRepository, times(1))
                .searchLedger(eq(5L), eq(startDate), eq(endDate), isNull(), isNull(), any(PageRequest.class));
    }

    @Test
    void shouldReturnPaginationMetadata() {
        authenticateAs("staff-admin");
        when(userRepository.findByUsername("staff-admin")).thenReturn(Optional.of(staffUser("staff-admin")));
        when(b2bUnitRepository.findByIdAndActiveTrue(8L)).thenReturn(Optional.of(activeB2BUnit(8L)));

        Page<B2BUnitTransaction> txPage = new PageImpl<>(
                List.of(tx(8L, LocalDate.of(2026, 1, 5), B2BUnitTransaction.TransactionType.PAYMENT)),
                PageRequest.of(0, 25),
                40
        );
        when(transactionRepository.searchLedger(eq(8L), any(LocalDate.class), any(LocalDate.class), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(txPage);

        B2BUnitTransactionPageResponse response = service.getTransactions(8L, null, null, null, PageRequest.of(0, 25));

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(25);
        assertThat(response.getTotalElements()).isEqualTo(40);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldRejectCariUserAccessToAnotherB2BUnit() {
        authenticateAs("cari-user");
        User cariUser = new User();
        cariUser.setUsername("cari-user");
        cariUser.setRole(User.Role.CARI_USER);
        B2BUnit ownB2B = new B2BUnit();
        ownB2B.setId(10L);
        cariUser.setB2bUnit(ownB2B);

        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cariUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(activeB2BUnit(11L)));

        assertThatThrownBy(() -> service.getTransactions(11L, null, null, null, PageRequest.of(0, 25)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit");

        verify(transactionRepository, never()).searchLedger(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldHandleEmptyResult() {
        authenticateAs("staff-user");
        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser("staff-user")));
        when(b2bUnitRepository.findByIdAndActiveTrue(12L)).thenReturn(Optional.of(activeB2BUnit(12L)));
        when(transactionRepository.searchLedger(eq(12L), any(LocalDate.class), any(LocalDate.class), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 25)));

        B2BUnitTransactionPageResponse response = service.getTransactions(12L, null, null, null, PageRequest.of(0, 25));

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "x", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User staffUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(User.Role.STAFF_USER);
        user.setUserType(User.UserType.STAFF);
        return user;
    }

    private B2BUnit activeB2BUnit(Long id) {
        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(id);
        b2bUnit.setActive(true);
        return b2bUnit;
    }

    private B2BUnitTransaction tx(Long b2bUnitId, LocalDate date, B2BUnitTransaction.TransactionType type) {
        B2BUnitTransaction transaction = new B2BUnitTransaction();
        B2BUnit b2bUnit = activeB2BUnit(b2bUnitId);
        transaction.setB2bUnit(b2bUnit);
        transaction.setTransactionDate(date);
        transaction.setTransactionType(type);
        transaction.setDebitAmount(BigDecimal.TEN);
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setBalanceAfterTransaction(BigDecimal.ONE);
        transaction.setDescription("test");
        return transaction;
    }
}
