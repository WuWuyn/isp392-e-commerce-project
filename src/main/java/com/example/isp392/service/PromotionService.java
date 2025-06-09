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
        // This would typically query a usage tracking table
        // For now, we'll return 0 as a placeholder
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
} 