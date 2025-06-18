package com.example.isp392.service;

import com.example.isp392.model.Promotion;
import com.example.isp392.repository.PromotionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    
    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }
    
    /**
     * Find a promotion by its code
     * @param code the promotion code
     * @return Optional containing the promotion if found, empty otherwise
     */
    public Optional<Promotion> findByCode(String code) {
        return promotionRepository.findByCode(code);
    }
    
    /**
     * Get the number of times a user has used a specific promotion
     * @param userId the user ID
     * @param promotionId the promotion ID
     * @return the usage count
     */
    public int getUserUsageCount(Integer userId, Integer promotionId) {
        // TODO: In a real application, this would query a user_promotions table
        // For now, just return 0 to allow usage
        return 0;
    }
    
    /**
     * Record that a promotion was used by a user
     * @param userId the user ID
     * @param promotionId the promotion ID
     */
    public void recordUsage(Integer userId, Integer promotionId) {
        // This would typically insert a record in a usage tracking table
        // and increment the current_usage_count in the promotion
        
        // Get the promotion
        Optional<Promotion> promotionOpt = promotionRepository.findById(promotionId);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();
            
            // Increment usage count
            Integer currentCount = promotion.getCurrentUsageCount();
            if (currentCount == null) {
                currentCount = 0;
            }
            promotion.setCurrentUsageCount(currentCount + 1);
            
            // Save the updated promotion
            promotionRepository.save(promotion);
        }
    }

    /**
     * Update promotion usage statistics when a promotion is applied to an order
     * @param code the promotion code
     * @param userId the user ID who used the promotion
     * @return true if updated successfully, false otherwise
     */
    @Transactional
    public boolean updatePromotionUsage(String code, Integer userId) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(code);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();
            
            // Increment total usage count
            if (promotion.getCurrentUsageCount() == null) {
                promotion.setCurrentUsageCount(1);
            } else {
                promotion.setCurrentUsageCount(promotion.getCurrentUsageCount() + 1);
            }
            
            // TODO: In a real application, track user-specific usage in a separate table
            
            promotionRepository.save(promotion);
            return true;
        }
        return false;
    }
} 