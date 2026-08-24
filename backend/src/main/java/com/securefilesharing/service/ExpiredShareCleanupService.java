package com.securefilesharing.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefilesharing.entity.File;
import com.securefilesharing.entity.FileShare;
import com.securefilesharing.repository.FileShareRepository;

@Service
public class ExpiredShareCleanupService {

    private final FileShareRepository fileShareRepository;
    private final FileStorageService storageService;

    public ExpiredShareCleanupService(
            FileShareRepository fileShareRepository,
            FileStorageService storageService) {
        this.fileShareRepository = fileShareRepository;
        this.storageService = storageService;
    }

    @Scheduled(
            fixedDelayString =
                    "${cleanup.fixed-delay-ms:3600000}")
    @Transactional
    public void cleanupExpiredStorage() {

        LocalDateTime now = LocalDateTime.now();

        for (FileShare share : fileShareRepository.findAll()) {

            boolean unavailable =
                    share.isRevoked() ||
                    !now.isBefore(share.getExpiresAt());

            if (!unavailable) {
                continue;
            }

            File file = share.getFile();

            List<FileShare> sameFileShares =
                    fileShareRepository.findAllByFile(file);

            boolean anotherUsableShareExists =
                    sameFileShares.stream()
                            .anyMatch(other ->
                                    other.getId().equals(share.getId())
                                    ? false
                                    : !other.isRevoked()
                                      && now.isBefore(
                                              other.getExpiresAt()));

            if (!anotherUsableShareExists) {
                try {
                    storageService.delete(file.getFilePath());
                } catch (Exception ex) {
                    System.err.println(
                            "Could not delete stored file " +
                            file.getFilePath() + ": " +
                            ex.getMessage());
                }
            }
        }
    }
}
