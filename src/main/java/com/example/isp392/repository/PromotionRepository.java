package com.example.isp392.repository;

import com.example.isp392.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer>, JpaSpecificationExecutor<Promotion> {

    /**
     * Find a promotion by its code
     * @param code the promotion code
     * @return Optional containing the promotion if found, empty otherwise
     */
    Optional<Promotion> findByCode(String code);

    /**
     * Find a promotion by code and active status
     * @param code the promotion code
     * @return the promotion if found and active
     */
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    /**
     * Find promotions by status
     */
    Page<Promotion> findByStatus(Promotion.PromotionStatus status, Pageable pageable);

    /**
     * Find promotions by scope type
     */
    Page<Promotion> findByScopeType(Promotion.ScopeType scopeType, Pageable pageable);

    /**
     * Find active promotions
     */
    Page<Promotion> findByIsActiveTrue(Pageable pageable);

    /**
     * Find promotions by name or code containing search term
     */
    @Query("SELECT p FROM Promotion p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Promotion> findBySearchTerm(@Param("search") String search, Pageable pageable);

    /**
     * Find overlapping promotions for validation
     */
    @Query("SELECT p FROM Promotion p WHERE " +
           "p.isActive = true AND " +
           "p.scopeType = :scopeType AND " +
           "((p.startDate <= :endDate AND p.endDate >= :startDate))")
    List<Promotion> findOverlappingPromotions(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             @Param("scopeType") Promotion.ScopeType scopeType);

    /**
     * Find expired promotions that are still active
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.endDate < :now")
    List<Promotion> findExpiredActivePromotions(@Param("now") LocalDateTime now);

    /**
     * Find promotions that should be activated (start date has passed)
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = 'DRAFT' AND p.startDate <= :now")
    List<Promotion> findPromotionsToActivate(@Param("now") LocalDateTime now);

    /**
     * Find promotions by created user
     */
    Page<Promotion> findByCreatedBy(com.example.isp392.model.User createdBy, Pageable pageable);

    /**
     * Find promotions created within date range
     */
    @Query("SELECT p FROM Promotion p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Page<Promotion> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    /**
     * Get promotion usage statistics
     */
    @Query("SELECT p.promotionId, p.code, p.name, p.currentUsageCount, p.totalUsageLimit " +
           "FROM Promotion p WHERE p.isActive = true ORDER BY p.currentUsageCount DESC")
    List<Object[]> getPromotionUsageStats();

    /**
     * Find site-wide promotions
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.scopeType = 'SITE_WIDE'")
    List<Promotion> findSiteWidePromotions();

    // Removed findBookPromotions and findShopPromotions as these scopes are no longer supported in simplified system

    /**
     * Find promotions applicable to book categories
     */
    @Query("SELECT DISTINCT p FROM Promotion p JOIN p.applicableCategories ac JOIN ac.books b WHERE p.isActive = true AND p.scopeType = 'CATEGORY' AND b.bookId = :bookId")
    List<Promotion> findCategoryPromotions(@Param("bookId") Integer bookId);
}