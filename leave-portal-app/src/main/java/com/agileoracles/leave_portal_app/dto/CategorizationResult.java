package com.agileoracles.leave_portal_app.dto;

public class CategorizationResult {

    private String category;
    private String matchedReason;

    public CategorizationResult(String category, String matchedReason) {
        this.category = category;
        this.matchedReason = matchedReason;
    }

    public String getCategory() {
        return category;
    }

    public String getMatchedReason() {
        return matchedReason;
    }
}