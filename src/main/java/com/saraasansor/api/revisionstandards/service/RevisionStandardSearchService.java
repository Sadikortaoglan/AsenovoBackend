package com.saraasansor.api.revisionstandards.service;

import com.saraasansor.api.revisionstandards.dto.RevisionStandardSearchResponse;
import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import com.saraasansor.api.revisionstandards.repository.RevisionStandardsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RevisionStandardSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final RevisionStandardsRepository revisionStandardsRepository;

    public RevisionStandardSearchService(RevisionStandardsRepository revisionStandardsRepository) {
        this.revisionStandardsRepository = revisionStandardsRepository;
    }

    public List<RevisionStandardSearchResponse> search(String query, Integer requestedLimit) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }

        int limit = DEFAULT_LIMIT;
        if (requestedLimit != null) {
            limit = Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        }

        return revisionStandardsRepository.search(query.trim(), limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private RevisionStandardSearchResponse toResponse(RevisionStandard standard) {
        return new RevisionStandardSearchResponse(
                standard.getId(),
                standard.getArticleNo(),
                standard.getDescription(),
                standard.getStandardCode(),
                standard.getTagColor()
        );
    }
}
