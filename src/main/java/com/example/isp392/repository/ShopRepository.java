package com.example.isp392.repository;

import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer>, JpaSpecificationExecutor<Shop> {



    Optional<Shop> findByUserUserId(Integer userId);

    @Query("SELECT s FROM Shop s WHERE s.approvalStatus = :status")
    List<Shop> findByApprovalStatus(@Param("status") Shop.ApprovalStatus status);

    @Query("SELECT count(s) FROM Shop s WHERE s.approvalStatus = :status")
    long countByApprovalStatus(@Param("status") Shop.ApprovalStatus status);

    /**
     * Get the registration date of a shop by shopId
     * @param shopId the shop ID
     * @return the registration date or null if not found
     */
    @Query(value = "SELECT registration_date FROM shops s WHERE s.shop_id = :shopId",nativeQuery = true)
    java.time.LocalDateTime getRegistrationDateByShopId(@Param("shopId") Integer shopId);

    // Method to update the isActive status of a shop
    @Query("UPDATE Shop s SET s.isActive = :isActive WHERE s.shopId = :shopId")
    void updateShopActiveStatus(@Param("shopId") Integer shopId, @Param("isActive") boolean isActive);
} 