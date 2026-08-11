package com.agileoracles.leave_portal_app.controller;

import com.agileoracles.leave_portal_app.dto.OciUploadResult;
import com.agileoracles.leave_portal_app.entity.LeaveRequestRecord;
import com.agileoracles.leave_portal_app.repository.LeaveRequestRepository;
import com.agileoracles.leave_portal_app.service.OciStorageService;
import com.agileoracles.leave_portal_app.service.PdfTextExtractionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final OciStorageService ociStorageService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestController(OciStorageService ociStorageService,
                                  PdfTextExtractionService pdfTextExtractionService,
                                  LeaveRequestRepository leaveRequestRepository) {
        this.ociStorageService = ociStorageService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLeaveRequest(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty or missing");
        }

        String fileName = file.getOriginalFilename();
        boolean isPdf = fileName != null && fileName.toLowerCase().endsWith(".pdf");
        boolean isTxt = fileName != null && fileName.toLowerCase().endsWith(".txt");

        if (!isPdf && !isTxt) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Unsupported file type. Only .txt and .pdf are supported");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read file content");
        }

        String reasonForLeave;
        try {
            reasonForLeave = isPdf
                    ? pdfTextExtractionService.extractText(fileBytes)
                    : new String(fileBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to read file content: " + e.getMessage());
        }

        OciUploadResult uploadResult;
        try {
            uploadResult = ociStorageService.uploadFile(fileName, fileBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to upload file to OCI Object Storage: " + e.getMessage());
        }

        String authenticatedUser = (principal != null) ? principal.getAttribute("email") : "unknown";

        LeaveRequestRecord record = new LeaveRequestRecord();
        record.setUserEmail(authenticatedUser);
        record.setAttachedFilename(fileName);
        record.setReasonForLeave(reasonForLeave);
        record.setLeaveCategory(null);
        record.setCreatedAt(LocalDateTime.now());
        record.setOciObjectName(uploadResult.getObjectName());
        record.setOciObjectId(uploadResult.getObjectId());
        record.setOciBucketName(ociStorageService.getBucketName());

        LeaveRequestRecord saved = leaveRequestRepository.save(record);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles(@AuthenticationPrincipal OAuth2User principal) {
        String authenticatedUser = (principal != null) ? principal.getAttribute("email") : "unknown";
        return ResponseEntity.ok(leaveRequestRepository.findByUserEmail(authenticatedUser));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id,
                                          @AuthenticationPrincipal OAuth2User principal) {

        String authenticatedUser = (principal != null) ? principal.getAttribute("email") : "unknown";

        Optional<LeaveRequestRecord> recordOpt = leaveRequestRepository.findById(id);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LeaveRequestRecord record = recordOpt.get();

        if (!authenticatedUser.equals(record.getUserEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have access to this file");
        }

        byte[] fileBytes;
        try {
            fileBytes = ociStorageService.downloadFile(record.getOciObjectName());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to download file: " + e.getMessage());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getAttachedFilename() + "\"")
                .contentType(record.getAttachedFilename().toLowerCase().endsWith(".pdf")
                        ? MediaType.APPLICATION_PDF : MediaType.TEXT_PLAIN)
                .body(fileBytes);
    }
}