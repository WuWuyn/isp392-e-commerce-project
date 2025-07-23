package com.example.isp392.controller;

import com.example.isp392.dto.DiscountPreviewRequest;
import com.example.isp392.dto.DiscountPreviewResponse;
import com.example.isp392.service.PromotionPreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling promotion code preview functionality
 * Provides real-time discount calculations without applying the promotion
 */
@RestController
@RequestMapping("/api/promotion")
public class PromotionPreviewController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionPreviewController.class);

    @Autowired
    private PromotionPreviewService promotionPreviewService;

    /**
     * Preview discount calculation for a promotion code
     * Returns detailed breakdown without applying the discount
     */
    @PostMapping("/preview")
    public ResponseEntity<DiscountPreviewResponse> previewDiscount(@RequestBody DiscountPreviewRequest request) {
        logger.info("Preview discount request for code: {}", request.getPromoCode());
        
        try {
            DiscountPreviewResponse response = promotionPreviewService.calculateDiscountPreview(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating discount preview: {}", e.getMessage(), e);
            
            DiscountPreviewResponse errorResponse = new DiscountPreviewResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Unable to calculate discount preview");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Validate promotion code without calculating discount
     * Quick validation for UI feedback
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePromotionCode(@RequestBody Map<String, String> request) {
        String promoCode = request.get("promoCode");
        logger.info("Validating promotion code: {}", promoCode);
        
        try {
            boolean isValid = promotionPreviewService.validatePromotionCode(promoCode);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", isValid,
                "message", isValid ? "Valid promotion code" : "Invalid or expired promotion code"
            ));
        } catch (Exception e) {
            logger.error("Error validating promotion code: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "valid", false,
                "message", "Unable to validate promotion code"
            ));
        }
    }

    /**
     * Get available promotions for user
     * Returns list of applicable promotions
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailablePromotions() {
        logger.info("Getting available promotions");
        
        try {
            // This could be enhanced to return user-specific promotions
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Available promotions retrieved",
                "promotions", promotionPreviewService.getAvailablePromotions()
            ));
        } catch (Exception e) {
            logger.error("Error getting available promotions: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Unable to retrieve available promotions"
            ));
        }
    }

    /**
     * Get promotion details by code
     * Returns promotion information for display
     */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getPromotionDetails(@PathVariable String code) {
        logger.info("Getting promotion details for code: {}", code);
        
        try {
            Map<String, Object> details = promotionPreviewService.getPromotionDetails(code);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "promotion", details
            ));
        } catch (Exception e) {
            logger.error("Error getting promotion details: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Promotion not found or invalid"
            ));
        }
    }
}
