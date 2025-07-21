package com.example.isp392.service;

import com.example.isp392.dto.DiscountPreviewRequest;
import com.example.isp392.dto.DiscountPreviewResponse;
import com.example.isp392.dto.CheckoutDiscountBreakdown;
import com.example.isp392.model.Promotion;
import com.example.isp392.repository.PromotionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling promotion preview calculations
 * Provides real-time discount calculations without applying the promotion
 */
@Service
public class PromotionPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionPreviewService.class);

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionCalculationService promotionCalculationService;

    @Autowired
    private DiscountDistributionService discountDistributionService;

    /**
     * Calculate discount preview for a promotion code
     */
    public DiscountPreviewResponse calculateDiscountPreview(DiscountPreviewRequest request) {
        logger.info("Calculating discount preview for code: {}", request.getPromoCode());

        DiscountPreviewResponse response = new DiscountPreviewResponse();
        response.setPromoCode(request.getPromoCode());

        // Validate promotion code
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(request.getPromoCode());
        if (promotionOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Mã giảm giá không tồn tại hoặc đã hết hạn");
            return response;
        }

        Promotion promotion = promotionOpt.get();

        // Check if promotion is still valid
        if (!isPromotionValid(promotion)) {
            response.setSuccess(false);
            response.setMessage("Mã giảm giá đã hết hạn hoặc đã hết lượt sử dụng");
            return response;
        }

        // Calculate total order value
        BigDecimal totalOrderValue = calculateTotalOrderValue(request.getOrders());
        
        // Use existing promotion calculation service
        var calculationResult = promotionCalculationService.calculateDiscount(promotion, totalOrderValue);
        
        if (calculationResult.getDiscountAmount().compareTo(BigDecimal.ZERO) == 0) {
            response.setSuccess(false);
            response.setMessage("Đơn hàng không đủ điều kiện áp dụng mã giảm giá");
            return response;
        }

        // Build successful response
        response.setSuccess(true);
        response.setMessage("Mã giảm giá hợp lệ");
        response.setPromotionName(promotion.getName());
        response.setPromotionType(promotion.getPromotionType().name());
        response.setTotalDiscount(calculationResult.getDiscountAmount());
        response.setTotalOriginalAmount(totalOrderValue);
        response.setTotalDiscountedAmount(calculationResult.getFinalTotal());

        // Calculate per-order breakdown
        List<DiscountPreviewResponse.OrderDiscountBreakdown> orderBreakdowns = 
            calculateOrderBreakdowns(request.getOrders(), calculationResult.getDiscountAmount(), totalOrderValue);
        response.setOrders(orderBreakdowns);
        
        response.calculateTotalSavings();

        logger.info("Discount preview calculated successfully. Total discount: {}", calculationResult.getDiscountAmount());
        return response;
    }

    /**
     * Validate promotion code without full calculation
     */
    public boolean validatePromotionCode(String promoCode) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(promoCode);
        return promotionOpt.isPresent() && isPromotionValid(promotionOpt.get());
    }

    /**
     * Get available promotions for display
     */
    public List<Promotion> getAvailablePromotions() {
        // Use existing method to find site-wide promotions and filter by date
        List<Promotion> siteWidePromotions = promotionRepository.findSiteWidePromotions();
        LocalDateTime now = LocalDateTime.now();

        return siteWidePromotions.stream()
            .filter(p -> (p.getStartDate() == null || !now.isBefore(p.getStartDate())) &&
                        (p.getEndDate() == null || !now.isAfter(p.getEndDate())))
            .collect(Collectors.toList());
    }

    /**
     * Check if promotion is currently valid
     */
    private boolean isPromotionValid(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check date validity
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return false;
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return false;
        }
        
        // Check usage limits
        if (promotion.getTotalUsageLimit() != null && 
            promotion.getCurrentUsageCount() != null &&
            promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
            return false;
        }
        
        return promotion.getIsActive();
    }

    /**
     * Calculate total order value from request
     */
    private BigDecimal calculateTotalOrderValue(List<DiscountPreviewRequest.OrderPreviewItem> orders) {
        return orders.stream()
            .filter(order -> {
                // Filter out orders with zero or null subtotal
                BigDecimal subtotal = order.getSubtotal();
                boolean isValid = subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0;
                if (!isValid) {
                    logger.warn("Filtering out invalid order: Shop={}, Subtotal={}",
                               order.getShopName(), subtotal);
                }
                return isValid;
            })
            .map(order -> order.getSubtotal().add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate discount breakdown for each order
     */
    private List<DiscountPreviewResponse.OrderDiscountBreakdown> calculateOrderBreakdowns(
            List<DiscountPreviewRequest.OrderPreviewItem> orders, 
            BigDecimal totalDiscount, 
            BigDecimal totalOrderValue) {
        
        List<DiscountPreviewResponse.OrderDiscountBreakdown> breakdowns = new ArrayList<>();
        
        for (DiscountPreviewRequest.OrderPreviewItem order : orders) {
            DiscountPreviewResponse.OrderDiscountBreakdown breakdown = new DiscountPreviewResponse.OrderDiscountBreakdown();
            
            BigDecimal orderTotal = order.getSubtotal().add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
            
            // Calculate proportional discount for this order
            BigDecimal proportion = orderTotal.divide(totalOrderValue, 4, RoundingMode.HALF_UP);
            BigDecimal orderDiscount = totalDiscount.multiply(proportion).setScale(0, RoundingMode.HALF_UP);
            
            breakdown.setOrderId(order.getOrderId());
            breakdown.setShopName(order.getShopName());
            breakdown.setOriginalTotal(orderTotal);
            breakdown.setDiscountAmount(orderDiscount);
            breakdown.setDiscountedTotal(orderTotal.subtract(orderDiscount));
            breakdown.setSavingsAmount(orderDiscount);
            breakdown.setEligible(true);
            
            // Calculate discount percentage for this order
            if (orderTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = orderDiscount.multiply(new BigDecimal("100"))
                    .divide(orderTotal, 2, RoundingMode.HALF_UP);
                breakdown.setDiscountPercentage(percentage.toString() + "%");
            } else {
                breakdown.setDiscountPercentage("0%");
            }
            
            breakdowns.add(breakdown);
        }
        
        return breakdowns;
    }

    /**
     * Calculate detailed checkout breakdown with item-level information
     */
    public CheckoutDiscountBreakdown calculateCheckoutBreakdown(DiscountPreviewRequest request) {
        logger.info("Calculating detailed checkout breakdown for code: {}", request.getPromoCode());

        CheckoutDiscountBreakdown breakdown = new CheckoutDiscountBreakdown();
        breakdown.setPromoCode(request.getPromoCode());

        // Validate input orders
        if (request.getOrders() == null || request.getOrders().isEmpty()) {
            breakdown.setSuccess(false);
            breakdown.setMessage("Không có đơn hàng để áp dụng mã giảm giá");
            return breakdown;
        }

        // Filter out invalid orders (zero subtotal)
        List<DiscountPreviewRequest.OrderPreviewItem> validOrders = request.getOrders().stream()
            .filter(order -> {
                BigDecimal subtotal = order.getSubtotal();
                boolean isValid = subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0;
                if (!isValid) {
                    logger.warn("Filtering out invalid order in breakdown: Shop={}, Subtotal={}",
                               order.getShopName(), subtotal);
                }
                return isValid;
            })
            .collect(Collectors.toList());

        if (validOrders.isEmpty()) {
            breakdown.setSuccess(false);
            breakdown.setMessage("Không có đơn hàng hợp lệ để áp dụng mã giảm giá");
            return breakdown;
        }

        // Update request with valid orders only
        request.setOrders(validOrders);

        // Validate promotion code
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(request.getPromoCode());
        if (promotionOpt.isEmpty()) {
            breakdown.setSuccess(false);
            breakdown.setMessage("Mã giảm giá không tồn tại hoặc đã hết hạn");
            return breakdown;
        }

        Promotion promotion = promotionOpt.get();

        // Check if promotion is still valid
        if (!isPromotionValid(promotion)) {
            breakdown.setSuccess(false);
            breakdown.setMessage("Mã giảm giá đã hết hạn hoặc đã hết lượt sử dụng");
            return breakdown;
        }

        // Calculate total order value from valid orders only
        BigDecimal totalOrderValue = calculateTotalOrderValue(validOrders);
        BigDecimal totalSubtotal = validOrders.stream()
            .map(DiscountPreviewRequest.OrderPreviewItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalShipping = validOrders.stream()
            .map(order -> order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Use existing promotion calculation service
        var calculationResult = promotionCalculationService.calculateDiscount(promotion, totalOrderValue);

        if (calculationResult.getDiscountAmount().compareTo(BigDecimal.ZERO) == 0) {
            breakdown.setSuccess(false);
            breakdown.setMessage("Đơn hàng không đủ điều kiện áp dụng mã giảm giá");
            return breakdown;
        }

        // Build successful response
        breakdown.setSuccess(true);
        breakdown.setMessage("Mã giảm giá được áp dụng thành công");
        breakdown.setPromotionName(promotion.getName());
        breakdown.setPromotionType(promotion.getPromotionType().name());
        breakdown.setTotalDiscount(calculationResult.getDiscountAmount());
        breakdown.setTotalOriginalAmount(totalOrderValue);
        breakdown.setTotalSubtotal(totalSubtotal);
        breakdown.setTotalShipping(totalShipping);
        breakdown.setTotalDiscountedAmount(calculationResult.getFinalTotal());

        // Calculate detailed per-order breakdown
        List<CheckoutDiscountBreakdown.OrderCheckoutBreakdown> orderBreakdowns =
            calculateDetailedOrderBreakdowns(request.getOrders(), calculationResult.getDiscountAmount(), totalOrderValue);
        breakdown.setOrders(orderBreakdowns);

        breakdown.calculateTotalSavings();

        logger.info("Detailed checkout breakdown calculated successfully. Total discount: {}", calculationResult.getDiscountAmount());
        return breakdown;
    }

    /**
     * Get promotion details by code for display
     */
    public Map<String, Object> getPromotionDetails(String code) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(code);

        if (promotionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promotion not found or inactive");
        }

        Promotion promotion = promotionOpt.get();
        Map<String, Object> details = new HashMap<>();

        details.put("code", promotion.getCode());
        details.put("description", promotion.getDescription());
        details.put("type", promotion.getPromotionType().toString());
        details.put("discountValue", promotion.getDiscountValue());
        details.put("maxDiscountAmount", promotion.getMaxDiscountAmount());
        details.put("minOrderAmount", promotion.getMinOrderValue());
        details.put("startDate", promotion.getStartDate());
        details.put("endDate", promotion.getEndDate());
        details.put("currentUsageCount", promotion.getCurrentUsageCount());
        details.put("totalUsageLimit", promotion.getTotalUsageLimit());
        details.put("isValid", isPromotionValid(promotion));

        return details;
    }

    /**
     * Calculate detailed discount breakdown for each order with item information
     */
    private List<CheckoutDiscountBreakdown.OrderCheckoutBreakdown> calculateDetailedOrderBreakdowns(
            List<DiscountPreviewRequest.OrderPreviewItem> orders,
            BigDecimal totalDiscount,
            BigDecimal totalOrderValue) {

        List<CheckoutDiscountBreakdown.OrderCheckoutBreakdown> breakdowns = new ArrayList<>();

        for (DiscountPreviewRequest.OrderPreviewItem order : orders) {
            // Skip orders with zero or null subtotal
            if (order.getSubtotal() == null || order.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Skipping order with zero subtotal in breakdown: Shop={}", order.getShopName());
                continue;
            }

            CheckoutDiscountBreakdown.OrderCheckoutBreakdown breakdown = new CheckoutDiscountBreakdown.OrderCheckoutBreakdown();

            BigDecimal orderTotal = order.getSubtotal().add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);

            // Calculate proportional discount for this order
            BigDecimal proportion = orderTotal.divide(totalOrderValue, 4, RoundingMode.HALF_UP);
            BigDecimal orderDiscount = totalDiscount.multiply(proportion).setScale(0, RoundingMode.HALF_UP);

            breakdown.setOrderId(order.getOrderId());
            breakdown.setShopName(order.getShopName());
            breakdown.setSubtotal(order.getSubtotal());
            breakdown.setShippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
            breakdown.setOriginalTotal(orderTotal);
            breakdown.setDiscountAmount(orderDiscount);
            breakdown.setDiscountedTotal(orderTotal.subtract(orderDiscount));
            breakdown.setSavingsAmount(orderDiscount);
            breakdown.setEligible(true);

            // Calculate discount percentage for this order
            if (orderTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = orderDiscount.multiply(new BigDecimal("100"))
                    .divide(orderTotal, 2, RoundingMode.HALF_UP);
                breakdown.setDiscountPercentage(percentage.toString() + "%");
            } else {
                breakdown.setDiscountPercentage("0%");
            }

            // Add item details
            if (order.getItems() != null) {
                List<CheckoutDiscountBreakdown.ItemDetail> itemDetails = new ArrayList<>();
                for (DiscountPreviewRequest.CartItemPreview item : order.getItems()) {
                    CheckoutDiscountBreakdown.ItemDetail itemDetail = new CheckoutDiscountBreakdown.ItemDetail();
                    itemDetail.setBookId(item.getBookId());
                    itemDetail.setBookTitle(item.getBookTitle());
                    itemDetail.setQuantity(item.getQuantity());
                    itemDetail.setUnitPrice(item.getUnitPrice());
                    itemDetail.setTotalPrice(item.getTotalPrice());
                    itemDetails.add(itemDetail);
                }
                breakdown.setItems(itemDetails);
            }

            breakdowns.add(breakdown);
        }

        return breakdowns;
    }

    /**
     * Get applied promotion from session/storage for checkout
     */
    public CheckoutDiscountBreakdown getAppliedPromotionForCheckout(String promoCode, List<DiscountPreviewRequest.OrderPreviewItem> orders) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            CheckoutDiscountBreakdown breakdown = new CheckoutDiscountBreakdown();
            breakdown.setSuccess(false);
            breakdown.setMessage("Không có mã giảm giá được áp dụng");
            return breakdown;
        }

        DiscountPreviewRequest request = new DiscountPreviewRequest();
        request.setPromoCode(promoCode);
        request.setOrders(orders);
        request.setTotalOrderValue(calculateTotalOrderValue(orders));

        return calculateCheckoutBreakdown(request);
    }
}
