package com.saraasansor.api.service;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.InvalidPathException;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final String DEFAULT_UPLOAD_DIRECTORY = "./uploads";

    @Value("${app.file-storage.local.directory:./uploads}")
    private String uploadDirectory = DEFAULT_UPLOAD_DIRECTORY;
    
    /**
     * Save file to local storage
     * Returns storage key (relative path)
     */
    public String saveFile(MultipartFile file, String entityType, Long entityId) throws IOException {
        // Create directory structure: uploads/{entityType}/{entityId}/
        String directoryPath = uploadDirectory + "/" + entityType.toLowerCase() + "/" + entityId;
        Path directory = Paths.get(directoryPath);
        
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = directory.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return storage key (relative path)
        return entityType.toLowerCase() + "/" + entityId + "/" + uniqueFilename;
    }

    /**
     * Save file to a specific relative directory under upload root.
     * Returns storage key (relative path).
     */
    public String saveFileToDirectory(MultipartFile file, String relativeDirectory, String fileName) throws IOException {
        Path directory = Paths.get(uploadDirectory).resolve(relativeDirectory);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return relativeDirectory + "/" + fileName;
    }
    
    /**
     * Get file URL (for local storage, returns relative path)
     */
    public String getFileUrl(String storageKey) {
        return "/api/files/" + storageKey;
    }

    public String toStorageKey(String attachmentUrlOrStorageKey) {
        if (!StringUtils.hasText(attachmentUrlOrStorageKey)) {
            return null;
        }

        String value = attachmentUrlOrStorageKey.trim();
        if (value.startsWith("/api/files/")) {
            return value.substring("/api/files/".length());
        }

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                URI uri = URI.create(value);
                String path = uri.getPath();
                if (StringUtils.hasText(path) && path.startsWith("/api/files/")) {
                    return path.substring("/api/files/".length());
                }
            } catch (IllegalArgumentException ignored) {
            }
            return null;
        }

        return value.startsWith("/") ? value.substring(1) : value;
    }

    public Path resolveStoredPath(String attachmentUrlOrStorageKey) {
        String storageKey = toStorageKey(attachmentUrlOrStorageKey);
        if (!StringUtils.hasText(storageKey)) {
            throw new RuntimeException("Attachment storage key is required");
        }

        String baseDirectory = StringUtils.hasText(uploadDirectory)
                ? uploadDirectory
                : DEFAULT_UPLOAD_DIRECTORY;
        Path root = Paths.get(baseDirectory).toAbsolutePath().normalize();
        Path target;
        try {
            target = root.resolve(storageKey).normalize();
        } catch (InvalidPathException ex) {
            throw new RuntimeException("Invalid attachment path");
        }

        if (!target.startsWith(root)) {
            throw new RuntimeException("Invalid attachment path");
        }

        return target;
    }
}
