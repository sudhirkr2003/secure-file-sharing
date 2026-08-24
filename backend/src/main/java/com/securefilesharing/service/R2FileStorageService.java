package com.securefilesharing.service;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "r2")
public class R2FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public R2FileStorageService(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.region:auto}") String region,
            @Value("${r2.access-key}") String accessKey,
            @Value("${r2.secret-key}") String secretKey,
            @Value("${r2.bucket}") String bucket) {

        this.bucket = bucket;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey)))
                .serviceConfiguration(
                        software.amazon.awssdk.services.s3.S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build())
                .build();
    }

    @Override
    public String store(MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename() == null
                ? "file"
                : file.getOriginalFilename();

        String key =
                "files/" + UUID.randomUUID() + "_" +
                java.nio.file.Paths.get(originalName).getFileName();

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()));

        return key;
    }

    @Override
    public Resource load(String location) throws IOException {

        GetObjectRequest request =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(location)
                        .build();

        ResponseInputStream<GetObjectResponse> stream =
                s3Client.getObject(request);

        return new InputStreamResource(stream);
    }

    @Override
    public void delete(String location) throws IOException {

        if (location == null || location.isBlank()) {
            return;
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(location)
                        .build());
    }
}
