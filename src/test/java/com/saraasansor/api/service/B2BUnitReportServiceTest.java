package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitReportData;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.tenant.dto.TenantBrandingResponseDTO;
import com.saraasansor.api.tenant.service.TenantBrandingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2BUnitReportServiceTest {

    @Mock
    private B2BUnitService b2bUnitService;

    @Mock
    private B2BUnitTransactionRepository transactionRepository;

    @Mock
    private TenantBrandingService tenantBrandingService;

    @Test
    void shouldGenerateReportDataSuccessfully() {
        B2BUnitReportService service = new B2BUnitReportService(b2bUnitService, transactionRepository, tenantBrandingService);

        B2BUnit unit = new B2BUnit();
        unit.setId(5L);
        unit.setName("Acme Cari");
        unit.setAddress("Istanbul");
        unit.setPhone("+90 555 111 22 33");

        when(b2bUnitService.getB2BUnitById(5L)).thenReturn(unit);
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanOrderByTransactionDateDescIdDesc(5L, LocalDate.of(2026, 3, 1)))
                .thenReturn(Optional.of(tx(LocalDate.of(2026, 2, 20), B2BUnitTransaction.TransactionType.MANUAL_DEBIT, "100.00", "0.00", "100.00")));
        when(transactionRepository.findByB2bUnitIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(
                        tx(LocalDate.of(2026, 3, 5), B2BUnitTransaction.TransactionType.CASH_COLLECTION, "0.00", "40.00", "60.00"),
                        tx(LocalDate.of(2026, 3, 10), B2BUnitTransaction.TransactionType.CASH_PAYMENT, "20.00", "0.00", "80.00")
                ));
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(5L, LocalDate.of(2026, 3, 31)))
                .thenReturn(Optional.of(tx(LocalDate.of(2026, 3, 10), B2BUnitTransaction.TransactionType.CASH_PAYMENT, "20.00", "0.00", "80.00")));

        TenantBrandingResponseDTO branding = new TenantBrandingResponseDTO();
        branding.setName("Acme Company");
        branding.setLogoUrl("https://cdn.example/logo.png");
        when(tenantBrandingService.getCurrentTenantBranding()).thenReturn(branding);

        B2BUnitReportData data = service.getReportData(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(data.getCompanyName()).isEqualTo("Acme Company");
        assertThat(data.getB2bUnitName()).isEqualTo("Acme Cari");
        assertThat(data.getOpeningBalance()).isEqualByComparingTo("100.00");
        assertThat(data.getClosingBalance()).isEqualByComparingTo("80.00");
        assertThat(data.getBalanceLabel()).isEqualTo("Borçlu");
        assertThat(data.getRows()).hasSize(2);
        assertThat(data.getRows().get(0).getTransactionTypeLabel()).isEqualTo("Nakit Tahsilat");
        assertThat(data.getRows().get(1).getTransactionTypeLabel()).isEqualTo("Nakit Ödeme");
    }

    @Test
    void shouldGenerateEmptyDataReport() {
        B2BUnitReportService service = new B2BUnitReportService(b2bUnitService, transactionRepository, tenantBrandingService);

        B2BUnit unit = new B2BUnit();
        unit.setId(7L);
        unit.setName("No Tx Cari");

        when(b2bUnitService.getB2BUnitById(7L)).thenReturn(unit);
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanOrderByTransactionDateDescIdDesc(7L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByB2bUnitIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of());
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(7L, LocalDate.of(2026, 1, 31)))
                .thenReturn(Optional.empty());
        when(tenantBrandingService.getCurrentTenantBranding()).thenThrow(new RuntimeException("No branding"));

        B2BUnitReportData data = service.getReportData(7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(data.getRows()).isEmpty();
        assertThat(data.getOpeningBalance()).isEqualByComparingTo("0.00");
        assertThat(data.getClosingBalance()).isEqualByComparingTo("0.00");
        assertThat(data.getBalanceLabel()).isEqualTo("Hesap Yok");
    }

    @Test
    void shouldValidateInvalidDateRange() {
        B2BUnitReportService service = new B2BUnitReportService(b2bUnitService, transactionRepository, tenantBrandingService);

        assertThatThrownBy(() -> service.getReportData(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("startDate cannot be after endDate");
    }

    @Test
    void shouldPropagateForbiddenAccess() {
        B2BUnitReportService service = new B2BUnitReportService(b2bUnitService, transactionRepository, tenantBrandingService);

        when(b2bUnitService.getB2BUnitById(5L)).thenThrow(new AccessDeniedException("Forbidden"));

        assertThatThrownBy(() -> service.getReportData(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldBuildPrintableHtmlSuccessfully() {
        B2BUnitReportService service = new B2BUnitReportService(b2bUnitService, transactionRepository, tenantBrandingService);

        B2BUnit unit = new B2BUnit();
        unit.setId(5L);
        unit.setName("Acme Cari");
        when(b2bUnitService.getB2BUnitById(5L)).thenReturn(unit);
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanOrderByTransactionDateDescIdDesc(5L, LocalDate.of(2026, 3, 1)))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByB2bUnitIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of());
        when(transactionRepository.findTopByB2bUnitIdAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(5L, LocalDate.of(2026, 3, 31)))
                .thenReturn(Optional.empty());
        when(tenantBrandingService.getCurrentTenantBranding()).thenThrow(new RuntimeException("No branding"));

        String html = service.buildReportHtml(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(html).contains("CARİ ADI")
                .contains("RAPOR TÜRÜ")
                .contains("TARİH")
                .contains("İŞLEM TİPİ")
                .contains("Tarihinden Önceki Bakiye")
                .contains("Tarihinden Sonraki Bakiye");
    }

    private B2BUnitTransaction tx(LocalDate date,
                                  B2BUnitTransaction.TransactionType type,
                                  String debit,
                                  String credit,
                                  String balance) {
        B2BUnitTransaction transaction = new B2BUnitTransaction();
        transaction.setTransactionDate(date);
        transaction.setTransactionType(type);
        transaction.setDebitAmount(new BigDecimal(debit));
        transaction.setCreditAmount(new BigDecimal(credit));
        transaction.setBalanceAfterTransaction(new BigDecimal(balance));
        return transaction;
    }
}
