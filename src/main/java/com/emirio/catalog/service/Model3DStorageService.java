package com.emirio.catalog.service;

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
public class Model3DStorageService {

    @Value("${app.model3d.upload-dir:./uploads/models}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    private static final String[] ALLOWED_EXTENSIONS = {
        ".obj", ".glb", ".gltf", ".fbx", ".stl", ".ply", ".3ds", ".dae"
    };

    public String storeFile(MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename to prevent collisions
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String storedFilename = UUID.randomUUID().toString() + extension;
        Path destination = uploadPath.resolve(storedFilename);
        
        // Copy file
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        return storedFilename;
    }

    public byte[] loadFile(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }
        return Files.readAllBytes(filePath);
    }

    public void deleteFile(String filename) throws IOException {
        if (filename != null && !filename.isEmpty()) {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File too large. Max size: %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null) {
            boolean isValidType = false;
            String lowerFilename = filename.toLowerCase();
            for (String allowedExtension : ALLOWED_EXTENSIONS) {
                if (lowerFilename.endsWith(allowedExtension)) {
                    isValidType = true;
                    break;
                }
            }
            if (!isValidType) {
                throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS)
                );
            }
        }
    }
}