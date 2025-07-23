package com.example.isp392.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for discount preview calculation
 */
@Data
public class DiscountPreviewResponse {
    private boolean success;
    private String message;
    private String promoCode;
    private String promotionName;
    private String promotionType;
    private BigDecimal totalDiscount;
    private BigDecimal totalOriginalAmount;
    private BigDecimal totalDiscountedAmount;
    private BigDecimal totalSavings;
    private List<OrderDiscountBreakdown> orders;

    @Data
    public static class OrderDiscountBreakdown {
        private Integer orderId;
        private String shopName;
        private BigDecimal originalTotal;
        private BigDecimal discountAmount;
        private BigDecimal discountedTotal;
        private BigDecimal savingsAmount;
        private String discountPercentage;
        private boolean eligible;
        private String ineligibilityReason;
    }

    /**
     * Calculate total savings across all orders
     */
    public void calculateTotalSavings() {
        if (orders != null) {
            this.totalSavings = orders.stream()
                .map(OrderDiscountBreakdown::getSavingsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Check if any orders are eligible for discount
     */
    public boolean hasEligibleOrders() {
        return orders != null && orders.stream().anyMatch(OrderDiscountBreakdown::isEligible);
    }

    /**
     * Get formatted discount percentage for display
     */
    public String getFormattedDiscountPercentage() {
        if (totalOriginalAmount != null && totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalDiscount
                .multiply(new BigDecimal("100"))
                .divide(totalOriginalAmount, 2, BigDecimal.ROUND_HALF_UP);
            return percentage.toString() + "%";
        }
        return "0%";
    }
}
