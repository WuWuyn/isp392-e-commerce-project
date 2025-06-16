package com.example.isp392.service;

import com.example.isp392.dto.PromotionDTO;
import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import com.example.isp392.repository.PromotionRepository;
import com.example.isp392.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository, UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.userRepository = userRepository;
    }

    public PromotionDTO createPromotion(PromotionDTO promotionDTO) {
        validatePromotionData(promotionDTO);

        if (promotionRepository.existsByCode(promotionDTO.getCode())) {
            throw new IllegalArgumentException("Promotion code already exists");
        }

        User admin = userRepository.findById(promotionDTO.getCreatedByAdminId())
                .orElseThrow(() -> new EntityNotFoundException("Admin not found"));

        Promotion promotion = new Promotion();
        mapDTOToEntity(promotionDTO, promotion);
        promotion.setCreatedByAdmin(admin);
        promotion.setCurrentUsageCount(0);
        promotion.setIsActive(true);

        Promotion savedPromotion = promotionRepository.save(promotion);
        return mapEntityToDTO(savedPromotion);
    }

    public PromotionDTO updatePromotion(Integer id, PromotionDTO promotionDTO) {
        validatePromotionData(promotionDTO);

        Promotion existingPromotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        if (!existingPromotion.getCode().equals(promotionDTO.getCode()) &&
            promotionRepository.existsByCode(promotionDTO.getCode())) {
            throw new IllegalArgumentException("New promotion code already exists");
        }

        mapDTOToEntity(promotionDTO, existingPromotion);
        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        return mapEntityToDTO(updatedPromotion);
    }

    public void disablePromotion(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
    }

    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    public List<PromotionDTO> getActivePromotions() {
        return promotionRepository.findByIsActiveTrue().stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    public PromotionDTO getPromotionById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));
        return mapEntityToDTO(promotion);
    }

    private void validatePromotionData(PromotionDTO promotionDTO) {
        if (promotionDTO.getStartDate() == null || promotionDTO.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (promotionDTO.getStartDate().isAfter(promotionDTO.getEndDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (promotionDTO.getDiscountType().equals("PERCENTAGE")) {
            if (promotionDTO.getDiscountValue().compareTo(new java.math.BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot be greater than 100%");
            }
        }

        if (promotionDTO.getMinOrderValue() != null && promotionDTO.getMinOrderValue().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum order value cannot be negative");
        }

        if (promotionDTO.getMaxDiscountAmount() != null && promotionDTO.getMaxDiscountAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum discount amount cannot be negative");
        }

        if (promotionDTO.getUsageLimitPerUser() != null && promotionDTO.getUsageLimitPerUser() < 1) {
            throw new IllegalArgumentException("Usage limit per user must be at least 1");
        }

        if (promotionDTO.getTotalUsageLimit() != null && promotionDTO.getTotalUsageLimit() < 1) {
            throw new IllegalArgumentException("Total usage limit must be at least 1");
        }
    }

    private void mapDTOToEntity(PromotionDTO dto, Promotion entity) {
        BeanUtils.copyProperties(dto, entity, "promotionId", "createdByAdmin", "currentUsageCount");
    }

    private PromotionDTO mapEntityToDTO(Promotion entity) {
        PromotionDTO dto = new PromotionDTO();
        BeanUtils.copyProperties(entity, dto);
        if (entity.getCreatedByAdmin() != null) {
            dto.setCreatedByAdminId(entity.getCreatedByAdmin().getUserId());
        }
        return dto;
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
            
            // Check if promotion can still be used
            if (!isPromotionValid(code, java.math.BigDecimal.ZERO, userId)) {
                return false;
            }
            
            // Increment total usage count
            if (promotion.getCurrentUsageCount() == null) {
                promotion.setCurrentUsageCount(1);
            } else {
                promotion.setCurrentUsageCount(promotion.getCurrentUsageCount() + 1);
            }
            
            // TODO: In a real application, track user-specific usage in a separate table
            // For now, we'll just update the total count
            
            promotionRepository.save(promotion);
            return true;
        }
        return false;
    }

    /**
     * Validate if a promotion can be used
     * @param code Promotion code
     * @param orderValue Current order value
     * @param userId User ID
     * @return true if promotion is valid and can be used
     */
    public boolean isPromotionValid(String code, java.math.BigDecimal orderValue, Integer userId) {
        return promotionRepository.findByCode(code)
                .map(promotion -> {
                    LocalDateTime now = LocalDateTime.now();
                    
                    if (!promotion.getIsActive()) {
                        throw new IllegalStateException("Promotion is not active");
                    }

                    if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
                        throw new IllegalStateException("Promotion is not valid at this time");
                    }

                    if (promotion.getMinOrderValue() != null && 
                        orderValue.compareTo(promotion.getMinOrderValue()) < 0) {
                        throw new IllegalStateException(
                            "Order value must be at least " + promotion.getMinOrderValue());
                    }

                    if (promotion.getTotalUsageLimit() != null && 
                        promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
                        throw new IllegalStateException("Promotion usage limit has been reached");
                    }

                    // Check user-specific usage limit
                    int userUsageCount = getUserUsageCount(userId, promotion.getPromotionId());
                    if (promotion.getUsageLimitPerUser() != null && 
                        userUsageCount >= promotion.getUsageLimitPerUser()) {
                        throw new IllegalStateException("You have reached the usage limit for this promotion");
                    }

                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));
    }
} 