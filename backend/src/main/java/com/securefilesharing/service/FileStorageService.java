package com.securefilesharing.service;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file) throws IOException;

    Resource load(String location) throws IOException;

    void delete(String location) throws IOException;
}
