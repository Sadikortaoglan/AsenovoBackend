package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitReportData;
import com.saraasansor.api.dto.B2BUnitReportRow;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.dto.TenantBrandingResponseDTO;
import com.saraasansor.api.tenant.service.TenantBrandingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class B2BUnitReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final B2BUnitService b2bUnitService;
    private final B2BUnitTransactionRepository transactionRepository;
    private final TenantBrandingService tenantBrandingService;

    public B2BUnitReportService(B2BUnitService b2bUnitService,
                                B2BUnitTransactionRepository transactionRepository,
                                TenantBrandingService tenantBrandingService) {
        this.b2bUnitService = b2bUnitService;
        this.transactionRepository = transactionRepository;
        this.tenantBrandingService = tenantBrandingService;
    }

    public B2BUnitReportData getReportData(Long b2bUnitId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        B2BUnit b2bUnit = b2bUnitService.getB2BUnitById(b2bUnitId);

        BigDecimal openingBalance = transactionRepository
                .findTopByB2bUnitIdAndTransactionDateLessThanOrderByTransactionDateDescIdDesc(b2bUnitId, startDate)
                .map(B2BUnitTransaction::getBalanceAfterTransaction)
                .orElse(BigDecimal.ZERO);

        List<B2BUnitTransaction> inRangeTransactions = transactionRepository
                .findByB2bUnitIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(b2bUnitId, startDate, endDate);

        BigDecimal closingBalance = transactionRepository
                .findTopByB2bUnitIdAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(b2bUnitId, endDate)
                .map(B2BUnitTransaction::getBalanceAfterTransaction)
                .orElse(openingBalance);

        B2BUnitReportData reportData = new B2BUnitReportData();
        reportData.setReportType("B2BUnit Statement");
        reportData.setReportCreatedAt(LocalDateTime.now());
        reportData.setPeriodStart(startDate);
        reportData.setPeriodEnd(endDate);
        reportData.setB2bUnitName(b2bUnit.getName());
        reportData.setB2bUnitAddress(b2bUnit.getAddress());
        reportData.setB2bUnitPhone(b2bUnit.getPhone());
        reportData.setOpeningBalance(scale(openingBalance));
        reportData.setClosingBalance(scale(closingBalance));
        reportData.setBalanceLabel(resolveBalanceLabel(closingBalance));

        TenantBrandingResponseDTO branding = resolveTenantBranding();
        reportData.setCompanyName(resolveCompanyName(branding));
        reportData.setCompanyLogoUrl(branding != null ? branding.getLogoUrl() : null);

        reportData.setRows(inRangeTransactions.stream().map(this::toRow).toList());
        return reportData;
    }

    public String buildReportHtml(Long b2bUnitId, LocalDate startDate, LocalDate endDate) {
        B2BUnitReportData data = getReportData(b2bUnitId, startDate, endDate);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>")
                .append("<title>B2BUnit Report</title>")
                .append("<style>")
                .append("@page{size:A4 portrait;margin:12mm;} ")
                .append("body{font-family:Arial,sans-serif;color:#1f2937;margin:0;padding:0;} ")
                .append(".container{padding:14px;} ")
                .append(".header{display:flex;justify-content:space-between;align-items:center;border-bottom:2px solid #e5e7eb;padding-bottom:12px;margin-bottom:14px;} ")
                .append(".logo{height:54px;max-width:180px;object-fit:contain;} ")
                .append(".company{font-size:22px;font-weight:700;} ")
                .append(".blocks{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:10px;} ")
                .append(".block{border:1px solid #e5e7eb;border-radius:6px;padding:10px;} ")
                .append(".title{font-size:12px;color:#6b7280;margin-bottom:8px;text-transform:uppercase;font-weight:700;} ")
                .append(".row{display:flex;justify-content:space-between;gap:10px;font-size:13px;margin-bottom:6px;} ")
                .append(".label{font-weight:700;} ")
                .append(".summary{border:1px solid #111827;background:#f8fafc;border-radius:6px;padding:8px 10px;margin-bottom:12px;display:flex;justify-content:space-between;} ")
                .append("table{width:100%;border-collapse:collapse;font-size:12px;} ")
                .append("th,td{border:1px solid #d1d5db;padding:7px;text-align:left;} ")
                .append("th{background:#f3f4f6;font-weight:700;} ")
                .append(".right{text-align:right;} ")
                .append(".muted{color:#6b7280;} ")
                .append(".bold{font-weight:700;} ")
                .append("</style></head><body><div class=\"container\">");

        html.append("<div class=\"header\">")
                .append("<div>");
        if (StringUtils.hasText(data.getCompanyLogoUrl())) {
            html.append("<img class=\"logo\" src=\"").append(escapeHtml(data.getCompanyLogoUrl())).append("\" alt=\"logo\"/> ");
        }
        html.append("<div class=\"company\">").append(escapeHtml(data.getCompanyName())).append("</div>")
                .append("</div>")
                .append("<div class=\"muted\">RAPOR TARİHİ: ")
                .append(escapeHtml(formatDateTime(data.getReportCreatedAt())))
                .append("</div>")
                .append("</div>");

        html.append("<div class=\"blocks\">")
                .append("<div class=\"block\">")
                .append("<div class=\"title\">B2BUnit Bilgileri</div>")
                .append(infoRow("CARİ ADI", data.getB2bUnitName()))
                .append(infoRow("ADRES", data.getB2bUnitAddress()))
                .append(infoRow("TELEFON NUMARASI", data.getB2bUnitPhone()))
                .append("</div>")
                .append("<div class=\"block\">")
                .append("<div class=\"title\">Rapor Bilgileri</div>")
                .append(infoRow("RAPOR TÜRÜ", data.getReportType()))
                .append(infoRow("TARİH ARALIĞI", formatDate(data.getPeriodStart()) + " - " + formatDate(data.getPeriodEnd())))
                .append(infoRow("RAPOR TARİHİ", formatDateTime(data.getReportCreatedAt())))
                .append("</div>")
                .append("</div>");

        html.append("<div class=\"summary\">")
                .append("<div><span class=\"label\">BAKİYE:</span> ")
                .append(escapeHtml(formatMoney(data.getClosingBalance())))
                .append("</div>")
                .append("<div class=\"bold\">" + escapeHtml(data.getBalanceLabel()) + "</div>")
                .append("</div>");

        html.append("<table><thead><tr>")
                .append("<th>TARİH</th>")
                .append("<th>İŞLEM TİPİ</th>")
                .append("<th class=\"right\">BORÇ</th>")
                .append("<th class=\"right\">ALACAK</th>")
                .append("<th class=\"right\">KALAN</th>")
                .append("</tr></thead><tbody>");

        html.append("<tr>")
                .append(td(formatDate(data.getPeriodStart())))
                .append(td("Tarihinden Önceki Bakiye"))
                .append(tdRight("-"))
                .append(tdRight("-"))
                .append(tdRight(formatMoney(data.getOpeningBalance())))
                .append("</tr>");

        for (B2BUnitReportRow row : data.getRows()) {
            html.append("<tr>")
                    .append(td(formatDate(row.getTransactionDate())))
                    .append(td(row.getTransactionTypeLabel()))
                    .append(tdRight(formatMoney(row.getDebitAmount())))
                    .append(tdRight(formatMoney(row.getCreditAmount())))
                    .append(tdRight(formatMoney(row.getBalanceAfterTransaction())))
                    .append("</tr>");
        }

        if (data.getRows().isEmpty()) {
            html.append("<tr><td colspan=\"5\" class=\"muted\">Kayıt bulunamadı</td></tr>");
        }

        html.append("<tr>")
                .append(td(formatDate(data.getPeriodEnd())))
                .append(td("Tarihinden Sonraki Bakiye"))
                .append(tdRight("-"))
                .append(tdRight("-"))
                .append(tdRight(formatMoney(data.getClosingBalance())))
                .append("</tr>");

        html.append("</tbody></table>")
                .append("</div></body></html>");

        return html.toString();
    }

    private B2BUnitReportRow toRow(B2BUnitTransaction transaction) {
        B2BUnitReportRow row = new B2BUnitReportRow();
        row.setTransactionDate(transaction.getTransactionDate());
        row.setTransactionTypeLabel(resolveTransactionTypeLabel(transaction.getTransactionType()));
        row.setDebitAmount(scale(transaction.getDebitAmount()));
        row.setCreditAmount(scale(transaction.getCreditAmount()));
        row.setBalanceAfterTransaction(scale(transaction.getBalanceAfterTransaction()));
        return row;
    }

    private String resolveTransactionTypeLabel(B2BUnitTransaction.TransactionType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case PURCHASE -> "Alış Fatura";
            case SALE -> "Satış Fatura";
            case COLLECTION -> "Tahsilat";
            case PAYMENT -> "Ödeme";
            case CASH_COLLECTION -> "Nakit Tahsilat";
            case PAYTR_COLLECTION -> "PayTR Tahsilat";
            case CREDIT_CARD_COLLECTION -> "Kredi Kartı Tahsilat";
            case BANK_COLLECTION -> "Banka Tahsilat";
            case CHECK_COLLECTION -> "Çek Tahsilat";
            case PROMISSORY_NOTE_COLLECTION -> "Senet Tahsilat";
            case CASH_PAYMENT -> "Nakit Ödeme";
            case CREDIT_CARD_PAYMENT -> "Kredi Kartı Ödeme";
            case BANK_PAYMENT -> "Banka Ödeme";
            case CHECK_PAYMENT -> "Çek Ödeme";
            case PROMISSORY_NOTE_PAYMENT -> "Senet Ödeme";
            case MANUAL_DEBIT -> "Cari Borçlandır";
            case MANUAL_CREDIT -> "Cari Alacaklandır";
            case OPENING_BALANCE -> "Açılış Bakiyesi";
        };
    }

    private String resolveBalanceLabel(BigDecimal balance) {
        BigDecimal normalized = scale(balance);
        if (normalized.compareTo(BigDecimal.ZERO) > 0) {
            return "Borçlu";
        }
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return "Alacaklı";
        }
        return "Hesap Yok";
    }

    private TenantBrandingResponseDTO resolveTenantBranding() {
        try {
            return tenantBrandingService.getCurrentTenantBranding();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveCompanyName(TenantBrandingResponseDTO branding) {
        if (branding != null && StringUtils.hasText(branding.getName())) {
            return branding.getName().trim();
        }
        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant != null && StringUtils.hasText(tenant.getName())) {
            return tenant.getName().trim();
        }
        return "Asenovo";
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new RuntimeException("startDate is required");
        }
        if (endDate == null) {
            throw new RuntimeException("endDate is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate cannot be after endDate");
        }
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String td(String value) {
        return "<td>" + escapeHtml(value) + "</td>";
    }

    private String tdRight(String value) {
        return "<td class=\"right\">" + escapeHtml(value) + "</td>";
    }

    private String infoRow(String label, String value) {
        return "<div class=\"row\"><span class=\"label\">" + escapeHtml(label)
                + "</span><span>" + escapeHtml(value) + "</span></div>";
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMAT);
    }

    private String formatMoney(BigDecimal amount) {
        return scale(amount).toPlainString();
    }

    private String escapeHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return escaped.trim();
    }
}
