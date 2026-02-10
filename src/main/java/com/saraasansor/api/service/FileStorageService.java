package com.saraasansor.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${app.file-storage.local.directory:./uploads}")
    private String uploadDirectory;
    
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
     * Get file URL (for local storage, returns relative path)
     */
    public String getFileUrl(String storageKey) {
        return "/api/files/" + storageKey;
    }
}
