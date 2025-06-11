package com.example.isp392.repository;

import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
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
} 