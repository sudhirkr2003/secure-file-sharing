package com.securefilesharing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securefilesharing.entity.FileShare;
import com.securefilesharing.entity.ShareAllowedUser;

public interface ShareAllowedUserRepository
        extends JpaRepository<ShareAllowedUser, Long> {

    Optional<ShareAllowedUser> findByFileShareAndEmail(
            FileShare fileShare,
            String email
    );
    
    List<ShareAllowedUser> findAllByFileShare(FileShare fileShare);
}