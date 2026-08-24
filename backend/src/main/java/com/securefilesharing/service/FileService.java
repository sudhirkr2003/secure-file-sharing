package com.securefilesharing.service;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.securefilesharing.dto.FileResponse;
import com.securefilesharing.entity.File;
import com.securefilesharing.entity.User;
import com.securefilesharing.repository.FileRepository;
import com.securefilesharing.repository.UserRepository;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;

    public FileService(
            FileRepository fileRepository,
            UserRepository userRepository,
            FileStorageService storageService) {

        super();
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    public FileResponse uploadFile(
            MultipartFile multipartFile) throws IOException {

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        String originalFileName =
                multipartFile.getOriginalFilename() == null
                        ? "file"
                        : multipartFile.getOriginalFilename();

        String storedLocation =
                storageService.store(multipartFile);

        User uploadedBy = getLoggedInUser();

        File file = new File(
                originalFileName,
                storedLocation,
                storedLocation,
                multipartFile.getContentType(),
                multipartFile.getSize(),
                uploadedBy
        );

        try {
            File savedFile = fileRepository.save(file);

            return new FileResponse(
                    savedFile.getId(),
                    savedFile.getOriginalFileName(),
                    savedFile.getContentType(),
                    savedFile.getFileSize(),
                    savedFile.getUploadedBy() != null
                            ? savedFile.getUploadedBy().getId()
                            : null
            );
        } catch (RuntimeException ex) {
            storageService.delete(storedLocation);
            throw ex;
        }
    }

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(
                        authentication.getPrincipal())) {

            return null;
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElse(null);
    }
}
