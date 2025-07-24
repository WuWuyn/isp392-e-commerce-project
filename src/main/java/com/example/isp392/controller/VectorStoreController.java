package com.example.isp392.controller;

import com.example.isp392.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller để quản lý Vector Store
 * Chỉ admin mới có thể truy cập
 */
@Controller
public class VectorStoreController {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreController.class);

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * Admin Vector Store Management Page
     */
    @GetMapping("/admin/vectorstore")
    @PreAuthorize("hasRole('ADMIN')")
    public String vectorStorePage() {
        return "admin/vectorstore";
    }

    /**
     * Load tất cả sách vào Vector Store
     */
    @PostMapping("/api/admin/vectorstore/load-books")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadBooks() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!vectorStoreService.isVectorStoreAvailable()) {
                response.put("success", false);
                response.put("message", "Vector Store không khả dụng. Hãy kiểm tra ChromaDB.");
                return ResponseEntity.badRequest().body(response);
            }

            vectorStoreService.loadBooksToVectorStore();
            
            int documentCount = vectorStoreService.getDocumentCount();
            
            response.put("success", true);
            response.put("message", "Đã load sách vào Vector Store thành công");
            response.put("documentCount", documentCount);
            
            logger.info("Admin đã load {} documents vào Vector Store", documentCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Lỗi khi load sách vào Vector Store: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Kiểm tra trạng thái Vector Store
     */
    @GetMapping("/api/admin/vectorstore/status")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isAvailable = vectorStoreService.isVectorStoreAvailable();
        int documentCount = isAvailable ? vectorStoreService.getDocumentCount() : 0;
        
        response.put("available", isAvailable);
        response.put("documentCount", documentCount);
        
        if (isAvailable) {
            response.put("message", "Vector Store đang hoạt động");
        } else {
            response.put("message", "Vector Store không khả dụng");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa tất cả documents khỏi Vector Store
     */
    @DeleteMapping("/api/admin/vectorstore/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearVectorStore() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!vectorStoreService.isVectorStoreAvailable()) {
                response.put("success", false);
                response.put("message", "Vector Store không khả dụng");
                return ResponseEntity.badRequest().body(response);
            }

            // Note: Clear all functionality not implemented yet
            // Would need to implement logic to clear all documents

            response.put("success", true);
            response.put("message", "Clear functionality will be implemented later");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Lỗi khi clear Vector Store: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
