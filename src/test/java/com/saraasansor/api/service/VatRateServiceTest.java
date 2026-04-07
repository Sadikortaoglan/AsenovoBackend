package com.saraasansor.api.service;

import com.saraasansor.api.dto.VatRateCreateRequest;
import com.saraasansor.api.dto.VatRatePageResponse;
import com.saraasansor.api.dto.VatRateResponse;
import com.saraasansor.api.dto.VatRateUpdateRequest;
import com.saraasansor.api.model.VatRate;
import com.saraasansor.api.repository.VatRateRepository;
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

import java.math.BigDecimal;
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
class VatRateServiceTest {

    @Mock
    private VatRateRepository vatRateRepository;

    private VatRateService service;

    @BeforeEach
    void setUp() {
        service = new VatRateService(vatRateRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff", "x", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateVatRate() {
        VatRateCreateRequest request = new VatRateCreateRequest();
        request.setRate(new BigDecimal("20"));
        request.setActive(true);

        when(vatRateRepository.existsByRate(new BigDecimal("20.00"))).thenReturn(false);
        when(vatRateRepository.save(any(VatRate.class))).thenAnswer(invocation -> {
            VatRate vatRate = invocation.getArgument(0);
            vatRate.setId(5L);
            vatRate.setCreatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            vatRate.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 10, 0));
            return vatRate;
        });

        VatRateResponse response = service.createVatRate(request);

        ArgumentCaptor<VatRate> captor = ArgumentCaptor.forClass(VatRate.class);
        verify(vatRateRepository).save(captor.capture());
        assertThat(captor.getValue().getRate()).isEqualByComparingTo("20.00");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("staff");
        assertThat(response.getRate()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldUpdateVatRate() {
        VatRateUpdateRequest request = new VatRateUpdateRequest();
        request.setRate(new BigDecimal("10"));
        request.setActive(false);

        VatRate existing = vatRate(3L, "8.00", true);
        when(vatRateRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(vatRateRepository.existsByRateAndIdNot(new BigDecimal("10.00"), 3L)).thenReturn(false);
        when(vatRateRepository.save(any(VatRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VatRateResponse response = service.updateVatRate(3L, request);

        assertThat(response.getRate()).isEqualByComparingTo("10.00");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void shouldListVatRatesOrdered() {
        when(vatRateRepository.search(eq(""), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        vatRate(1L, "1.00", true),
                        vatRate(2L, "20.00", true)
                )));

        VatRatePageResponse response = service.getVatRates("", null, PageRequest.of(0, 25));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getRate()).isEqualByComparingTo("1.00");
        assertThat(response.getContent().get(1).getRate()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldSoftDeleteVatRate() {
        VatRate existing = vatRate(10L, "20.00", true);
        when(vatRateRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(vatRateRepository.save(any(VatRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteVatRate(10L);

        assertThat(existing.getActive()).isFalse();
        assertThat(existing.getUpdatedBy()).isEqualTo("staff");
    }

    @Test
    void shouldRejectNegativeVatRate() {
        VatRateCreateRequest request = new VatRateCreateRequest();
        request.setRate(new BigDecimal("-1"));

        assertThatThrownBy(() -> service.createVatRate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("rate cannot be negative");
    }

    @Test
    void shouldRejectVatRateOverHundred() {
        VatRateCreateRequest request = new VatRateCreateRequest();
        request.setRate(new BigDecimal("120"));

        assertThatThrownBy(() -> service.createVatRate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("less than or equal to 100");
    }

    @Test
    void shouldRejectDuplicateVatRate() {
        VatRateCreateRequest request = new VatRateCreateRequest();
        request.setRate(new BigDecimal("20"));

        when(vatRateRepository.existsByRate(new BigDecimal("20.00"))).thenReturn(true);

        assertThatThrownBy(() -> service.createVatRate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    private VatRate vatRate(Long id, String rate, boolean active) {
        VatRate vatRate = new VatRate();
        vatRate.setId(id);
        vatRate.setRate(new BigDecimal(rate));
        vatRate.setActive(active);
        vatRate.setCreatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        vatRate.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 9, 0));
        return vatRate;
    }
}
