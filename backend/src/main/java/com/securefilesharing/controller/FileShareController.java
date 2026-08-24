package com.securefilesharing.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.securefilesharing.dto.FileShareResponse;
import com.securefilesharing.service.FileShareService;

@RestController
@RequestMapping("/api/files")
public class FileShareController {

    private final FileShareService fileShareService;

    public FileShareController(
            FileShareService fileShareService) {

        super();
        this.fileShareService = fileShareService;
    }


    // =====================================================
    // SECURE SHARE
    // =====================================================

    @PostMapping(
            value = "/share",
            consumes = "multipart/form-data")
    public ResponseEntity<FileShareResponse> createShare(

            @RequestParam("file")
            MultipartFile file,

            @RequestParam(defaultValue = "24") int expiryHours,

            @RequestParam int maxDownloads,

            @RequestParam(required = false)
            List<String> allowedUserEmails)

            throws IOException {

        FileShareResponse response =
                fileShareService.createShare(
                        file,
                        expiryHours,
                        maxDownloads,
                        allowedUserEmails
                );

        return ResponseEntity.ok(response);
    }


    // =====================================================
    // QUICK SHARE
    // =====================================================

    @PostMapping(
            value = "/quick-share",
            consumes = "multipart/form-data")
    public ResponseEntity<FileShareResponse> createQuickShare(

            @RequestParam("file")
            MultipartFile file,

            @RequestParam(required = false)
            Integer expiryHours)

            throws IOException {

        FileShareResponse response =
                fileShareService.createQuickShare(
                        file,
                        expiryHours
                );

        return ResponseEntity.ok(response);
    }
    
    
	 // =====================================================
	 // UPDATE SECURE SHARE EXPIRY
	 // =====================================================
	
	 @PostMapping("/share/{shareToken}/expiry")
	 public ResponseEntity<FileShareResponse> updateSecureShareExpiry(
	
	         @PathVariable String shareToken,
	
	         @RequestParam int expiryHours) {
	
	     FileShareResponse response =
	             fileShareService.updateSecureShareExpiry(
	                     shareToken,
	                     expiryHours
	             );
	
	     return ResponseEntity.ok(response);
	 }


    // =====================================================
    // REVOKE SECURE SHARE
    // =====================================================

    @PostMapping("/share/{shareToken}/revoke")
    public ResponseEntity<FileShareResponse> revokeSecureShare(
            @PathVariable String shareToken) {

        return ResponseEntity.ok(
                fileShareService.revokeSecureShare(shareToken));
    }


    // =====================================================
    // SHARE HISTORY
    // =====================================================

    @GetMapping("/shares/history")
    public ResponseEntity<List<FileShareResponse>> getShareHistory() {

        return ResponseEntity.ok(
                fileShareService.getShareHistory());
    }


    // =====================================================
    // DOWNLOAD HISTORY
    // =====================================================

    @GetMapping("/download-history")
    public ResponseEntity<List<com.securefilesharing.dto.DownloadHistoryResponse>>
            getDownloadHistory() {

        return ResponseEntity.ok(
                fileShareService.getDownloadHistory());
    }
}