package com.securefilesharing.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", true);

        this.cloudinary = new Cloudinary(config);
    }

    // Location format stored in the DB: "<resourceType>|<publicId>|<secureUrl>"
    // resourceType/publicId are needed to delete the asset later,
    // secureUrl is used to stream the file back on download.

    @Override
    @SuppressWarnings("unchecked")
    public String store(MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename() == null
                ? "file"
                : file.getOriginalFilename();

        String publicId = "files/" + UUID.randomUUID() + "_" +
                java.nio.file.Paths.get(originalName).getFileName();

        Map<String, Object> options = ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "auto",
                "use_filename", false,
                "unique_filename", false,
                "overwrite", false
        );

        Map<String, Object> result =
                cloudinary.uploader().upload(file.getBytes(), options);

        String resourceType = (String) result.get("resource_type");
        String returnedPublicId = (String) result.get("public_id");
        String secureUrl = (String) result.get("secure_url");

        return resourceType + "|" + returnedPublicId + "|" + secureUrl;
    }

    @Override
    public Resource load(String location) throws IOException {

        String[] parts = location.split("\\|", 3);
        String secureUrl = parts.length == 3 ? parts[2] : location;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(secureUrl))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                throw new IOException(
                        "Failed to fetch file from Cloudinary, status: " +
                                response.statusCode());
            }

            return new InputStreamResource(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching file from Cloudinary", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(String location) throws IOException {

        if (location == null || location.isBlank()) {
            return;
        }

        String[] parts = location.split("\\|", 3);

        if (parts.length < 2) {
            return;
        }

        String resourceType = parts[0];
        String publicId = parts[1];

        Map<String, Object> options = ObjectUtils.asMap(
                "resource_type", resourceType
        );

        cloudinary.uploader().destroy(publicId, options);
    }
}