package com.example.isp392.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderDTO {
    // Shipping address details
    private String existingAddressId;
    private String recipientName;
    private String recipientPhone;
    private String company;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private Integer addressType;
    private Boolean saveAddress;
    
    // Payment details
    private String paymentMethod;
    
    // Order details
    private String notes;
    private String discountCode;
    private String promotionCode; // Added for promotion tracking
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Cart items for payment reservation
    private List<CartItemDTO> selectedItems;
} 