package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class B2BUnitTransactionPageResponse {

    private List<B2BUnitTransactionResponse> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public B2BUnitTransactionPageResponse() {
    }

    public List<B2BUnitTransactionResponse> getContent() {
        return content;
    }

    public void setContent(List<B2BUnitTransactionResponse> content) {
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
