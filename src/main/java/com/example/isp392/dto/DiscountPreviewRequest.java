package com.example.isp392.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for discount preview calculation
 */
@Data
public class DiscountPreviewRequest {
    private String promoCode;
    private List<OrderPreviewItem> orders;
    private BigDecimal totalOrderValue;

    @Data
    public static class OrderPreviewItem {
        private Integer orderId;
        private String shopName;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal totalAmount;
        private List<CartItemPreview> items;
    }

    @Data
    public static class CartItemPreview {
        private Integer bookId;
        private String bookTitle;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
