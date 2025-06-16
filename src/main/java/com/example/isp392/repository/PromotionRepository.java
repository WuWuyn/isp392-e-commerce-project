package com.example.isp392.repository;

import com.example.isp392.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    
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

    List<Promotion> findByIsActiveTrue();

    boolean existsByCode(String code);
} 