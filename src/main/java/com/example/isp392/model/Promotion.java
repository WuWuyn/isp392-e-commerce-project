package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Integer promotionId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "discount_type", length = 200, nullable = false)
    private String discountType;

    @Column(name = "discount_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 10, scale = 0)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", precision = 18, scale = 0)
    private BigDecimal minOrderValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "current_usage_count")
    private Integer currentUsageCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin")
    private User createdByAdmin;
}
