package com.example.isp392.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionDTO {
    private Integer promotionId;
    private String code;
    private String description;
    private String discountType;  // PERCENTAGE or FIXED_AMOUNT
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Integer usageLimitPerUser;
    private Integer totalUsageLimit;
    private Integer currentUsageCount;
    private Integer createdByAdminId;
} 