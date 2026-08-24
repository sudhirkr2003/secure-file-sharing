package com.securefilesharing.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FileShareResponse {

    private Long id;

    private FileResponse file;

    private String shareToken;

    private LocalDateTime expiresAt;

    private boolean quickShare;

    private Integer maxDownloads;

    private Integer downloadCount;

    private LocalDateTime createdAt;
    private boolean revoked;
    private List<UserAccessResponse> allowedUsers;
    private List<DownloadedByResponse> downloadedBy;


    // =====================================================
    // DEFAULT CONSTRUCTOR
    // =====================================================

    public FileShareResponse() {
    }


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FileShareResponse(
            Long id,
            FileResponse file,
            String shareToken,
            LocalDateTime expiresAt,
            boolean quickShare,
            Integer maxDownloads,
            Integer downloadCount,
            LocalDateTime createdAt,
            boolean revoked,
            List<UserAccessResponse> allowedUsers,
            List<DownloadedByResponse> downloadedBy) {

        this.id = id;
        this.file = file;
        this.shareToken = shareToken;
        this.expiresAt = expiresAt;
        this.quickShare = quickShare;
        this.maxDownloads = maxDownloads;
        this.downloadCount = downloadCount;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.allowedUsers = allowedUsers;
        this.downloadedBy = downloadedBy;
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public FileResponse getFile() {
        return file;
    }

    public String getShareToken() {
        return shareToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isQuickShare() {
        return quickShare;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public List<UserAccessResponse> getAllowedUsers() {
        return allowedUsers;
    }

    public List<DownloadedByResponse> getDownloadedBy() {
        return downloadedBy;
    }
}