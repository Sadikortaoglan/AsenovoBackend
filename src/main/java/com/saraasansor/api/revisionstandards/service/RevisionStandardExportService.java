package com.saraasansor.api.revisionstandards.service;

import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import com.saraasansor.api.revisionstandards.model.RevisionStandardSet;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardAdminRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

@Service
public class RevisionStandardExportService {

    private static final String FORMAT_CSV = "csv";
    private static final String FORMAT_XLSX = "xlsx";
    private static final String FORMAT_PDF = "pdf";
    private final RevisionStandardAdminRepository revisionStandardAdminRepository;

    public RevisionStandardExportService(RevisionStandardAdminRepository revisionStandardAdminRepository) {
        this.revisionStandardAdminRepository = revisionStandardAdminRepository;
    }

    public ExportFile exportStandards(String query, String format) {
        String normalizedFormat = normalizeFormat(format);
        List<RevisionStandardSet> rows = revisionStandardAdminRepository.findStandardSets(query);
        return switch (normalizedFormat) {
            case FORMAT_CSV -> new ExportFile(
                    buildStandardsCsv(rows),
                    "text/csv; charset=UTF-8",
                    "revision-standards.csv"
            );
            case FORMAT_XLSX -> new ExportFile(
                    buildStandardsXlsx(rows),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "revision-standards.xlsx"
            );
            case FORMAT_PDF -> new ExportFile(
                    buildStandardsPdf(rows),
                    "application/pdf",
                    "revision-standards.pdf"
            );
            default -> throw new RuntimeException("Desteklenmeyen export formatı");
        };
    }

    public ExportFile exportArticles(Long standardId,
                                     String query,
                                     String tagColor,
                                     BigDecimal minPrice,
                                     BigDecimal maxPrice,
                                     String format) {
        String normalizedFormat = normalizeFormat(format);
        List<RevisionStandard> rows = revisionStandardAdminRepository.findArticlesByStandardSetId(
                standardId, query, normalizeNullable(tagColor), minPrice, maxPrice
        );
        return switch (normalizedFormat) {
            case FORMAT_CSV -> new ExportFile(
                    buildArticlesCsv(rows),
                    "text/csv; charset=UTF-8",
                    "revision-standard-articles-" + standardId + ".csv"
            );
            case FORMAT_XLSX -> new ExportFile(
                    buildArticlesXlsx(rows),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "revision-standard-articles-" + standardId + ".xlsx"
            );
            case FORMAT_PDF -> new ExportFile(
                    buildArticlesPdf(rows),
                    "application/pdf",
                    "revision-standard-articles-" + standardId + ".pdf"
            );
            default -> throw new RuntimeException("Desteklenmeyen export formatı");
        };
    }

    private byte[] buildStandardsCsv(List<RevisionStandardSet> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append("Standart Kodu,Madde Sayisi\n");
        for (RevisionStandardSet row : rows) {
            builder.append(csv(row.getStandardCode())).append(',')
                    .append(row.getArticleCount() == null ? 0 : row.getArticleCount())
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildArticlesCsv(List<RevisionStandard> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append("Madde No,Aciklama,Etiket,Fiyat\n");
        for (RevisionStandard row : rows) {
            builder.append(csv(row.getArticleNo())).append(',')
                    .append(csv(row.getDescription())).append(',')
                    .append(csv(row.getTagColor())).append(',')
                    .append(csv(formatPrice(row.getPrice())))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildStandardsXlsx(List<RevisionStandardSet> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Standartlar");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Standart Kodu");
            header.createCell(1).setCellValue("Madde Sayisi");
            for (int i = 0; i < rows.size(); i++) {
                RevisionStandardSet item = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.getStandardCode());
                row.createCell(1).setCellValue(item.getArticleCount() == null ? 0 : item.getArticleCount());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Excel export olusturulamadi", ex);
        }
    }

    private byte[] buildArticlesXlsx(List<RevisionStandard> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Maddeler");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Madde No");
            header.createCell(1).setCellValue("Aciklama");
            header.createCell(2).setCellValue("Etiket");
            header.createCell(3).setCellValue("Fiyat");
            for (int i = 0; i < rows.size(); i++) {
                RevisionStandard item = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(nullSafe(item.getArticleNo()));
                row.createCell(1).setCellValue(nullSafe(item.getDescription()));
                row.createCell(2).setCellValue(nullSafe(item.getTagColor()));
                row.createCell(3).setCellValue(formatPrice(item.getPrice()));
            }
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Excel export olusturulamadi", ex);
        }
    }

    private byte[] buildStandardsPdf(List<RevisionStandardSet> rows) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writePdfHeader(content, "Revision Standards");
                float y = 760;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                content.newLineAtOffset(50, y);
                content.showText("Standart Kodu | Madde Sayisi");
                content.endText();
                y -= 20;
                for (RevisionStandardSet row : rows) {
                    if (y < 50) {
                        break;
                    }
                    writePdfLine(content, y, sanitizePdfText(row.getStandardCode()) + " | " + (row.getArticleCount() == null ? 0 : row.getArticleCount()));
                    y -= 16;
                }
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("PDF export olusturulamadi", ex);
        }
    }

    private byte[] buildArticlesPdf(List<RevisionStandard> rows) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writePdfHeader(content, "Revision Standard Articles");
                float y = 520;
                writePdfLine(content, y, "Madde No | Etiket | Fiyat | Aciklama");
                y -= 20;
                for (RevisionStandard row : rows) {
                    if (y < 40) {
                        break;
                    }
                    String line = "%s | %s | %s | %s".formatted(
                            sanitizePdfText(row.getArticleNo()),
                            sanitizePdfText(row.getTagColor()),
                            sanitizePdfText(formatPrice(row.getPrice())),
                            sanitizePdfText(truncate(row.getDescription(), 90))
                    );
                    writePdfLine(content, y, line);
                    y -= 16;
                }
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("PDF export olusturulamadi", ex);
        }
    }

    private void writePdfHeader(PDPageContentStream content, String title) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 14);
        content.newLineAtOffset(50, 800);
        content.showText(title);
        content.endText();
    }

    private void writePdfLine(PDPageContentStream content, float y, String text) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 10);
        content.newLineAtOffset(50, y);
        content.showText(text);
        content.endText();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String normalizeFormat(String format) {
        String normalized = format == null ? FORMAT_CSV : format.trim().toLowerCase(Locale.ROOT);
        if (FORMAT_CSV.equals(normalized) || FORMAT_XLSX.equals(normalized) || FORMAT_PDF.equals(normalized)) {
            return normalized;
        }
        throw new RuntimeException("Desteklenmeyen export formatı");
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String formatPrice(BigDecimal price) {
        BigDecimal safe = price == null ? BigDecimal.ZERO : price;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("tr", "TR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(safe);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return nullSafe(value);
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String sanitizePdfText(String value) {
        return nullSafe(value)
                .replace("ı", "i")
                .replace("İ", "I")
                .replace("ğ", "g")
                .replace("Ğ", "G")
                .replace("ü", "u")
                .replace("Ü", "U")
                .replace("ş", "s")
                .replace("Ş", "S")
                .replace("ö", "o")
                .replace("Ö", "O")
                .replace("ç", "c")
                .replace("Ç", "C");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record ExportFile(byte[] content, String contentType, String fileName) {
    }
}
