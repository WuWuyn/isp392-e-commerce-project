package com.example.isp392.service;

import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Centralized service for promotion validation and calculation logic.
 * Consolidates all promotion-related business logic from cart and checkout components.
 */
@Service
public class PromotionCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionCalculationService.class);

    @Autowired
    private PromotionService promotionService;

    /**
     * Result class for promotion validation
     */
    public static class PromotionValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Promotion promotion;

        public PromotionValidationResult(boolean valid, String errorMessage, Promotion promotion) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.promotion = promotion;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public Promotion getPromotion() { return promotion; }

        public static PromotionValidationResult success(Promotion promotion) {
            return new PromotionValidationResult(true, null, promotion);
        }

        public static PromotionValidationResult error(String message) {
            return new PromotionValidationResult(false, message, null);
        }
    }

    /**
     * Result class for discount calculation
     */
    public static class DiscountCalculationResult {
        private final BigDecimal discountAmount;
        private final BigDecimal finalTotal;

        public DiscountCalculationResult(BigDecimal discountAmount, BigDecimal finalTotal) {
            this.discountAmount = discountAmount;
            this.finalTotal = finalTotal;
        }

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getFinalTotal() { return finalTotal; }
    }

    /**
     * Validate if a promotion can be applied by a user for a given order total
     */
    public PromotionValidationResult validatePromotion(String promotionCode, User user, BigDecimal orderTotal) {
        logger.info("Validating promotion code: {} for user: {}", promotionCode, user.getUserId());

        // Find the promotion by code
        Optional<Promotion> promotionOpt = promotionService.findByCode(promotionCode);
        if (promotionOpt.isEmpty()) {
            return PromotionValidationResult.error("Mã giảm giá không tồn tại");
        }

        Promotion promotion = promotionOpt.get();

        // Check if promotion is active
        if (!promotion.getIsActive()) {
            return PromotionValidationResult.error("Mã giảm giá đã hết hạn");
        }

        // Check if promotion is within valid date range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            return PromotionValidationResult.error("Mã giảm giá không trong thời gian sử dụng");
        }

        // Check usage limit per user if applicable
        if (promotion.getUsageLimitPerUser() != null && promotion.getUsageLimitPerUser() > 0) {
            int userUsageCount = promotionService.getUserUsageCount(user.getUserId(), promotion.getPromotionId());
            if (userUsageCount >= promotion.getUsageLimitPerUser()) {
                return PromotionValidationResult.error("Bạn đã sử dụng hết số lần cho phép của mã giảm giá này");
            }
        }

        // Check total usage limit if applicable
        if (promotion.getTotalUsageLimit() != null && promotion.getTotalUsageLimit() > 0) {
            if (promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
                return PromotionValidationResult.error("Mã giảm giá đã hết lượt sử dụng");
            }
        }

        // Check minimum order value if applicable
        if (promotion.getMinOrderValue() != null && orderTotal.compareTo(promotion.getMinOrderValue()) < 0) {
            return PromotionValidationResult.error("Giá trị đơn hàng tối thiểu để sử dụng mã này là " + 
                                                 promotion.getMinOrderValue().toString() + " VND");
        }

        return PromotionValidationResult.success(promotion);
    }

    /**
     * Calculate discount amount for a given promotion and order total
     */
    /**
     * Calculate discount following the 3-step business logic:
     * Step 1: Check minimum order value eligibility
     * Step 2: Calculate potential discount amount
     * Step 3: Apply maximum discount cap
     *
     * @param promotion The promotion to apply
     * @param orderTotal Total order value (subtotal + shipping)
     * @return DiscountCalculationResult with calculated discount
     */
    public DiscountCalculationResult calculateDiscount(Promotion promotion, BigDecimal orderTotal) {
        logger.info("Starting 3-step discount calculation for promotion: {} with order total: {}",
                   promotion.getCode(), orderTotal);

        // STEP 1: Check minimum order value eligibility
        if (promotion.getMinOrderValue() != null &&
            orderTotal.compareTo(promotion.getMinOrderValue()) < 0) {
            logger.info("Order total {} is below minimum required {} for promotion: {}",
                       orderTotal, promotion.getMinOrderValue(), promotion.getCode());
            return new DiscountCalculationResult(BigDecimal.ZERO, orderTotal);
        }

        BigDecimal discountAmount;

        // STEP 2: Calculate potential discount amount
        if (promotion.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
            // Calculate percentage discount
            discountAmount = orderTotal.multiply(promotion.getDiscountValue().divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
            logger.info("Step 2 - Calculated potential percentage discount: {} ({}% of {})",
                       discountAmount, promotion.getDiscountValue(), orderTotal);
        } else if (promotion.getPromotionType() == Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT) {
            // Fixed amount discount
            discountAmount = promotion.getDiscountValue();
            logger.info("Step 2 - Fixed amount discount: {}", discountAmount);

            // Ensure discount doesn't exceed order total
            if (discountAmount.compareTo(orderTotal) > 0) {
                discountAmount = orderTotal;
                logger.info("Fixed discount capped to order total: {}", discountAmount);
            }
        } else {
            logger.warn("Unknown promotion type: {} for promotion: {}",
                       promotion.getPromotionType(), promotion.getCode());
            return new DiscountCalculationResult(BigDecimal.ZERO, orderTotal);
        }

        // STEP 3: Apply maximum discount cap (only for percentage discounts)
        if (promotion.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT &&
            promotion.getMaxDiscountAmount() != null &&
            discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {

            BigDecimal originalDiscount = discountAmount;
            discountAmount = promotion.getMaxDiscountAmount();
            logger.info("Step 3 - Discount capped: {} -> {} (max: {})",
                       originalDiscount, discountAmount, promotion.getMaxDiscountAmount());
        }

        BigDecimal finalTotal = orderTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        logger.info("Final discount calculation result - Discount: {}, Final total: {} (Original: {})",
                   discountAmount, finalTotal, orderTotal);

        return new DiscountCalculationResult(discountAmount, finalTotal);
    }



    /**
     * Apply promotion to an order - validates and calculates discount in one operation
     */
    public PromotionApplicationResult applyPromotion(String promotionCode, User user, BigDecimal orderTotal) {
        logger.info("Applying promotion: {} for user: {} with order total: {}",
                   promotionCode, user.getEmail(), orderTotal);

        // Validate promotion
        PromotionValidationResult validationResult = validatePromotion(promotionCode, user, orderTotal);
        if (!validationResult.isValid()) {
            logger.warn("Promotion validation failed: {}", validationResult.getErrorMessage());
            return new PromotionApplicationResult(false, validationResult.getErrorMessage(),
                                                BigDecimal.ZERO, orderTotal, null);
        }

        logger.info("Promotion validation successful for: {}", promotionCode);

        // Calculate discount
        DiscountCalculationResult discountResult = calculateDiscount(validationResult.getPromotion(), orderTotal);

        logger.info("Discount calculation result: Amount: {}, Final Total: {}",
                   discountResult.getDiscountAmount(), discountResult.getFinalTotal());

        return new PromotionApplicationResult(true, null, discountResult.getDiscountAmount(),
                                            discountResult.getFinalTotal(), validationResult.getPromotion());
    }

    /**
     * Result class for promotion application
     */
    public static class PromotionApplicationResult {
        private final boolean success;
        private final String errorMessage;
        private final BigDecimal discountAmount;
        private final BigDecimal finalTotal;
        private final Promotion promotion;

        public PromotionApplicationResult(boolean success, String errorMessage, BigDecimal discountAmount, 
                                        BigDecimal finalTotal, Promotion promotion) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.discountAmount = discountAmount;
            this.finalTotal = finalTotal;
            this.promotion = promotion;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getFinalTotal() { return finalTotal; }
        public Promotion getPromotion() { return promotion; }
    }

    /**
     * Record promotion usage after successful order completion
     */
    public void recordPromotionUsage(String promotionCode, Integer userId) {
        logger.info("Recording promotion usage for code: {} by user: {}", promotionCode, userId);
        promotionService.updatePromotionUsage(promotionCode, userId);
    }

    /**
     * Record promotion usage with customer order details
     */
    public void recordPromotionUsage(String promotionCode, Integer userId, Integer customerOrderId, BigDecimal discountAmount) {
        logger.info("Recording promotion usage for code: {} by user: {} with customer order: {} and discount: {}",
                   promotionCode, userId, customerOrderId, discountAmount);
        promotionService.updatePromotionUsage(promotionCode, userId, customerOrderId, discountAmount);
    }
}
