package com.securefilesharing.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ShareDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "share_id", nullable = false)
    private FileShare fileShare;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime downloadedAt;

    public ShareDownload() {
    }

    public ShareDownload(
            FileShare fileShare,
            User user) {

        this.fileShare = fileShare;
        this.user = user;
        this.downloadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileShare getFileShare() {
        return fileShare;
    }

    public void setFileShare(FileShare fileShare) {
        this.fileShare = fileShare;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(LocalDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }
}