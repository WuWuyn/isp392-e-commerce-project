package com.example.isp392.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    // **THAY ĐỔI QUAN TRỌNG**
    // rootLocation giờ đây cũng trỏ đến thư mục 'uploads' ở gốc project,
    // khớp hoàn toàn với cấu hình trong FileUploadConfig.
    private final Path rootLocation;

    public FileStorageService() {
        this.rootLocation = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
        log.info("File storage location set to: {}", this.rootLocation);
    }

    public String storeFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        try {
            // Tạo đường dẫn đến thư mục con (ví dụ: /project/uploads/reviews)
            Path destinationDir = this.rootLocation.resolve(subDir).normalize();

            if (!Files.exists(destinationDir)) {
                Files.createDirectories(destinationDir);
            }

            // Tạo tên file duy nhất
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
            Path destinationFile = destinationDir.resolve(uniqueFilename);

            // Sao chép file
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về URL truy cập web
            return "/uploads/" + subDir + "/" + uniqueFilename;

        } catch (IOException e) {
            log.error("Failed to store file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }
}