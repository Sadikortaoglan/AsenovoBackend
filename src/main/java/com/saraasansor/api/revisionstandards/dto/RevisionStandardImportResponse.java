package com.saraasansor.api.revisionstandards.dto;

import java.util.ArrayList;
import java.util.List;

public class RevisionStandardImportResponse {

    private int filesProcessed;
    private int articlesParsed;
    private int articlesInserted;
    private int articlesUpdated;
    private final List<String> errors = new ArrayList<>();

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public void incrementFilesProcessed() {
        this.filesProcessed++;
    }

    public int getArticlesParsed() {
        return articlesParsed;
    }

    public void addParsed(int count) {
        this.articlesParsed += count;
    }

    public int getArticlesInserted() {
        return articlesInserted;
    }

    public void addInserted(int count) {
        this.articlesInserted += count;
    }

    public int getArticlesUpdated() {
        return articlesUpdated;
    }

    public void addUpdated(int count) {
        this.articlesUpdated += count;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
