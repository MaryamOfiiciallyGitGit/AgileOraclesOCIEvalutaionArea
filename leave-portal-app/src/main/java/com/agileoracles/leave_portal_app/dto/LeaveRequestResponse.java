package com.agileoracles.leave_portal_app.dto;

import java.time.LocalDateTime;

public class LeaveRequestResponse {

    private String authenticatedUser;
    private String fileName;
    private String category;
    private String matchedReason;
    private LocalDateTime uploadTimestamp;
    private String ociObjectName;
    private String ociObjectId;

    public LeaveRequestResponse(String authenticatedUser, String fileName, String category, String matchedReason,
                                LocalDateTime uploadTimestamp, String ociObjectName, String ociObjectId) {
        this.authenticatedUser = authenticatedUser;
        this.fileName = fileName;
        this.category = category;
        this.matchedReason = matchedReason;
        this.uploadTimestamp = uploadTimestamp;
        this.ociObjectName = ociObjectName;
        this.ociObjectId = ociObjectId;
    }

    public String getAuthenticatedUser() { return authenticatedUser; }
    public String getFileName() { return fileName; }
    public String getCategory() { return category; }
    public String getMatchedReason() { return matchedReason; }
    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public String getOciObjectName() { return ociObjectName; }
    public String getOciObjectId() { return ociObjectId; }
}