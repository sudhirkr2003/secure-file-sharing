package com.securefilesharing.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.securefilesharing.dto.DownloadedByResponse;
import com.securefilesharing.dto.UserAccessResponse;
import com.securefilesharing.dto.FileResponse;
import com.securefilesharing.dto.DownloadHistoryResponse;
import com.securefilesharing.dto.FileShareResponse;
import com.securefilesharing.entity.File;
import com.securefilesharing.entity.FileShare;
import com.securefilesharing.entity.ShareAllowedUser;
import com.securefilesharing.entity.ShareDownload;
import com.securefilesharing.entity.User;
import com.securefilesharing.exception.ForbiddenException;
import com.securefilesharing.exception.UnauthorizedException;
import com.securefilesharing.repository.FileRepository;
import com.securefilesharing.repository.FileShareRepository;
import com.securefilesharing.repository.ShareAllowedUserRepository;
import com.securefilesharing.repository.ShareDownloadRepository;
import com.securefilesharing.repository.UserRepository;

@Service
public class FileShareService {

    private final FileShareRepository fileShareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ShareAllowedUserRepository shareAllowedUserRepository;
    private final ShareDownloadRepository shareDownloadRepository;
    private final FileService fileService;

    public FileShareService(
            FileShareRepository fileShareRepository,
            FileRepository fileRepository,
            UserRepository userRepository,
            ShareAllowedUserRepository shareAllowedUserRepository,
            ShareDownloadRepository shareDownloadRepository,
            FileService fileService) {

        this.fileShareRepository = fileShareRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.shareAllowedUserRepository = shareAllowedUserRepository;
        this.shareDownloadRepository = shareDownloadRepository;
        this.fileService = fileService;
    }

    // =========================================================
    // SECURE SHARE
    // File upload + Secure Share creation
    // =========================================================

