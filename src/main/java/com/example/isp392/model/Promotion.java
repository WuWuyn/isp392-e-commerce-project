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
     * Enum for promotion types
     */
    public enum PromotionType {
        PERCENTAGE_DISCOUNT("Percentage Discount"),
        FIXED_AMOUNT_DISCOUNT("Fixed Amount Discount"),
        BUY_ONE_GET_ONE("Buy One Get One"),
        FREE_SHIPPING("Free Shipping"),
        BUNDLE_DISCOUNT("Bundle Discount");

        private final String displayName;

        PromotionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for promotion scope
     */
    public enum ScopeType {
        SITE_WIDE("Site-wide"),
        CATEGORY("Category"),
        PRODUCT("Product"),
        SHOP("Shop");

        private final String displayName;

        ScopeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for promotion status
     */
    public enum PromotionStatus {
        DRAFT("Draft"),
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
    private PromotionStatus status = PromotionStatus.DRAFT;

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

    // Scope-specific relationships
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotion_categories",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> applicableCategories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotion_books",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> applicableBooks = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotion_shops",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "shop_id")
    )
    private Set<Shop> applicableShops = new HashSet<>();

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
}
