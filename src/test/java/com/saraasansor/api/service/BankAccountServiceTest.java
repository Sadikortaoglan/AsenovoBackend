package com.saraasansor.api.service;

import com.saraasansor.api.dto.BankCreateRequest;
import com.saraasansor.api.dto.BankPageResponse;
import com.saraasansor.api.dto.BankResponse;
import com.saraasansor.api.dto.BankUpdateRequest;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.BankAccount;
import com.saraasansor.api.repository.BankAccountRepository;
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
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private BankAccountService service;

    @BeforeEach
    void setUp() {
        service = new BankAccountService(bankAccountRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateBankWithExistingCurrencyModel() {
        BankCreateRequest request = new BankCreateRequest();
        request.setName("Akbank");
        request.setBranchName("Kadikoy");
        request.setAccountNumber("12345");
        request.setIban("TR12 0006 2000 0000 0000 0000 00");
        request.setCurrencyCode("USD");
        request.setActive(true);

        when(bankAccountRepository.existsByNameIgnoreCase("Akbank")).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount bank = invocation.getArgument(0);
            bank.setId(6L);
            bank.setCreatedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
            bank.setUpdatedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
            return bank;
        });

        BankResponse response = service.createBank(request);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(B2BCurrency.USD);
        assertThat(captor.getValue().getIban()).isEqualTo("TR12000620000000000000000000");
        assertThat(response.getCurrencyCode()).isEqualTo("USD");
        assertThat(response.getCurrencyName()).isEqualTo("US Dollar");
    }

    @Test
    void shouldRejectInvalidIbanFormat() {
        BankCreateRequest request = new BankCreateRequest();
        request.setName("Akbank");
        request.setCurrencyCode("TRY");
        request.setIban("123-invalid");

        assertThatThrownBy(() -> service.createBank(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid iban format");
    }

    @Test
    void shouldListBanksWithSearch() {
        when(bankAccountRepository.search(eq("ak"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bank(1L, "Akbank", "TR11", B2BCurrency.TRY, true))));

        BankPageResponse response = service.getBanks("ak", true, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Akbank");
        assertThat(response.getContent().get(0).getCurrencyCode()).isEqualTo("TRY");
    }

    @Test
    void shouldUpdateBank() {
        BankUpdateRequest request = new BankUpdateRequest();
        request.setName("Yapi Kredi");
        request.setBranchName("Besiktas");
        request.setAccountNumber("9988");
        request.setIban("TR220001100000000000000001");
        request.setCurrencyCode("EUR");
        request.setActive(false);

        BankAccount existing = bank(5L, "Old", "TR99", B2BCurrency.TRY, true);
        when(bankAccountRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bankAccountRepository.existsByNameIgnoreCaseAndIdNot("Yapi Kredi", 5L)).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankResponse response = service.updateBank(5L, request);

        assertThat(response.getName()).isEqualTo("Yapi Kredi");
        assertThat(response.getCurrencyCode()).isEqualTo("EUR");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void shouldSoftDeleteBank() {
        BankAccount existing = bank(8L, "Isbank", "TR55", B2BCurrency.GBP, true);
        when(bankAccountRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteBank(8L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    private BankAccount bank(Long id, String name, String iban, B2BCurrency currency, boolean active) {
        BankAccount bank = new BankAccount();
        bank.setId(id);
        bank.setName(name);
        bank.setIban(iban);
        bank.setCurrency(currency);
        bank.setActive(active);
        bank.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        bank.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        return bank;
    }
}
