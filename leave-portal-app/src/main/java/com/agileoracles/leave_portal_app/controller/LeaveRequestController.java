package com.agileoracles.leave_portal_app.controller;

import com.agileoracles.leave_portal_app.dto.CategorizationResult;
import com.agileoracles.leave_portal_app.dto.LeaveRequestResponse;
import com.agileoracles.leave_portal_app.dto.OciUploadResult;
import com.agileoracles.leave_portal_app.service.LeaveCategorizationService;
import com.agileoracles.leave_portal_app.service.OciStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveCategorizationService categorizationService;
    private final OciStorageService ociStorageService;

    public LeaveRequestController(LeaveCategorizationService categorizationService,
                                  OciStorageService ociStorageService) {
        this.categorizationService = categorizationService;
        this.ociStorageService = ociStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLeaveRequest(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty or missing");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Unsupported file type. Only .txt is supported for now");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read file content");
        }

        String content = new String(fileBytes, StandardCharsets.UTF_8);
        CategorizationResult result = categorizationService.categorize(content);

        OciUploadResult uploadResult;
        try {
            uploadResult = ociStorageService.uploadFile(fileName, fileBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload file to OCI Object Storage: " + e.getMessage());
        }

        String authenticatedUser = (principal != null) ? principal.getAttribute("email") : "unknown";

        LeaveRequestResponse response = new LeaveRequestResponse(
                authenticatedUser,
                fileName,
                result.getCategory(),
                result.getMatchedReason(),
                LocalDateTime.now(),
                uploadResult.getObjectName(),
                uploadResult.getObjectId()
        );

        return ResponseEntity.ok(response);
    }
}