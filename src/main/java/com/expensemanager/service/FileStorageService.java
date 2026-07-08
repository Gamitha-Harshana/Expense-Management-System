package com.expensemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/receipts}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "application/pdf"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    public String storeFile(MultipartFile file) throws IOException {
        validateFile(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID() + extension;
        Path targetPath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return storedFilename;
    }

    public Path loadFile(String filename) {
        return Paths.get(uploadDir).resolve(filename);
    }

    public void deleteFile(String filename) {
        try {
            Path path = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log warning but don't fail
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Accepted: JPEG, PNG, PDF");
        }
    }
}
