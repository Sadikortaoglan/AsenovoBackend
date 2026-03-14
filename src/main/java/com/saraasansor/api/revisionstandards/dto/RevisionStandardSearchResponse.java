package com.saraasansor.api.revisionstandards.dto;

public class RevisionStandardSearchResponse {

    private final Long id;
    private final String articleNo;
    private final String description;
    private final String standardCode;
    private final String tagColor;

    public RevisionStandardSearchResponse(Long id, String articleNo, String description, String standardCode, String tagColor) {
        this.id = id;
        this.articleNo = articleNo;
        this.description = description;
        this.standardCode = standardCode;
        this.tagColor = tagColor;
    }

    public Long getId() {
        return id;
    }

    public String getArticleNo() {
        return articleNo;
    }

    public String getDescription() {
        return description;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public String getTagColor() {
        return tagColor;
    }
}
