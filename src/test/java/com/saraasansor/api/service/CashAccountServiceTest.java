package com.saraasansor.api.service;

import com.saraasansor.api.dto.CashboxCreateRequest;
import com.saraasansor.api.dto.CashboxPageResponse;
import com.saraasansor.api.dto.CashboxResponse;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.CashAccount;
import com.saraasansor.api.repository.CashAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashAccountServiceTest {

    @Mock
    private CashAccountRepository cashAccountRepository;

    private CashAccountService service;

    @BeforeEach
    void setUp() {
        service = new CashAccountService(cashAccountRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateCashboxUsingExistingCurrencyModel() {
        CashboxCreateRequest request = new CashboxCreateRequest();
        request.setName("Main Cashbox");
        request.setCurrencyCode("TRY");
        request.setDescription("default");
        request.setActive(true);

        when(cashAccountRepository.existsByNameIgnoreCase("Main Cashbox")).thenReturn(false);
        when(cashAccountRepository.save(any(CashAccount.class))).thenAnswer(invocation -> {
            CashAccount account = invocation.getArgument(0);
            account.setId(5L);
            account.setCreatedAt(LocalDateTime.of(2026, 4, 3, 9, 0));
            account.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 9, 0));
            return account;
        });

        CashboxResponse response = service.createCashbox(request);

        ArgumentCaptor<CashAccount> captor = ArgumentCaptor.forClass(CashAccount.class);
        verify(cashAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(B2BCurrency.TRY);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("staff");
        assertThat(response.getCurrencyCode()).isEqualTo("TRY");
        assertThat(response.getCurrencyName()).isEqualTo("Turkish Lira");
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        CashboxCreateRequest request = new CashboxCreateRequest();
        request.setName("Main Cashbox");
        request.setCurrencyCode("XYZ");

        when(cashAccountRepository.existsByNameIgnoreCase("Main Cashbox")).thenReturn(false);

        assertThatThrownBy(() -> service.createCashbox(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Currency not found");
    }

    @Test
    void shouldReturnCashboxListWithSearchAndActiveFilter() {
        CashAccount active = cashbox(1L, "Kasa 1", B2BCurrency.TRY, true);
        when(cashAccountRepository.search(eq("kasa"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(active)));

        CashboxPageResponse response = service.getCashboxes("kasa", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCurrencyCode()).isEqualTo("TRY");
        assertThat(response.getContent().get(0).getName()).isEqualTo("Kasa 1");
    }

    @Test
    void shouldReturnTenantScopedCashboxLookupWithCurrencyFields() {
        when(cashAccountRepository.findLookup(eq("main"), any(PageRequest.class)))
                .thenReturn(List.of(cashbox(4L, "Main Cashbox", B2BCurrency.USD, true)));

        var response = service.getCashboxLookup("main");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getCurrencyCode()).isEqualTo("USD");
        assertThat(response.get(0).getCurrencyName()).isEqualTo("US Dollar");
    }

    @Test
    void shouldSoftDeleteCashboxBySettingInactive() {
        CashAccount account = cashbox(9L, "Obsolete", B2BCurrency.EUR, true);
        when(cashAccountRepository.findById(9L)).thenReturn(Optional.of(account));
        when(cashAccountRepository.save(any(CashAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteCashbox(9L);

        assertThat(account.getActive()).isFalse();
        assertThat(account.getUpdatedBy()).isEqualTo("staff");
        verify(cashAccountRepository).save(account);
    }

    private CashAccount cashbox(Long id, String name, B2BCurrency currency, boolean active) {
        CashAccount account = new CashAccount();
        account.setId(id);
        account.setName(name);
        account.setCurrency(currency);
        account.setActive(active);
        account.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        account.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        return account;
    }
}
