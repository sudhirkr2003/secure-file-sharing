package com.securefilesharing.controller;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.securefilesharing.dto.FileResponse;
import com.securefilesharing.entity.File;
import com.securefilesharing.service.FileService;
import com.securefilesharing.service.FileShareService;
import com.securefilesharing.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final FileShareService fileShareService;
    private final FileStorageService storageService;

    public FileController(
            FileService fileService,
            FileShareService fileShareService,
            FileStorageService storageService) {

        super();
        this.fileService = fileService;
        this.fileShareService = fileShareService;
        this.storageService = storageService;
    }

    // =====================================================
    // UPLOAD FILE
    // =====================================================

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return ResponseEntity.ok(
                fileService.uploadFile(file)
        );
    }

    // =====================================================
    // DOWNLOAD
    // =====================================================

    @GetMapping("/download/{shareToken}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String shareToken)
            throws IOException {

        File file =
                fileShareService.getFileForDownload(shareToken);

        Resource resource =
                storageService.load(file.getFilePath());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                file.getOriginalFileName() + "\"")
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        file.getContentType() == null
                                ? "application/octet-stream"
                                : file.getContentType())
                .body(resource);
    }

}