package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitReportData;
import com.saraasansor.api.service.B2BUnitReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitReportController {

    private final B2BUnitReportService reportService;

    public B2BUnitReportController(B2BUnitReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/{id}/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReportHtml(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(reportService.buildReportHtml(id, startDate, endDate));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtmlErrorPage("Forbidden", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildHtmlErrorPage("Report Error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/report-data")
    public ResponseEntity<ApiResponse<B2BUnitReportData>> getReportData(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReportData(id, startDate, endDate)));
    }

    private String buildHtmlErrorPage(String title, String message) {
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>"
                + "<title>" + safeTitle + "</title>"
                + "<style>body{font-family:Arial,sans-serif;padding:24px;color:#1f2937;}"
                + "h2{margin:0 0 8px 0;}p{margin:0;color:#6b7280;}</style></head>"
                + "<body><h2>" + safeTitle + "</h2><p>" + safeMessage + "</p></body></html>";
    }

    private String escapeHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
