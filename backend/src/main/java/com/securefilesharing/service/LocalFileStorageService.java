package com.securefilesharing.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
        name = "storage.type",
        havingValue = "local",
        matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDirectory =
            Paths.get("uploads");

    @Override
    public String store(MultipartFile file) throws IOException {

        Files.createDirectories(uploadDirectory);

        String originalName = file.getOriginalFilename() == null
                ? "file"
                : file.getOriginalFilename();

        String storedName =
                UUID.randomUUID() + "_" + Paths.get(originalName)
                        .getFileName();

        Path destination =
                uploadDirectory.resolve(storedName);

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING);

        return destination.toString();
    }

    @Override
    public Resource load(String location) throws IOException {

        Resource resource =
                new UrlResource(Paths.get(location)
                        .toAbsolutePath()
                        .toUri());

        if (!resource.exists()) {
            throw new IOException("Stored file not found");
        }

        return resource;
    }

    @Override
    public void delete(String location) throws IOException {
        if (location != null) {
            Files.deleteIfExists(Paths.get(location));
        }
    }
}
