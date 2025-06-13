package com.example.isp392.dto;

import com.example.isp392.model.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutDTO {
    // Address Information
    private Integer existingAddressId;
    private Boolean useNewAddress;
    
    // New Address Information (if useNewAddress is true)
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private String company;
    private Integer addressType; // 0: Home, 1: Company
    private Boolean saveAddress; // Whether to save this address for future use
    
    // Order Information
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String discountCode;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private String notes;
} 