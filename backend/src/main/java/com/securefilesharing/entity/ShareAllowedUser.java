package com.securefilesharing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ShareAllowedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "share_id", nullable = false)
    private FileShare fileShare;

    @Column(nullable = false)
    private String email;

    public ShareAllowedUser() {
    }

    public ShareAllowedUser(FileShare fileShare, String email) {
        this.fileShare = fileShare;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}