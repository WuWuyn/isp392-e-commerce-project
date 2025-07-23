package com.example.isp392.repository;

import com.example.isp392.model.PromotionUsage;
import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PromotionUsage entity
 */
@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer> {

    /**
     * Count how many times a user has used a specific promotion
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.user.userId = :userId AND pu.promotion.promotionId = :promotionId")
    int countByUserIdAndPromotionId(@Param("userId") Integer userId, @Param("promotionId") Integer promotionId);

    /**
     * Find all usage records for a specific promotion
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.promotion.promotionId = :promotionId ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByPromotionId(@Param("promotionId") Integer promotionId);

    /**
     * Find all usage records for a specific user
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.user.userId = :userId ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByUserId(@Param("userId") Integer userId);

    /**
     * Find usage records for a specific customer order
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.customerOrder.customerOrderId = :customerOrderId")
    List<PromotionUsage> findByCustomerOrderId(@Param("customerOrderId") Integer customerOrderId);

    /**
     * Find usage records within a date range
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.usedAt BETWEEN :startDate AND :endDate ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByUsedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find usage records for a specific promotion code
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.promotionCode = :promotionCode ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByPromotionCode(@Param("promotionCode") String promotionCode);

    /**
     * Count total usage for a specific promotion
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.promotion.promotionId = :promotionId")
    int countByPromotionId(@Param("promotionId") Integer promotionId);

    /**
     * Count how many times a user has used a specific promotion (using entities)
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.promotion = :promotion AND pu.user = :user")
    long countByPromotionAndUser(@Param("promotion") Promotion promotion, @Param("user") User user);
}