    public FileShareResponse createShare(
            MultipartFile multipartFile,
            int expiryHours,
            int maxDownloads,
            List<String> allowedUserEmails) throws IOException {

        // -----------------------------------------------------
        // 1. Sender must be logged in
        // -----------------------------------------------------
    	


        User sender = getLoggedInUser();

        if (sender == null) {

            throw new RuntimeException(
                    "Login is required for secure sharing");
        }

        // -----------------------------------------------------
        // 2. File validation
        // -----------------------------------------------------

        if (multipartFile == null ||
                multipartFile.isEmpty()) {

            throw new RuntimeException(
                    "File is required");
        }

        // -----------------------------------------------------
        // 3. Expiry validation
        // -----------------------------------------------------

        if (expiryHours <= 0) {

            throw new RuntimeException(
                    "Expiry must be greater than 0 hours");
        }

        // At creation Secure Share can be at most 720 hours.
        // The 30-day limit applies only to later extensions.
        if (expiryHours > 720) {

            throw new RuntimeException(
                    "Secure share expiry at creation cannot exceed 720 hours");
        }

	     // -----------------------------------------------------
	     // 4. Download limit validation
	     // -----------------------------------------------------
	
	     if (maxDownloads <= 0) {
	         throw new RuntimeException(
	                 "Download limit must be greater than 0");
	     }
	
	     // Normalize and check duplicate emails
	     Set<String> normalizedEmails = new LinkedHashSet<>();

	     if (allowedUserEmails != null) {

	         for (String email : allowedUserEmails) {

	             if (email == null || email.isBlank()) {
	                 continue;
	             }

	             String normalizedEmail =
	            	        email.trim().toLowerCase();

            	// Validate email format first
            	if (!isValidEmail(normalizedEmail)) {

            	    throw new RuntimeException(
            	            "Invalid email address: "
            	            + email);
            	}

            	// Check duplicate email
            	if (!normalizedEmails.add(normalizedEmail)) {

            	    throw new RuntimeException(
            	            "Duplicate email entered: "
            	            + email);
            	}
	         }
	     }

        // -----------------------------------------------------
        // 5. Upload file
        // -----------------------------------------------------

        FileResponse uploadedFileResponse =
                fileService.uploadFile(multipartFile);

        // -----------------------------------------------------
        // 6. Get actual File entity
        // -----------------------------------------------------

        File file =
                fileRepository.findById(
                        uploadedFileResponse.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Uploaded file not found"));

        // -----------------------------------------------------
        // 7. Generate share token
        // -----------------------------------------------------

        String shareToken =
                UUID.randomUUID().toString();

        // -----------------------------------------------------
        // 8. Calculate expiry
        // -----------------------------------------------------

        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusHours(expiryHours);

        // -----------------------------------------------------
        // 9. Create Secure Share
        // -----------------------------------------------------

        FileShare fileShare =
                new FileShare(
                        file,
                        shareToken,
                        expiresAt,
                        false,
                        maxDownloads);

        FileShare savedShare =
                fileShareRepository.save(fileShare);

	     // -----------------------------------------------------
	     // 10. Allowed users are OPTIONAL and are supplied by email.
	     // -----------------------------------------------------
	
	     if (!normalizedEmails.isEmpty()) {
	
	         for (String email : normalizedEmails) {
	
	             ShareAllowedUser allowedUser =
	                     new ShareAllowedUser(
	                             savedShare,
	                             email);
	
	             shareAllowedUserRepository.save(
	                     allowedUser);
	         }
	     }

        // -----------------------------------------------------
        // 11. Return response
        // -----------------------------------------------------

        return convertToResponse(savedShare);
    }
    
    private boolean isValidEmail(String email) {
    	if(email == null || email.isBlank()) {
    		return false;
    	}
    	
    	return email.matches("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    // =========================================================
    // QUICK SHARE
    // File upload + Quick Share creation
    // =========================================================

    public FileShareResponse createQuickShare(
            MultipartFile multipartFile,
            Integer expiryHours) throws IOException {

        // -----------------------------------------------------
        // 1. File validation
        // -----------------------------------------------------

        if (multipartFile == null ||
                multipartFile.isEmpty()) {

            throw new RuntimeException(
                    "File is required");
        }

        // -----------------------------------------------------
        // 2. Default expiry = 24 hours
        // -----------------------------------------------------

        if (expiryHours == null) {

            expiryHours = 24;
        }

        // -----------------------------------------------------
        // 3. Quick Share expiry = 1 to 24 hours
        // -----------------------------------------------------

        if (expiryHours <= 0 ||
                expiryHours > 24) {

            throw new RuntimeException(
                    "Quick share expiry must be between 1 and 24 hours");
        }

        // -----------------------------------------------------
        // 4. Upload file
        // -----------------------------------------------------

        FileResponse uploadedFileResponse =
                fileService.uploadFile(multipartFile);

        // -----------------------------------------------------
        // 5. Get actual File entity
        // -----------------------------------------------------

        File file =
                fileRepository.findById(
                        uploadedFileResponse.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Uploaded file not found"));

        // -----------------------------------------------------
        // 6. Generate share token
        // -----------------------------------------------------

        String shareToken =
                UUID.randomUUID().toString();

        // -----------------------------------------------------
        // 7. Calculate expiry
        // -----------------------------------------------------

        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusHours(expiryHours);

        // -----------------------------------------------------
        // 8. Create Quick Share
        // -----------------------------------------------------

        FileShare fileShare =
                new FileShare(
                        file,
                        shareToken,
                        expiresAt,
                        true,
                        null);

        FileShare savedShare =
                fileShareRepository.save(fileShare);

        // -----------------------------------------------------
        // 9. Return response
        // -----------------------------------------------------

        return convertToResponse(savedShare);
    }

    // =========================================================
    // GET LOGGED-IN USER
    // =========================================================

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

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElse(null);
    }

    // =========================================================
    // SECURE SHARE DOWNLOAD
    // =========================================================
    
    public File getFileByShareToken(String shareToken) {

        // -----------------------------------------------------
        // 1. Find share
        // -----------------------------------------------------

        FileShare fileShare =
                fileShareRepository
                        .findByShareToken(shareToken)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Share link not found"));

        User currentUser =
                getLoggedInUser();

        if (currentUser == null) {

            throw new UnauthorizedException(
                    "Login is required to download this file");
        }

        // -----------------------------------------------------
        // 5. Get file and owner
        // -----------------------------------------------------

        File file =
                fileShare.getFile();

        User owner =
                file.getUploadedBy();

        boolean isOwner =
                owner != null &&
                owner.getId().equals(
                        currentUser.getId());

        // -----------------------------------------------------
        // 6. Owner can always access his own Secure Share
        // -----------------------------------------------------

        if (isOwner) {

            // Revoked information is okay to reveal
            // because the user is the owner/authorized person.

            if (fileShare.isRevoked()) {

                throw new RuntimeException(
                        "Share has been revoked");
            }

            return file;
        }

        // -----------------------------------------------------
        // 7. Check allowed users
        // -----------------------------------------------------

        List<ShareAllowedUser> allowedUsers =
                shareAllowedUserRepository
                        .findAllByFileShare(fileShare);

        if (!allowedUsers.isEmpty()) {
        	
            boolean allowed =
                    shareAllowedUserRepository
                            .findByFileShareAndEmail(
                                    fileShare,
                                    currentUser.getEmail().trim().toLowerCase())
                            .isPresent();

            if (!allowed) {

                // IMPORTANT:
                // Do not tell this user whether the share
                // is revoked or not.

                throw new ForbiddenException(
                        "You are not an authorized person for this file");
            }
        }

        // -----------------------------------------------------
        // 8. At this point user is authorized
        //
        // Now it is safe to reveal revoked status.
        // -----------------------------------------------------

        if (fileShare.isRevoked()) {

            throw new RuntimeException(
                    "Share has been revoked");
        }

        // -----------------------------------------------------
        // 9. Check global download limit
        // -----------------------------------------------------



        // -----------------------------------------------------
        // 10. Check whether user already downloaded
        // -----------------------------------------------------

        boolean alreadyDownloaded =
                shareDownloadRepository
                        .findByFileShareAndUser(
                                fileShare,
                                currentUser)
                        .isPresent();
        
        // -----------------------------------------------------
        // 11. Count only first download by this user
        // -----------------------------------------------------

        if (alreadyDownloaded) {
        	
        } else if (fileShare.getMaxDownloads() != null &&
            fileShare.getDownloadCount() >=
            fileShare.getMaxDownloads()) {
        	throw new RuntimeException(
            "Download limit has been reached");
        } else {
            ShareDownload shareDownload =
                    new ShareDownload(
                            fileShare,
                            currentUser);

            shareDownloadRepository.save(
                    shareDownload);

            fileShare.setDownloadCount(
                    fileShare.getDownloadCount() + 1);

            fileShareRepository.save(fileShare);
        }

        // -----------------------------------------------------
        // 12. Return file
        // -----------------------------------------------------

        return file;
    }

    
    
	 // =========================================================
	 // UNIVERSAL DOWNLOAD
	 // =========================================================
	
    public File getFileForDownload(String shareToken) {

        FileShare fileShare =
                fileShareRepository
                        .findByShareToken(shareToken)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Share link not found"));
        
        if (!LocalDateTime.now()
                .isBefore(fileShare.getExpiresAt())) {

            throw new RuntimeException(
                    "Share link has expired");
        }

        if (fileShare.isQuickShare()) {
            return fileShare.getFile();
        }

        return getFileByShareToken(shareToken);
    }

    // =========================================================
    // UPDATE SECURE SHARE EXPIRY
    // =========================================================

    public FileShareResponse updateSecureShareExpiry(
            String shareToken,
            int expiryHours) {

        // -----------------------------------------------------
        // 1. Find share
        // -----------------------------------------------------

        FileShare fileShare =
                fileShareRepository
                        .findByShareToken(shareToken)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Share link not found"));

        // -----------------------------------------------------
        // 2. Quick Share cannot be modified
        // -----------------------------------------------------

        if (fileShare.isQuickShare()) {

            throw new RuntimeException(
                    "Quick Share cannot be modified");
        }

        if (fileShare.isRevoked()) {

            throw new RuntimeException(
                    "Revoked share cannot be modified");
        }

        // -----------------------------------------------------
        // 3. Login required
        // -----------------------------------------------------

        User sender = getLoggedInUser();

        if (sender == null) {

            throw new RuntimeException(
                    "Login is required");
        }

        // -----------------------------------------------------
        // 4. Only owner can modify
        // -----------------------------------------------------

        if (fileShare.getFile().getUploadedBy() == null ||
                !fileShare.getFile()
                        .getUploadedBy()
                        .getId()
                        .equals(sender.getId())) {

            throw new RuntimeException(
                    "You are not allowed to modify this share");
        }

        // -----------------------------------------------------
        // 5. Expired share cannot be modified
        // -----------------------------------------------------

        if (!LocalDateTime.now()
                .isBefore(fileShare.getExpiresAt())) {

            throw new RuntimeException(
                    "Expired share cannot be modified");
        }

        // -----------------------------------------------------
        // 6. Expiry must be positive
        // -----------------------------------------------------

        if (expiryHours <= 0) {

            throw new RuntimeException(
                    "Expiry must be greater than 0 hours");
        }

        // -----------------------------------------------------
        // 7. Calculate new expiry
        // -----------------------------------------------------

        LocalDateTime newExpiry =
                LocalDateTime.now()
                        .plusHours(expiryHours);

        // -----------------------------------------------------
        // 8. Make sure maximumExpiryAt exists
        // -----------------------------------------------------

        if (fileShare.getMaximumExpiryAt() == null) {

            LocalDateTime createdAt =
                    fileShare.getCreatedAt();

            if (createdAt == null) {

                throw new RuntimeException(
                        "Share creation date is missing");
            }

            fileShare.setMaximumExpiryAt(
                    createdAt.plusDays(30));

            fileShareRepository.save(fileShare);
        }

        // -----------------------------------------------------
        // 9. Check fixed 30-day maximum
        // -----------------------------------------------------

        if (newExpiry.isAfter(
                fileShare.getMaximumExpiryAt())) {

            throw new RuntimeException(
                    "Expiry cannot be extended beyond 30 days from share creation");
        }

        // -----------------------------------------------------
        // 10. Update expiry
        // -----------------------------------------------------

        fileShare.setExpiresAt(newExpiry);

        // -----------------------------------------------------
        // 11. Save
        // -----------------------------------------------------

        FileShare updatedShare =
                fileShareRepository.save(fileShare);

        // -----------------------------------------------------
        // 12. Return response
        // -----------------------------------------------------

        return convertToResponse(updatedShare);
    }

    // =========================================================
    // REVOKE SECURE SHARE
    // =========================================================

    public FileShareResponse revokeSecureShare(
            String shareToken) {

        FileShare fileShare =
                fileShareRepository
                        .findByShareToken(shareToken)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Share link not found"));

        if (fileShare.isQuickShare()) {

            throw new RuntimeException(
                    "Quick Share cannot be revoked");
        }

        User sender = getLoggedInUser();

        if (sender == null) {

            throw new RuntimeException(
                    "Login is required");
        }

        if (fileShare.getFile().getUploadedBy() == null ||
                !fileShare.getFile().getUploadedBy()
                        .getId()
                        .equals(sender.getId())) {

            throw new RuntimeException(
                    "You are not allowed to revoke this share");
        }

        if (fileShare.isRevoked()) {

            throw new RuntimeException(
                    "Share is already revoked");
        }

        fileShare.setRevoked(true);

        return convertToResponse(
                fileShareRepository.save(fileShare));
    }

    // =========================================================
    // SHARE HISTORY
    // =========================================================

    public List<FileShareResponse> getShareHistory() {

        User sender = getLoggedInUser();

        if (sender == null) {

            throw new RuntimeException(
                    "Login is required");
        }

        return fileShareRepository.findAll()
                .stream()
                .filter(share ->
                        share.getFile().getUploadedBy() != null &&
                        share.getFile().getUploadedBy()
                                .getId()
                                .equals(sender.getId()))
                .map(this::convertToResponse)
                .toList();
    }

    // =========================================================
    // DOWNLOAD HISTORY
    // =========================================================

    public List<DownloadHistoryResponse> getDownloadHistory() {

        User user = getLoggedInUser();

        if (user == null) {

            throw new RuntimeException(
                    "Login is required");
        }

        return shareDownloadRepository
                .findAllByUser(user)
                .stream()
                .map(download ->
                        new DownloadHistoryResponse(
                                download.getFileShare().getId(),
                                download.getFileShare().getFile()
                                        .getOriginalFileName(),
                                download.getUser().getName(),
                                download.getDownloadedAt()))
                .toList();
    }

    // =========================================================
    // FILE SHARE ENTITY -> DTO
    // =========================================================
    
    private FileShareResponse convertToResponse(FileShare fileShare) {

        File file = fileShare.getFile();

        // -----------------------------------------------------
        // File Entity -> FileResponse
        // -----------------------------------------------------

        FileResponse fileResponse =
                new FileResponse(
                        file.getId(),
                        file.getOriginalFileName(),
                        file.getContentType(),
                        file.getFileSize(),
                        file.getUploadedBy() != null
                                ? file.getUploadedBy().getId()
                                : null
                );

        // -----------------------------------------------------
        // Allowed Users
        // -----------------------------------------------------

        List<UserAccessResponse> allowedUsers =
                shareAllowedUserRepository
                        .findAllByFileShare(fileShare)
                        .stream()
                        .map(item ->
                                new UserAccessResponse(
                                        null,
                                        item.getEmail()
                                ))
                        .toList();

        // -----------------------------------------------------
        // Downloaded By
        // -----------------------------------------------------

        List<DownloadedByResponse> downloadedBy =
                shareDownloadRepository
                        .findAllByFileShare(fileShare)
                        .stream()
                        .map(item ->
                                new DownloadedByResponse(
                                        item.getUser().getName()
                                ))
                        .toList();

        // -----------------------------------------------------
        // Final Response
        // -----------------------------------------------------

        return new FileShareResponse(
                fileShare.getId(),
                fileResponse,
                fileShare.getShareToken(),
                fileShare.getExpiresAt(),
                fileShare.isQuickShare(),
                fileShare.getMaxDownloads(),
                fileShare.getDownloadCount(),
                fileShare.getCreatedAt(),
                fileShare.isRevoked(),
                allowedUsers,
                downloadedBy
        );
    }
}