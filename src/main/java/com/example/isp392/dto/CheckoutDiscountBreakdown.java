package com.example.isp392.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed discount breakdown for checkout page
 */
@Data
public class CheckoutDiscountBreakdown {
    private boolean success;
    private String message;
    private String promoCode;
    private String promotionName;
    private String promotionType;
    
    // Total amounts
    private BigDecimal totalOriginalAmount;
    private BigDecimal totalSubtotal;
    private BigDecimal totalShipping;
    private BigDecimal totalDiscount;
    private BigDecimal totalDiscountedAmount;
    private BigDecimal totalSavings;
    
    // Detailed breakdown by order/shop
    private List<OrderCheckoutBreakdown> orders;

    @Data
    public static class OrderCheckoutBreakdown {
        private Integer orderId;
        private String shopName;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal originalTotal;
        private BigDecimal discountAmount;
        private BigDecimal discountedTotal;
        private BigDecimal savingsAmount;
        private String discountPercentage;
        private boolean eligible;
        private String ineligibilityReason;
        
        // Item details for this order
        private List<ItemDetail> items;
    }

    @Data
    public static class ItemDetail {
        private Integer bookId;
        private String bookTitle;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    /**
     * Calculate total savings across all orders
     */
    public void calculateTotalSavings() {
        if (orders != null) {
            this.totalSavings = orders.stream()
                .map(OrderCheckoutBreakdown::getSavingsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Check if any orders are eligible for discount
     */
    public boolean hasEligibleOrders() {
        return orders != null && orders.stream().anyMatch(OrderCheckoutBreakdown::isEligible);
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

    /**
     * Get total number of items across all orders
     */
    public Integer getTotalItemCount() {
        if (orders == null) return 0;
        
        return orders.stream()
            .mapToInt(order -> order.getItems() != null ? 
                order.getItems().stream().mapToInt(ItemDetail::getQuantity).sum() : 0)
            .sum();
    }

    /**
     * Get breakdown summary for display
     */
    public String getBreakdownSummary() {
        if (!success || orders == null) return "";
        
        int shopCount = orders.size();
        int itemCount = getTotalItemCount();
        
        return String.format("%d shop(s), %d item(s)", shopCount, itemCount);
    }
}
