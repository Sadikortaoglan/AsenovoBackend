package com.saraasansor.api.revisionstandards.service;

import com.saraasansor.api.revisionstandards.dto.CreateRevisionStandardArticleRequest;
import com.saraasansor.api.revisionstandards.dto.CreateRevisionStandardSetRequest;
import com.saraasansor.api.revisionstandards.dto.RevisionStandardArticleResponse;
import com.saraasansor.api.revisionstandards.dto.RevisionStandardSetResponse;
import com.saraasansor.api.revisionstandards.dto.UpdateRevisionStandardArticleRequest;
import com.saraasansor.api.revisionstandards.dto.UpdateRevisionStandardSetRequest;
import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import com.saraasansor.api.revisionstandards.model.RevisionStandardSet;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardAdminRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class RevisionStandardAdminService {

    private final RevisionStandardAdminRepository revisionStandardAdminRepository;

    public RevisionStandardAdminService(RevisionStandardAdminRepository revisionStandardAdminRepository) {
        this.revisionStandardAdminRepository = revisionStandardAdminRepository;
    }

    @Transactional(readOnly = true)
    public Page<RevisionStandardSetResponse> getStandardSets(String query, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(normalizePageNumber(page), normalizePageSize(size));
        List<RevisionStandardSetResponse> content = revisionStandardAdminRepository.findStandardSets(query, pageable).stream()
                .map(this::toSetResponse)
                .toList();
        long total = revisionStandardAdminRepository.countStandardSets(query);
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public RevisionStandardSetResponse getStandardSet(Long id) {
        return revisionStandardAdminRepository.findStandardSetById(id)
                .map(this::toSetResponse)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
    }

    public RevisionStandardSetResponse createStandardSet(CreateRevisionStandardSetRequest request) {
        String code = normalizeRequired(request.getStandardCode());
        try {
            Long id = revisionStandardAdminRepository.createStandardSet(code);
            return new RevisionStandardSetResponse(id, code, 0);
        } catch (DuplicateKeyException ex) {
            throw new RuntimeException("Standart zaten mevcut");
        }
    }

    public RevisionStandardSetResponse updateStandardSet(Long id, UpdateRevisionStandardSetRequest request) {
        RevisionStandardSet existing = revisionStandardAdminRepository.findStandardSetById(id)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
        String newCode = normalizeRequired(request.getStandardCode());
        if (!existing.getStandardCode().equals(newCode) &&
                revisionStandardAdminRepository.findStandardSetByCode(newCode).isPresent()) {
            throw new RuntimeException("Standart adi zaten mevcut");
        }
        revisionStandardAdminRepository.updateStandardSet(id, newCode);
        RevisionStandardSet updated = revisionStandardAdminRepository.findStandardSetById(id)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
        return toSetResponse(updated);
    }

    public void deleteStandardSet(Long id) {
        revisionStandardAdminRepository.findStandardSetById(id)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
        revisionStandardAdminRepository.deleteStandardSet(id);
    }

    @Transactional(readOnly = true)
    public Page<RevisionStandardArticleResponse> getArticles(Long standardSetId,
                                                             String query,
                                                             String tagColor,
                                                             BigDecimal minPrice,
                                                             BigDecimal maxPrice,
                                                             Integer page,
                                                             Integer size) {
        revisionStandardAdminRepository.findStandardSetById(standardSetId)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
        PageRequest pageable = PageRequest.of(normalizePageNumber(page), normalizePageSize(size));
        String normalizedTagColor = normalizeNullable(tagColor);
        validatePriceRange(minPrice, maxPrice);
        List<RevisionStandardArticleResponse> content = revisionStandardAdminRepository
                .findArticlesByStandardSetId(standardSetId, query, normalizedTagColor, minPrice, maxPrice, pageable).stream()
                .map(this::toArticleResponse)
                .toList();
        long total = revisionStandardAdminRepository.countArticlesByStandardSetId(
                standardSetId, query, normalizedTagColor, minPrice, maxPrice
        );
        return new PageImpl<>(content, pageable, total);
    }

    public RevisionStandardArticleResponse createArticle(Long standardSetId, CreateRevisionStandardArticleRequest request) {
        revisionStandardAdminRepository.findStandardSetById(standardSetId)
                .orElseThrow(() -> new RuntimeException("Standart bulunamadi"));
        RevisionStandard article = mapArticle(request.getArticleNo(), request.getDescription(), request.getTagColor(), request.getPrice());
        try {
            Long id = revisionStandardAdminRepository.createArticle(standardSetId, article);
            return revisionStandardAdminRepository.findArticleById(id)
                    .map(this::toArticleResponse)
                    .orElseThrow(() -> new RuntimeException("Madde olusturulamadi"));
        } catch (DuplicateKeyException ex) {
            throw new RuntimeException("Bu standart icin ayni madde zaten mevcut");
        }
    }

    public RevisionStandardArticleResponse updateArticle(Long articleId, UpdateRevisionStandardArticleRequest request) {
        RevisionStandard existing = revisionStandardAdminRepository.findArticleById(articleId)
                .orElseThrow(() -> new RuntimeException("Madde bulunamadi"));
        RevisionStandard article = mapArticle(request.getArticleNo(), request.getDescription(), request.getTagColor(), request.getPrice());
        article.setStandardCode(existing.getStandardCode());
        try {
            revisionStandardAdminRepository.updateArticle(articleId, article);
            return revisionStandardAdminRepository.findArticleById(articleId)
                    .map(this::toArticleResponse)
                    .orElseThrow(() -> new RuntimeException("Madde bulunamadi"));
        } catch (DuplicateKeyException ex) {
            throw new RuntimeException("Bu standart icin ayni madde zaten mevcut");
        }
    }

    public void deleteArticle(Long articleId) {
        revisionStandardAdminRepository.findArticleById(articleId)
                .orElseThrow(() -> new RuntimeException("Madde bulunamadi"));
        revisionStandardAdminRepository.deleteArticle(articleId);
    }

    private RevisionStandardSetResponse toSetResponse(RevisionStandardSet item) {
        long articleCount = item.getArticleCount() == null ? 0 : item.getArticleCount();
        return new RevisionStandardSetResponse(item.getId(), item.getStandardCode(), articleCount);
    }

    private RevisionStandardArticleResponse toArticleResponse(RevisionStandard item) {
        return new RevisionStandardArticleResponse(
                item.getId(),
                item.getArticleNo(),
                item.getDescription(),
                item.getTagColor(),
                item.getPrice()
        );
    }

    private RevisionStandard mapArticle(String articleNo, String description, String tagColor, java.math.BigDecimal price) {
        RevisionStandard article = new RevisionStandard();
        article.setArticleNo(normalizeRequired(articleNo));
        article.setDescription(normalizeRequired(description));
        article.setTagColor(normalizeNullable(tagColor));
        article.setPrice(price);
        return article;
    }

    private int normalizePageNumber(Integer page) {
        if (page == null || page <= 0) {
            return 0;
        }
        return page - 1;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return 25;
        }
        return Math.min(size, 100);
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Zorunlu alan bos olamaz");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new RuntimeException("Minimum fiyat maksimum fiyattan buyuk olamaz");
        }
    }
}
