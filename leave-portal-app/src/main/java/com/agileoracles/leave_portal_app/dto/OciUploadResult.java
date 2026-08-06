package com.agileoracles.leave_portal_app.dto;

public class OciUploadResult {

    private String objectName;
    private String objectId;

    public OciUploadResult(String objectName, String objectId) {
        this.objectName = objectName;
        this.objectId = objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectId() {
        return objectId;
    }
}