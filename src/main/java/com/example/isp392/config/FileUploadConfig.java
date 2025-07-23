package com.example.isp392.config;

import jakarta.servlet.MultipartConfigElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(FileUploadConfig.class);

    // Giữ nguyên các hằng số của bạn
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 1200;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(5));
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        return factory.createMultipartConfig();
    }

    /**
     * Cấu hình Resource Handler để phục vụ file từ thư mục /uploads/ ở gốc project
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối đến thư mục gốc của project
        String projectDir = System.getProperty("user.dir");

        // Tạo đường dẫn đến thư mục 'uploads' trong project.
        Path uploadPath = Paths.get(projectDir, "uploads");
        String uploadDir = uploadPath.toFile().getAbsolutePath();

        // ** THAY ĐỔI QUAN TRỌNG NHẤT **
        // Ánh xạ URL /uploads/** đến thư mục vật lý mà chúng ta sẽ lưu file
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadDir + "/");

        log.info("Serving uploaded files from: {}", "file:/" + uploadDir + "/");
    }

    // Bạn có thể giữ lại phương thức createUploadDirectories nếu muốn,
    // nhưng FileStorageService đã có thể tự tạo thư mục rồi nên không bắt buộc.
    // Nếu giữ lại, hãy đảm bảo nó cũng tạo thư mục ở gốc project.

    // Phương thức resizeImage của bạn đã rất tốt, không cần thay đổi
    public static byte[] resizeImage(byte[] originalImageData, String format) throws IOException {
        // ... giữ nguyên code của bạn
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImageData)) {
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                log.error("Failed to read image data");
                return originalImageData; // Return original if can't be read
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // Check if resizing is needed
            if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
                return originalImageData; // No need to resize
            }

            // Calculate new dimensions while maintaining aspect ratio
            int newWidth, newHeight;

            if (originalWidth > originalHeight) {
                // Landscape image
                newWidth = MAX_WIDTH;
                newHeight = (int) Math.round(originalHeight * ((double) MAX_WIDTH / originalWidth));
            } else {
                // Portrait image
                newHeight = MAX_HEIGHT;
                newWidth = (int) Math.round(originalWidth * ((double) MAX_HEIGHT / originalHeight));
            }

            // Create new image with calculated dimensions
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();

            // Set rendering hints for better quality
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw resized image
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            // Convert back to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ImageIO.write(resizedImage, format, outputStream);

            if (!success) {
                log.error("Failed to write resized image");
                return originalImageData; // Return original if write fails
            }

            log.info("Image resized from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error resizing image: {}", e.getMessage());
            return originalImageData; // Return original on error
        }
    }
}