package com.example.isp392.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final Path rootLocation = Paths.get("src/main/resources/static/uploads");

    public String storeFile(MultipartFile file, String subDir) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }
        Path destinationDir = rootLocation.resolve(Paths.get(subDir)).normalize().toAbsolutePath();
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        String uniqueFilename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path destinationFile = destinationDir.resolve(uniqueFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/" + subDir + "/" + uniqueFilename;
    }
}