package com.example.isp392.controller.api;

import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import com.example.isp392.service.PromotionService;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/discount")
public class DiscountApiController {

    private static final Logger logger = LoggerFactory.getLogger(DiscountApiController.class);
    
    private final PromotionService promotionService;
    private final UserService userService;

    public DiscountApiController(PromotionService promotionService, UserService userService) {
        this.promotionService = promotionService;
        this.userService = userService;
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal,
            Authentication authentication) {
        
        logger.info("Validating discount code: {} for subtotal: {}", code, subtotal);
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            // Get current user
            String email = authentication.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }
            User user = userOpt.get();

            // Validate discount code
            Optional<Promotion> promotionOpt = promotionService.findByCode(code);
            if (promotionOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã giảm giá không tồn tại"));
            }

            Promotion promotion = promotionOpt.get();
            
            // Check if promotion is active
            if (!promotion.getIsActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã giảm giá không còn hiệu lực"));
            }

            // Check if promotion is in valid date range
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã giảm giá không trong thời gian sử dụng"));
            }

            // Check if order meets minimum value
            if (promotion.getMinOrderValue() != null && 
                subtotal.compareTo(promotion.getMinOrderValue()) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Đơn hàng chưa đạt giá trị tối thiểu " + 
                             promotion.getMinOrderValue() + " VND"
                ));
            }

            // Calculate discount amount
            BigDecimal discountAmount;
            if ("PERCENTAGE".equals(promotion.getDiscountType())) {
                discountAmount = subtotal.multiply(promotion.getDiscountValue().divide(new BigDecimal(100)));
                
                if (promotion.getMaxDiscountAmount() != null && 
                    discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discountAmount = promotion.getMaxDiscountAmount();
                }
            } else {
                discountAmount = promotion.getDiscountValue();
                if (discountAmount.compareTo(subtotal) > 0) {
                    discountAmount = subtotal;
                }
            }

            // Return discount information
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("discountAmount", discountAmount);
            response.put("discountCode", code);
            response.put("message", "Áp dụng mã giảm giá thành công!");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating discount: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi xử lý mã giảm giá"));
        }
    }
} 