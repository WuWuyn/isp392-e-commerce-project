package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track individual promotion usage by users
 */
@Getter
@Setter
@Entity
@Table(name = "promotion_usage", indexes = {
    @Index(name = "idx_promotion_usage_promotion", columnList = "promotion_id"),
    @Index(name = "idx_promotion_usage_user", columnList = "user_id"),
    @Index(name = "idx_promotion_usage_order", columnList = "order_id"),
    @Index(name = "idx_promotion_usage_customer_order", columnList = "customer_order_id"),
    @Index(name = "idx_promotion_usage_date", columnList = "used_at")
})
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Integer usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id")
    private CustomerOrder customerOrder;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "promotion_code", length = 50, nullable = false)
    private String promotionCode;

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }

    // Helper constructor
    public PromotionUsage(Promotion promotion, User user, CustomerOrder customerOrder, 
                         BigDecimal discountAmount, String promotionCode) {
        this.promotion = promotion;
        this.user = user;
        this.customerOrder = customerOrder;
        this.discountAmount = discountAmount;
        this.promotionCode = promotionCode;
        this.usedAt = LocalDateTime.now();
    }

    // Default constructor for JPA
    public PromotionUsage() {
    }
}
