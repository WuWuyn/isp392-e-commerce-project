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

/**
 * Configuration for file uploads
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(FileUploadConfig.class);
    
    // Max width and height for optimized images
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 1200;

    /**
     * Configure multipart file uploads
     * @return MultipartConfigElement
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // Set maximum file size (5MB)
        factory.setMaxFileSize(DataSize.ofMegabytes(5));
        // Set maximum request size (10MB)
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        return factory.createMultipartConfig();
    }

    /**
     * Configure resource handlers for serving uploaded files
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Create upload directories if they don't exist
        createUploadDirectories();
        
        // Map uploaded files URLs to physical location
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/src/main/resources/static/uploads/");
    }
    
    /**
     * Create necessary upload directories
     */
    private void createUploadDirectories() {
        String baseDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
        
        // Create directories for different file types
        String[] subDirs = {"profile-pictures", "book-covers", "blog-images", "temp"};
        
        for (String dir : subDirs) {
            Path path = Paths.get(baseDir + dir);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                    log.info("Created upload directory: {}", path);
                } catch (IOException e) {
                    log.error("Failed to create directory: {}", path, e);
                }
            }
        }
    }
    
    /**
     * Utility method to resize an image while maintaining aspect ratio
     * @param originalImageData original image bytes
     * @param format image format (jpg, png, etc.)
     * @return resized image bytes
     * @throws IOException if image processing fails
     */
    public static byte[] resizeImage(byte[] originalImageData, String format) throws IOException {
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
