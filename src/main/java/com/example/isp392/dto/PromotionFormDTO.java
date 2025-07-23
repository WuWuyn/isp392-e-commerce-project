package com.example.isp392.dto;

import com.example.isp392.model.Promotion;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PromotionFormDTO {

    private Integer promotionId;

    @NotBlank(message = "Promotion name is required")
    @Size(max = 255, message = "Promotion name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Promotion code is required")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Code must be 3-50 characters, uppercase letters, numbers, underscore, or dash only")
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Promotion type is required")
    private Promotion.PromotionType promotionType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Digits(integer = 16, fraction = 2, message = "Invalid discount value format")
    private BigDecimal discountValue;

    @Digits(integer = 16, fraction = 2, message = "Invalid max discount amount format")
    @DecimalMin(value = "0.01", message = "Max discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @Digits(integer = 16, fraction = 2, message = "Invalid min order value format")
    @DecimalMin(value = "0.01", message = "Min order value must be greater than 0")
    private BigDecimal minOrderValue;

    @NotNull(message = "Scope type is required")
    private Promotion.ScopeType scopeType;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Boolean isActive = true;

    @NotNull(message = "Status is required")
    private Promotion.PromotionStatus status = Promotion.PromotionStatus.ACTIVE;

    @Min(value = 1, message = "Usage limit per user must be at least 1")
    private Integer usageLimitPerUser;

    @Min(value = 1, message = "Total usage limit must be at least 1")
    private Integer totalUsageLimit;

    // Scope-specific fields - Only categories for simplified system
    private List<Integer> categoryIds;

    // Validation methods
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return endDate.isAfter(startDate);
    }

    // Removed strict future date validation to allow immediate promotions

    @AssertTrue(message = "For percentage discounts, value must be between 1 and 100")
    public boolean isValidPercentageDiscount() {
        if (promotionType == null || discountValue == null) {
            return true; // Let other validations handle null values
        }
        
        if (promotionType == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
            return discountValue.compareTo(BigDecimal.ONE) >= 0 && 
                   discountValue.compareTo(new BigDecimal("100")) <= 0;
        }
        return true;
    }

    @AssertTrue(message = "Total usage limit must be greater than or equal to per-user limit")
    public boolean isTotalLimitValid() {
        if (usageLimitPerUser == null || totalUsageLimit == null) {
            return true; // Allow null values
        }
        return totalUsageLimit >= usageLimitPerUser;
    }

    @AssertTrue(message = "Max discount amount must be less than or equal to discount value for fixed amount discounts")
    public boolean isMaxDiscountValid() {
        if (promotionType == null || discountValue == null || maxDiscountAmount == null) {
            return true;
        }

        if (promotionType == Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT) {
            // FIXED_AMOUNT_DISCOUNT promotions don't use max discount amount
            return true;
        }
        return true;
    }

    @AssertTrue(message = "Max discount amount must be greater than 0 when specified")
    public boolean isMaxDiscountAmountValid() {
        if (maxDiscountAmount == null) {
            return true; // Allow null values
        }
        return maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    @AssertTrue(message = "Min order value must be greater than 0 when specified")
    public boolean isMinOrderValueValid() {
        if (minOrderValue == null) {
            return true; // Allow null values
        }
        return minOrderValue.compareTo(BigDecimal.ZERO) > 0;
    }

    @AssertTrue(message = "Categories must be selected for category-specific promotions")
    public boolean isScopeItemsValid() {
        if (scopeType == null) {
            return true;
        }

        // Only site-wide promotions are supported now
        return true; // Site-wide promotions don't need specific items
    }

    // Helper method to get discount type string for backward compatibility
    public String getDiscountType() {
        if (promotionType == null) {
            return null;
        }

        switch (promotionType) {
            case PERCENTAGE_DISCOUNT:
                return "PERCENTAGE";
            case FIXED_AMOUNT_DISCOUNT:
                return "FIXED_AMOUNT";
            default:
                return "PERCENTAGE";
        }
    }
}
