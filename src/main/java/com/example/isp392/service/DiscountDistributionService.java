package com.example.isp392.service;

import com.example.isp392.model.Order;
import com.example.isp392.model.Promotion;
import com.example.isp392.service.PromotionCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for distributing discount amounts across multiple orders proportionally
 */
@Service
public class DiscountDistributionService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountDistributionService.class);
    private final PromotionCalculationService promotionCalculationService;

    public DiscountDistributionService(PromotionCalculationService promotionCalculationService) {
        this.promotionCalculationService = promotionCalculationService;
    }

    /**
     * Result class for discount distribution
     */
    public static class DiscountDistributionResult {
        private final BigDecimal totalDiscountAmount;
        private final String promotionCode;
        private final List<Order> ordersWithDiscounts;

        public DiscountDistributionResult(BigDecimal totalDiscountAmount, String promotionCode, List<Order> ordersWithDiscounts) {
            this.totalDiscountAmount = totalDiscountAmount;
            this.promotionCode = promotionCode;
            this.ordersWithDiscounts = ordersWithDiscounts;
        }

        public BigDecimal getTotalDiscountAmount() { return totalDiscountAmount; }
        public String getPromotionCode() { return promotionCode; }
        public List<Order> getOrdersWithDiscounts() { return ordersWithDiscounts; }
    }

    /**
     * Distribute discount amount across multiple orders proportionally based on their subtotals
     * 
     * @param orders List of orders to distribute discount across
     * @param totalDiscountAmount Total discount amount to distribute
     * @param promotionCode Promotion code that was applied
     * @return DiscountDistributionResult with updated orders
     */
    public DiscountDistributionResult distributeDiscount(List<Order> orders, BigDecimal totalDiscountAmount, String promotionCode) {
        logger.info("Distributing discount of {} across {} orders using promotion code: {}", 
                   totalDiscountAmount, orders.size(), promotionCode);

        if (orders.isEmpty() || totalDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("No orders to distribute discount to or discount amount is zero/negative");
            return new DiscountDistributionResult(BigDecimal.ZERO, promotionCode, orders);
        }

        // Calculate total subtotal across all orders
        BigDecimal totalSubtotal = orders.stream()
                .map(Order::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Total subtotal is zero or negative, cannot distribute discount");
            return new DiscountDistributionResult(BigDecimal.ZERO, promotionCode, orders);
        }

        logger.info("Total subtotal across all orders: {}", totalSubtotal);

        // Distribute discount proportionally
        BigDecimal distributedAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            BigDecimal orderSubtotal = order.getSubTotal();
            BigDecimal orderDiscountAmount;

            if (i == orders.size() - 1) {
                // For the last order, assign remaining discount to avoid rounding errors
                orderDiscountAmount = totalDiscountAmount.subtract(distributedAmount);
            } else {
                // Calculate proportional discount for this order
                BigDecimal proportion = orderSubtotal.divide(totalSubtotal, 10, RoundingMode.HALF_UP);
                orderDiscountAmount = totalDiscountAmount.multiply(proportion).setScale(0, RoundingMode.HALF_UP);
            }

            // Ensure discount doesn't exceed order subtotal
            if (orderDiscountAmount.compareTo(orderSubtotal) > 0) {
                orderDiscountAmount = orderSubtotal;
            }

            // Apply discount to order
            order.setDiscountAmount(orderDiscountAmount);
            order.setDiscountCode(promotionCode);
            
            // Recalculate total amount: subtotal + shipping - discount
            BigDecimal newTotalAmount = order.getSubTotal()
                    .add(order.getShippingFee())
                    .subtract(orderDiscountAmount);
            order.setTotalAmount(newTotalAmount);

            distributedAmount = distributedAmount.add(orderDiscountAmount);

            logger.info("Order {} - Subtotal: {}, Discount: {}, New Total: {}", 
                       order.getOrderId(), orderSubtotal, orderDiscountAmount, newTotalAmount);
        }

        logger.info("Successfully distributed total discount of {} across {} orders. Actual distributed: {}", 
                   totalDiscountAmount, orders.size(), distributedAmount);

        return new DiscountDistributionResult(distributedAmount, promotionCode, orders);
    }

    /**
     * Apply promotion to multiple orders with proper distribution
     * 
     * @param orders List of orders to apply promotion to
     * @param promotion Promotion to apply
     * @param totalOrderValue Total value across all orders for promotion calculation
     * @return DiscountDistributionResult with applied discounts
     */
    public DiscountDistributionResult applyPromotionToOrders(List<Order> orders, Promotion promotion, BigDecimal totalOrderValue) {
        logger.info("Applying promotion {} to {} orders with total value: {}", 
                   promotion.getCode(), orders.size(), totalOrderValue);

        // Calculate total discount amount based on promotion type
        BigDecimal totalDiscountAmount;
        
        if (promotion.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
            // Calculate percentage discount
            totalDiscountAmount = totalOrderValue.multiply(promotion.getDiscountValue().divide(new BigDecimal(100)));
            
            // Apply max discount amount if applicable
            if (promotion.getMaxDiscountAmount() != null && 
                totalDiscountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                totalDiscountAmount = promotion.getMaxDiscountAmount();
            }
        } else {
            // Fixed amount discount
            totalDiscountAmount = promotion.getDiscountValue();
            
            // Ensure discount doesn't exceed total order value
            if (totalDiscountAmount.compareTo(totalOrderValue) > 0) {
                totalDiscountAmount = totalOrderValue;
            }
        }

        logger.info("Calculated total discount amount: {} for promotion: {}", totalDiscountAmount, promotion.getCode());

        // Distribute the discount across orders
        return distributeDiscount(orders, totalDiscountAmount, promotion.getCode());
    }



    /**
     * Enhanced apply promotion method that handles both PERCENTAGE_DISCOUNT and FIXED_AMOUNT_DISCOUNT
     * Applies the 3-step business logic and then distributes the discount proportionally
     *
     * @param orders List of orders to apply promotion to
     * @param promotion Promotion to apply
     * @param totalOrderValue Total value across all orders (subtotal + shipping)
     * @return DiscountDistributionResult with applied discounts
     */
    public DiscountDistributionResult applyPromotionToOrdersEnhanced(List<Order> orders, Promotion promotion, BigDecimal totalOrderValue) {
        logger.info("Applying promotion {} of type {} to {} orders with total value: {}",
                   promotion.getCode(), promotion.getPromotionType(), orders.size(), totalOrderValue);

        // Use the enhanced calculation service that follows the 3-step business logic
        return applyPromotionToOrders(orders, promotion, totalOrderValue);
    }
}
