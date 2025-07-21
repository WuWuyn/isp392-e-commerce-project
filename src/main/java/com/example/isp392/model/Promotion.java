package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotion_code", columnList = "code"),
    @Index(name = "idx_promotion_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_promotion_active", columnList = "is_active"),
    @Index(name = "idx_promotion_scope", columnList = "scope_type")
})
public class Promotion {

    /**
     * Enum for promotion types - Only percentage and fixed amount discounts
     */
    public enum PromotionType {
        PERCENTAGE_DISCOUNT("Percentage Discount"),
        FIXED_AMOUNT_DISCOUNT("Fixed Amount Discount");

        private final String displayName;

        PromotionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for promotion scope - Only site-wide promotions are supported
     */
    public enum ScopeType {
        SITE_WIDE("Site-wide");

        private final String displayName;

        ScopeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for promotion status - Simplified without DRAFT status
     */
    public enum PromotionStatus {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        EXPIRED("Expired"),
        USED_UP("Used Up");

        private final String displayName;

        PromotionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Integer promotionId;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false)
    private PromotionType promotionType;

    @Column(name = "discount_type", length = 200, nullable = false)
    private String discountType; // Keep for backward compatibility

    @Column(name = "discount_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "current_usage_count", nullable = false)
    private Integer currentUsageCount = 0;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Scope-specific relationships - Only categories for simplified system
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotion_categories",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> applicableCategories = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentUsageCount == null) {
            currentUsageCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isExpired() {
        return endDate != null && LocalDateTime.now().isAfter(endDate);
    }

    public boolean isNotStarted() {
        return startDate != null && LocalDateTime.now().isBefore(startDate);
    }

    public boolean isUsedUp() {
        return totalUsageLimit != null && currentUsageCount != null && currentUsageCount >= totalUsageLimit;
    }

    public boolean canBeUsed() {
        return Boolean.TRUE.equals(isActive) && !isExpired() && !isNotStarted() && !isUsedUp();
    }

    public double getUsagePercentage() {
        if (totalUsageLimit == null || totalUsageLimit == 0 || currentUsageCount == null) {
            return 0.0;
        }
        return (double) currentUsageCount / totalUsageLimit * 100.0;
    }

    /**
     * Check if this is a FIXED_AMOUNT_DISCOUNT promotion
     */
    public boolean isFixedAmountDiscountPromotion() {
        return promotionType == PromotionType.FIXED_AMOUNT_DISCOUNT;
    }

    /**
     * Check if this is a PERCENTAGE_DISCOUNT promotion
     */
    public boolean isPercentageDiscountPromotion() {
        return promotionType == PromotionType.PERCENTAGE_DISCOUNT;
    }

    /**
     * Validate promotion configuration based on type
     */
    public boolean isValidConfiguration() {
        if (promotionType == null) {
            return false;
        }

        switch (promotionType) {
            case PERCENTAGE_DISCOUNT:
                // Percentage discount must have a discount value between 1-100
                return discountValue != null &&
                       discountValue.compareTo(BigDecimal.ONE) >= 0 &&
                       discountValue.compareTo(new BigDecimal("100")) <= 0;

            case FIXED_AMOUNT_DISCOUNT:
                // Fixed amount discount must have a positive discount value
                return discountValue != null &&
                       discountValue.compareTo(BigDecimal.ZERO) > 0;

            default:
                return false;
        }
    }

    /**
     * Check if promotion has never been used (safe to edit all fields)
     */
    public boolean isNeverUsed() {
        return currentUsageCount == null || currentUsageCount == 0;
    }

    /**
     * Check if promotion has been used at least once (restricted editing)
     */
    public boolean hasBeenUsed() {
        return currentUsageCount != null && currentUsageCount > 0;
    }

    /**
     * Check if a specific field can be edited based on usage status
     */
    public boolean canEditField(String fieldName) {
        if (isNeverUsed()) {
            // Can edit all fields when never used
            return true;
        }

        // When promotion has been used, only certain fields can be edited
        switch (fieldName.toLowerCase()) {
            case "code":
            case "discountvalue":
            case "maxdiscountamount":
            case "minordervalue":
                // Core promotion terms cannot be changed after use
                return false;

            case "enddate":
            case "totalusagelimit":
            case "usagelimitperuser":
            case "isactive":
                // Operational fields can be changed
                return true;

            default:
                // Default to not editable for safety
                return false;
        }
    }

    /**
     * Get list of editable fields based on current usage status
     */
    public Set<String> getEditableFields() {
        Set<String> editableFields = new HashSet<>();

        if (isNeverUsed()) {
            // All fields are editable when never used
            editableFields.add("code");
            editableFields.add("discountValue");
            editableFields.add("maxDiscountAmount");
            editableFields.add("minOrderValue");
            editableFields.add("endDate");
            editableFields.add("totalUsageLimit");
            editableFields.add("usageLimitPerUser");
            editableFields.add("isActive");
        } else {
            // Only operational fields when used
            editableFields.add("endDate");
            editableFields.add("totalUsageLimit");
            editableFields.add("usageLimitPerUser");
            editableFields.add("isActive");
        }

        return editableFields;
    }

    /**
     * Get list of non-editable fields with reasons
     */
    public Set<String> getNonEditableFieldsWithReasons() {
        Set<String> nonEditableFields = new HashSet<>();

        if (hasBeenUsed()) {
            nonEditableFields.add("code - Cannot change promotion code after it has been used");
            nonEditableFields.add("discountValue - Cannot change discount percentage/amount after use");
            nonEditableFields.add("maxDiscountAmount - Cannot change maximum discount after use");
            nonEditableFields.add("minOrderValue - Cannot change minimum order requirement after use");
        }

        return nonEditableFields;
    }
}
