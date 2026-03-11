package com.saraasansor.api.revisionstandards.service;

import com.saraasansor.api.revisionstandards.dto.RevisionStandardImportResponse;
import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardAdminRepository;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardsRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RevisionStandardImportService {

    private static final Logger log = LoggerFactory.getLogger(RevisionStandardImportService.class);
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\s*(.*)$");
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{3})*,\\d{2}\\b");
    private static final Pattern NOISE_PATTERN = Pattern.compile("\\b(Duzenle|Sil|Fiyat|Islem)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<String> RESOURCE_PATTERNS = List.of(
            "classpath*:standards/*.pdf",
            "classpath*:pdf/*.pdf",
            "classpath*:*.pdf"
    );
    private static final Set<String> TAG_COLORS = Set.of("MAVI", "KIRMIZI", "SARI", "YESIL", "TURUNCU", "MOR", "GRI");

    private final RevisionStandardsRepository revisionStandardsRepository;
    private final RevisionStandardAdminRepository revisionStandardAdminRepository;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public RevisionStandardImportService(RevisionStandardsRepository revisionStandardsRepository,
                                        RevisionStandardAdminRepository revisionStandardAdminRepository) {
        this.revisionStandardsRepository = revisionStandardsRepository;
        this.revisionStandardAdminRepository = revisionStandardAdminRepository;
    }

    public RevisionStandardImportResponse importFromClasspath() {
        RevisionStandardImportResponse response = new RevisionStandardImportResponse();
        Resource[] resources = loadPdfResources();

        for (Resource resource : resources) {
            processResource(resource, response);
        }

        return response;
    }

    private Resource[] loadPdfResources() {
        try {
            Map<String, Resource> resources = new LinkedHashMap<>();
            for (String pattern : RESOURCE_PATTERNS) {
                for (Resource resource : resourceResolver.getResources(pattern)) {
                    String filename = resource.getFilename();
                    if (filename != null && !resources.containsKey(filename)) {
                        resources.put(filename, resource);
                    }
                }
            }
            return resources.values().toArray(Resource[]::new);
        } catch (IOException ex) {
            throw new IllegalStateException("Standart PDF dosyalari okunamadi", ex);
        }
    }

    private void processResource(Resource resource, RevisionStandardImportResponse response) {
        String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.pdf";
        String standardCode = inferStandardCode(filename);
        String sourceVersion = inferSourceVersion(filename);

        try {
            ParseResult parseResult = parseArticles(resource, standardCode, filename, sourceVersion);
            int inserted = 0;
            int updated = 0;

            log.info("Importing file={} standardCode={}", filename, standardCode);
            revisionStandardAdminRepository.ensureStandardSetExists(standardCode);

            for (RevisionStandard standard : parseResult.articles()) {
                Optional<Long> existingId = revisionStandardsRepository.findIdByStandardAndArticleNo(
                        standard.getStandardCode(),
                        standard.getArticleNo()
                );
                if (existingId.isPresent()) {
                    updated += revisionStandardsRepository.update(standard);
                } else {
                    inserted += revisionStandardsRepository.insert(standard);
                }
            }

            response.incrementFilesProcessed();
            response.addParsed(parseResult.articles().size());
            response.addInserted(inserted);
            response.addUpdated(updated);

            log.info("Imported file={} parsed={} inserted={} updated={} skipped={}",
                    filename,
                    parseResult.articles().size(),
                    inserted,
                    updated,
                    parseResult.skippedRows());
        } catch (Exception ex) {
            String error = "Import failed for " + filename + ": " + ex.getMessage();
            response.addError(error);
            log.error("Import error file={}", filename, ex);
        }
    }

    private ParseResult parseArticles(Resource resource, String standardCode, String sourceFileName, String sourceVersion) throws IOException {
        try (InputStream inputStream = resource.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return extractArticles(text, standardCode, sourceFileName, sourceVersion);
        }
    }

    ParseResult extractArticles(String text, String standardCode, String sourceFileName, String sourceVersion) {
        List<RevisionStandard> articles = new ArrayList<>();
        String currentArticleNo = null;
        StringBuilder currentContent = new StringBuilder();
        int skippedRows = 0;

        for (String rawLine : text.split("\\R")) {
            String normalizedLine = normalizeWhitespace(rawLine);
            if (!StringUtils.hasText(normalizedLine) || isHeaderLine(normalizedLine)) {
                continue;
            }

            Matcher matcher = ARTICLE_PATTERN.matcher(normalizedLine);
            if (matcher.matches()) {
                if (currentArticleNo != null) {
                    RevisionStandard article = buildArticle(currentArticleNo, currentContent.toString(), standardCode, sourceFileName, sourceVersion);
                    if (article != null) {
                        articles.add(article);
                    } else {
                        skippedRows++;
                    }
                }

                currentArticleNo = matcher.group(1);
                currentContent = new StringBuilder();

                String firstChunk = sanitizeLineContent(matcher.group(2));
                if (StringUtils.hasText(firstChunk)) {
                    currentContent.append(firstChunk);
                }
                continue;
            }

            if (currentArticleNo != null) {
                String sanitized = sanitizeLineContent(normalizedLine);
                if (StringUtils.hasText(sanitized)) {
                    if (currentContent.length() > 0) {
                        currentContent.append(' ');
                    }
                    currentContent.append(sanitized);
                }
            }
        }

        if (currentArticleNo != null) {
            RevisionStandard article = buildArticle(currentArticleNo, currentContent.toString(), standardCode, sourceFileName, sourceVersion);
            if (article != null) {
                articles.add(article);
            } else {
                skippedRows++;
            }
        }

        return new ParseResult(articles, skippedRows);
    }

    private RevisionStandard buildArticle(String articleNo, String rawContent, String standardCode, String sourceFileName, String sourceVersion) {
        ParsedContent parsedContent = extractTagColorAndDescription(rawContent);
        if (!StringUtils.hasText(parsedContent.description())) {
            return null;
        }

        RevisionStandard standard = new RevisionStandard();
        standard.setStandardCode(standardCode);
        standard.setArticleNo(articleNo);
        standard.setDescription(parsedContent.description());
        standard.setTagColor(parsedContent.tagColor());
        standard.setPrice(BigDecimal.ZERO);
        standard.setSourceFileName(sourceFileName);
        standard.setSourceVersion(sourceVersion);
        return standard;
    }

    private ParsedContent extractTagColorAndDescription(String rawContent) {
        String cleaned = sanitizeLineContent(rawContent);
        String tagColor = null;
        List<String> descriptionTokens = new ArrayList<>();

        for (String token : cleaned.split(" ")) {
            String normalizedToken = normalizeToken(token);
            if (TAG_COLORS.contains(normalizedToken)) {
                tagColor = normalizedToken;
                continue;
            }
            if (StringUtils.hasText(token)) {
                descriptionTokens.add(token);
            }
        }

        cleaned = normalizeWhitespace(String.join(" ", descriptionTokens));
        return new ParsedContent(cleaned, tagColor);
    }

    private boolean isHeaderLine(String line) {
        String normalized = normalizeToken(line.replace(" ", ""));
        return normalized.contains("MADDEACIKLAMAETIKETFIYATISLEM");
    }

    private String sanitizeLineContent(String line) {
        String cleaned = normalizeWhitespace(line);
        cleaned = PRICE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = NOISE_PATTERN.matcher(cleaned).replaceAll(" ");
        return normalizeWhitespace(cleaned);
    }

    private String inferStandardCode(String filename) {
        String baseName = filename.replaceFirst("(?i)\\.pdf$", "");
        baseName = baseName.replaceAll("(?i)\\bSTANDART\\b", "").trim();
        String[] parts = baseName.split("[_\\s]+");
        List<String> normalized = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i >= 2 && i + 1 < parts.length && isNumeric(part) && isNumeric(parts[i + 1])) {
                String combined = part + "-" + parts[i + 1];
                if (i + 2 < parts.length && parts[i + 2].matches("(?i)A\\d+")) {
                    combined += "+" + parts[i + 2].toUpperCase();
                    i++;
                }
                normalized.add(combined);
                i++;
                continue;
            }
            normalized.add(part.toUpperCase());
        }

        return String.join(" ", normalized).trim();
    }

    private String inferSourceVersion(String filename) {
        String baseName = filename.replaceFirst("(?i)\\.pdf$", "");
        String[] parts = baseName.split("[_\\s]+");
        String lastPart = parts[parts.length - 1];
        if (lastPart.matches("(?i)A\\d+")) {
            return lastPart.toUpperCase();
        }
        return null;
    }

    private boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private String normalizeToken(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toUpperCase();
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    record ParsedContent(String description, String tagColor) {
    }

    record ParseResult(List<RevisionStandard> articles, int skippedRows) {
    }
}
