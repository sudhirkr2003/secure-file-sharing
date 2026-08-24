package com.securefilesharing.dto;

import java.time.LocalDateTime;

public class DownloadHistoryResponse {

    private Long shareId;
    private String fileName;
    private String downloadedBy;
    private LocalDateTime downloadedAt;

    public DownloadHistoryResponse() {
    }

    public DownloadHistoryResponse(
            Long shareId,
            String fileName,
            String downloadedBy,
            LocalDateTime downloadedAt) {
        this.shareId = shareId;
        this.fileName = fileName;
        this.downloadedBy = downloadedBy;
        this.downloadedAt = downloadedAt;
    }

    public Long getShareId() {
        return shareId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadedBy() {
        return downloadedBy;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }
}
