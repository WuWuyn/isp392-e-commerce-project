package com.example.isp392.repository;

import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Shop entity
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    
    /**
     * Find a shop by user ID
     * @param userId the user ID
     * @return the shop or null if not found
     */
    Shop findByUserUserId(Integer userId);

    /**
     * Get the registration date of a shop by shopId
     * @param shopId the shop ID
     * @return the registration date or null if not found
     */
    @Query(value = "SELECT registration_date FROM shops s WHERE s.shop_id = :shopId",nativeQuery = true)
    java.time.LocalDateTime getRegistrationDateByShopId(@Param("shopId") Integer shopId);
} 