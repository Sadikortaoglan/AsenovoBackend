package com.saraasansor.api.revisionstandards.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class UpdateRevisionStandardArticleRequest {

    @NotBlank
    private String articleNo;

    @NotBlank
    private String description;

    private String tagColor;

    private BigDecimal price;

    public String getArticleNo() {
        return articleNo;
    }

    public void setArticleNo(String articleNo) {
        this.articleNo = articleNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
