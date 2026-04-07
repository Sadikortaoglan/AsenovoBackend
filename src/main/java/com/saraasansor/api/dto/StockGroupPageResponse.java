package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class StockGroupPageResponse {

    private List<StockGroupListItemResponse> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<StockGroupListItemResponse> getContent() {
        return content;
    }

    public void setContent(List<StockGroupListItemResponse> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
