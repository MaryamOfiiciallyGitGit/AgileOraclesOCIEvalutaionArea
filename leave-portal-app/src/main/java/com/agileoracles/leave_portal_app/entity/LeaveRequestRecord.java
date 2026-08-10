package com.agileoracles.leave_portal_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "LEAVE_REQUESTS")
public class LeaveRequestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_EMAIL")
    private String userEmail;

    @Column(name = "ATTACHED_FILENAME")
    private String attachedFilename;

    @Column(name = "REASON_FOR_LEAVE", length = 4000)
    private String reasonForLeave;

    @Column(name = "LEAVE_CATEGORY")
    private String leaveCategory;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "OCI_OBJECT_NAME")
    private String ociObjectName;

    @Column(name = "OCI_OBJECT_ID")
    private String ociObjectId;

    @Column(name = "OCI_BUCKET_NAME")
    private String ociBucketName;

    public Long getId() { return id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getAttachedFilename() { return attachedFilename; }
    public void setAttachedFilename(String attachedFilename) { this.attachedFilename = attachedFilename; }

    public String getReasonForLeave() { return reasonForLeave; }
    public void setReasonForLeave(String reasonForLeave) { this.reasonForLeave = reasonForLeave; }

    public String getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(String leaveCategory) { this.leaveCategory = leaveCategory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOciObjectName() { return ociObjectName; }
    public void setOciObjectName(String ociObjectName) { this.ociObjectName = ociObjectName; }

    public String getOciObjectId() { return ociObjectId; }
    public void setOciObjectId(String ociObjectId) { this.ociObjectId = ociObjectId; }

    public String getOciBucketName() { return ociBucketName; }
    public void setOciBucketName(String ociBucketName) { this.ociBucketName = ociBucketName; }
}