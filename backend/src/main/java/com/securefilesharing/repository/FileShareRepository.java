package com.securefilesharing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securefilesharing.entity.File;
import com.securefilesharing.entity.FileShare;

public interface FileShareRepository
        extends JpaRepository<FileShare, Long> {

    Optional<FileShare> findByShareToken(String shareToken);

    // existing methods...

    List<FileShare> findAllByFile(File file);
}