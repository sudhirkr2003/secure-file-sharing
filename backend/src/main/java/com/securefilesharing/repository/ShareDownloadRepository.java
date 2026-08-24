package com.securefilesharing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securefilesharing.entity.FileShare;
import com.securefilesharing.entity.ShareDownload;
import com.securefilesharing.entity.User;

public interface ShareDownloadRepository
        extends JpaRepository<ShareDownload, Long> {

    Optional<ShareDownload> findByFileShareAndUser(
            FileShare fileShare,
            User user
    );

    java.util.List<ShareDownload> findAllByFileShare(
            FileShare fileShare);

    java.util.List<ShareDownload> findAllByUser(
            User user);
}