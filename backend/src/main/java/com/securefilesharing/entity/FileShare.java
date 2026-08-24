package com.securefilesharing.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    private String shareToken;

    private LocalDateTime expiresAt;

    private boolean quickShare;

    private boolean revoked;

    private Integer maxDownloads;

    private Integer downloadCount;

    private LocalDateTime createdAt;

    // Maximum allowed expiry from original creation time
    private LocalDateTime maximumExpiryAt;


    // =====================================================
    // DEFAULT CONSTRUCTOR
    // =====================================================

    public FileShare() {
    }


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FileShare(
            File file,
            String shareToken,
            LocalDateTime expiresAt,
            boolean quickShare,
            Integer maxDownloads) {

        this.file = file;
        this.shareToken = shareToken;
        this.expiresAt = expiresAt;
        this.quickShare = quickShare;
        this.revoked = false;
        this.maxDownloads = maxDownloads;

        this.downloadCount = 0;

        // Original share creation time
        this.createdAt = LocalDateTime.now();

        // Fixed maximum expiry:
        // Share can never be available beyond
        // 30 days from original creation
        this.maximumExpiryAt =
                quickShare
                        ? this.createdAt.plusHours(24)
                        : this.createdAt.plusDays(30);
    }


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }


    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }


    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }


    public boolean isQuickShare() {
        return quickShare;
    }

    public void setQuickShare(boolean quickShare) {
        this.quickShare = quickShare;
    }


    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }


    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getMaximumExpiryAt() {
        return maximumExpiryAt;
    }

    public void setMaximumExpiryAt(
            LocalDateTime maximumExpiryAt) {

        this.maximumExpiryAt = maximumExpiryAt;
    }
}