package com.saraasansor.api.revisionstandards.dto;

import java.math.BigDecimal;

public class RevisionStandardArticleResponse {

    private final Long id;
    private final String articleNo;
    private final String description;
    private final String tagColor;
    private final BigDecimal price;

    public RevisionStandardArticleResponse(Long id, String articleNo, String description, String tagColor, BigDecimal price) {
        this.id = id;
        this.articleNo = articleNo;
        this.description = description;
        this.tagColor = tagColor;
        this.price = price;
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

    public String getTagColor() {
        return tagColor;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
