package com.saraasansor.api.revisionstandards.dto;

public class RevisionStandardSetResponse {

    private final Long id;
    private final String standardCode;
    private final long articleCount;

    public RevisionStandardSetResponse(Long id, String standardCode, long articleCount) {
        this.id = id;
        this.standardCode = standardCode;
        this.articleCount = articleCount;
    }

    public Long getId() {
        return id;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public long getArticleCount() {
        return articleCount;
    }
}
