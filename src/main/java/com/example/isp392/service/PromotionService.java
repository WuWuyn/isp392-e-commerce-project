package com.example.isp392.service;

import com.example.isp392.dto.PromotionFormDTO;
import com.example.isp392.model.*;
import com.example.isp392.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;

    public PromotionService(PromotionRepository promotionRepository,
                           CategoryRepository categoryRepository,
                           PromotionUsageRepository promotionUsageRepository,
                           CustomerOrderRepository customerOrderRepository,
                           UserRepository userRepository) {
        this.promotionRepository = promotionRepository;
        this.categoryRepository = categoryRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.userRepository = userRepository;
    }
    
    // ==================== CRUD Operations ====================

    /**
     * Get all promotions with pagination and filtering
     */
    public Page<Promotion> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable);
    }

    /**
     * Get active promotions for buyer listing
     */
    public Page<Promotion> getActivePromotions(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActivePromotions(
            Promotion.PromotionStatus.ACTIVE,
            now,
            pageable
        );
    }

    /**
     * Get promotions with filtering and search
     */
    public Page<Promotion> getPromotionsWithFilters(String search, Promotion.PromotionStatus status,
                                                   Promotion.ScopeType scopeType, Boolean isActive,
                                                   Pageable pageable) {
        Specification<Promotion> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%")
                )
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (scopeType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("scopeType"), scopeType));
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return promotionRepository.findAll(spec, pageable);
    }

    /**
     * Get promotion by ID
     */
    public Optional<Promotion> getPromotionById(Integer id) {
        return promotionRepository.findById(id);
    }

    /**
     * Get promotion by ID with eager loaded collections for detail view
     */
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByIdWithDetails(Integer id) {
        Optional<Promotion> promotionOpt = promotionRepository.findById(id);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();
            // Force initialization of lazy collections - only categories in simplified system
            promotion.getApplicableCategories().size();

            // Force initialization of lazy user references
            if (promotion.getCreatedBy() != null) {
                promotion.getCreatedBy().getEmail();
            }
            if (promotion.getUpdatedBy() != null) {
                promotion.getUpdatedBy().getEmail();
            }
        }
        return promotionOpt;
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
     * Create a new promotion
     */
    public Promotion createPromotion(PromotionFormDTO dto, User currentUser) {
        logger.info("Creating new promotion with code: {}", dto.getCode());

        // Validate unique code
        if (promotionRepository.findByCode(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Promotion code already exists: " + dto.getCode());
        }

        // Validate business rules
        validatePromotionBusinessRules(dto);

        Promotion promotion = new Promotion();
        mapDtoToEntity(dto, promotion, currentUser);

        // Set scope-specific relationships
        setScopeRelationships(promotion, dto);

        promotion = promotionRepository.save(promotion);
        logger.info("Successfully created promotion with ID: {}", promotion.getPromotionId());

        return promotion;
    }

    /**
     * Update an existing promotion
     */
    public Promotion updatePromotion(Integer id, PromotionFormDTO dto, User currentUser) {
        logger.info("Updating promotion with ID: {}", id);

        Promotion existingPromotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + id));

        // Validate unique code (excluding current promotion)
        Optional<Promotion> codeCheck = promotionRepository.findByCode(dto.getCode());
        if (codeCheck.isPresent() && !codeCheck.get().getPromotionId().equals(id)) {
            throw new IllegalArgumentException("Promotion code already exists: " + dto.getCode());
        }

        // Validate business rules
        validatePromotionBusinessRules(dto);

        // MODIFICATION: Check if promotion has been used or is active before editing
        if (existingPromotion.hasBeenUsed() || existingPromotion.getIsActive()) {
            throw new IllegalStateException("Promotion cannot be edited. It must be deactivated and have zero usage.");
        }

        // When never used and inactive, all fields can be updated
        mapDtoToEntity(dto, existingPromotion, currentUser);
        logger.info("Updated all fields for unused and inactive promotion: {}", existingPromotion.getCode());

        existingPromotion.setUpdatedBy(currentUser);
        existingPromotion.setUpdatedAt(LocalDateTime.now());

        // Update scope-specific relationships
        setScopeRelationships(existingPromotion, dto);

        existingPromotion = promotionRepository.save(existingPromotion);
        logger.info("Successfully updated promotion with ID: {}", id);

        return existingPromotion;
    }

    /**
     * Update only allowed fields of a promotion (for used promotions)
     */
    private void updateRestrictedPromotionFields(Promotion promotion, PromotionFormDTO dto, User currentUser) {
        // Only update operational fields that are safe to change after use
        promotion.setEndDate(dto.getEndDate());
        promotion.setTotalUsageLimit(dto.getTotalUsageLimit());
        promotion.setUsageLimitPerUser(dto.getUsageLimitPerUser());
        promotion.setIsActive(dto.getIsActive());

        // Update metadata
        promotion.setUpdatedBy(currentUser);
        promotion.setUpdatedAt(LocalDateTime.now());

        logger.info("Updated restricted fields for used promotion: {} (usage count: {})",
                   promotion.getCode(), promotion.getCurrentUsageCount());
    }

    /**
     * Get promotion edit status with editable fields information
     */
    public Map<String, Object> getPromotionEditStatus(Integer id) {
        Map<String, Object> result = new HashMap<>();

        Optional<Promotion> optionalPromotion = promotionRepository.findById(id);
        if (optionalPromotion.isPresent()) {
            Promotion promotion = optionalPromotion.get();

            result.put("id", promotion.getPromotionId());
            result.put("code", promotion.getCode());
            result.put("isActive", promotion.getIsActive());
            result.put("neverUsed", promotion.isNeverUsed());
            result.put("hasBeenUsed", promotion.hasBeenUsed());
            result.put("currentUsageCount", promotion.getCurrentUsageCount());
            result.put("editableFields", promotion.getEditableFields());
            result.put("nonEditableFields", promotion.getNonEditableFieldsWithReasons());

            return result;
        }

        result.put("error", "Promotion not found");
        return result;
    }

    /**
     * Delete a promotion (soft delete by setting inactive)
     */
    public void deletePromotion(Integer id, User currentUser) {
        logger.info("Deleting promotion with ID: {}", id);

        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + id));

        // Check if promotion can be deleted
        if (promotion.getCurrentUsageCount() > 0) {
            throw new IllegalArgumentException("Cannot delete a promotion that has been used. Set it to inactive instead.");
        }

        promotion.setIsActive(false);
        promotion.setStatus(Promotion.PromotionStatus.INACTIVE);
        promotion.setUpdatedBy(currentUser);
        promotion.setUpdatedAt(LocalDateTime.now());

        promotionRepository.save(promotion);
        logger.info("Successfully deleted promotion with ID: {}", id);
    }

    /**
     * Toggle promotion active status
     */
    public Promotion togglePromotionStatus(Integer id, User currentUser) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + id));

        promotion.setIsActive(!promotion.getIsActive());
        promotion.setStatus(promotion.getIsActive() ?
            Promotion.PromotionStatus.ACTIVE : Promotion.PromotionStatus.INACTIVE);
        promotion.setUpdatedBy(currentUser);
        promotion.setUpdatedAt(LocalDateTime.now());

        return promotionRepository.save(promotion);
    }

    // ==================== Helper Methods ====================

    /**
     * Map DTO to entity
     */
    private void mapDtoToEntity(PromotionFormDTO dto, Promotion promotion, User currentUser) {
        promotion.setName(dto.getName());
        promotion.setCode(dto.getCode().toUpperCase());
        promotion.setDescription(dto.getDescription());
        promotion.setPromotionType(dto.getPromotionType());
        promotion.setDiscountType(dto.getDiscountType()); // For backward compatibility
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        promotion.setMinOrderValue(dto.getMinOrderValue());
        promotion.setScopeType(dto.getScopeType());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());
        promotion.setIsActive(dto.getIsActive());
        promotion.setStatus(dto.getStatus());
        promotion.setUsageLimitPerUser(dto.getUsageLimitPerUser());
        promotion.setTotalUsageLimit(dto.getTotalUsageLimit());

        if (promotion.getPromotionId() == null) { // New promotion
            promotion.setCreatedBy(currentUser);
            promotion.setCreatedAt(LocalDateTime.now());
        }
    }

    /**
     * Set scope-specific relationships
     */
    private void setScopeRelationships(Promotion promotion, PromotionFormDTO dto) {
        // Clear existing relationships - only categories in simplified system
        promotion.getApplicableCategories().clear();

        switch (dto.getScopeType()) {
            case SITE_WIDE:
            default:
                // No specific relationships needed for site-wide promotions
                // All categories are cleared above
                break;
        }
    }

    /**
     * Validate business rules for promotion
     */
    private void validatePromotionBusinessRules(PromotionFormDTO dto) {
        // Validate discount value based on type
        if (dto.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
            if (dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 ||
                dto.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage discount must be between 1 and 100");
            }
        } else if (dto.getPromotionType() == Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT) {
            if (dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Fixed amount discount must be greater than 0");
            }
        }

        // Validate usage limits
        if (dto.getUsageLimitPerUser() != null && dto.getTotalUsageLimit() != null) {
            if (dto.getTotalUsageLimit() < dto.getUsageLimitPerUser()) {
                throw new IllegalArgumentException("Total usage limit must be greater than or equal to per-user limit");
            }
        }
    }

    /**
     * Get the number of times a user has used a specific promotion
     * @param userId the user ID
     * @param promotionId the promotion ID
     * @return the usage count
     */
    public int getUserUsageCount(Integer userId, Integer promotionId) {
        return promotionUsageRepository.countByUserIdAndPromotionId(userId, promotionId);
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
        return updatePromotionUsage(code, userId, null, BigDecimal.ZERO);
    }

    /**
     * Update promotion usage statistics with customer order details
     * Uses SERIALIZABLE isolation to prevent race conditions in usage counting
     * @param code the promotion code
     * @param userId the user ID who used the promotion
     * @param customerOrderId the customer order ID (optional)
     * @param discountAmount the discount amount applied
     * @return true if updated successfully, false otherwise
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public boolean updatePromotionUsage(String code, Integer userId, Integer customerOrderId, BigDecimal discountAmount) {
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(code);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();

            // Increment total usage count
            if (promotion.getCurrentUsageCount() == null) {
                promotion.setCurrentUsageCount(1);
            } else {
                promotion.setCurrentUsageCount(promotion.getCurrentUsageCount() + 1);
            }

            // Create usage record
            try {
                User user = new User();
                user.setUserId(userId);

                CustomerOrder customerOrder = null;
                if (customerOrderId != null) {
                    customerOrder = customerOrderRepository.findById(customerOrderId).orElse(null);
                }

                PromotionUsage usage = new PromotionUsage(promotion, user, customerOrder, discountAmount, code);
                promotionUsageRepository.save(usage);

                logger.info("Created promotion usage record for code: {} by user: {} with discount: {}",
                           code, userId, discountAmount);
            } catch (Exception e) {
                logger.error("Failed to create promotion usage record for code: {} by user: {}: {}",
                           code, userId, e.getMessage());
                // Don't fail the promotion update if usage tracking fails
            }

            promotionRepository.save(promotion);
            return true;
        }
        return false;
    }

    /**
     * Find promotions applicable to a specific book - simplified for site-wide and category only
     */
    public List<Promotion> findApplicablePromotions(Book book, Shop shop) {
        List<Promotion> applicablePromotions = new ArrayList<>();

        // Add site-wide promotions
        applicablePromotions.addAll(promotionRepository.findSiteWidePromotions());

        // Add category-specific promotions for the book
        if (book != null) {
            applicablePromotions.addAll(promotionRepository.findCategoryPromotions(book.getBookId()));
        }

        // Note: Shop-specific and book-specific promotions are no longer supported in simplified system

        // Remove duplicates and return
        return applicablePromotions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Validate promotion usage limits with concurrency control
     * This method checks both global and per-user limits atomically
     *
     * @param promotionCode the promotion code to validate
     * @param userId the user ID attempting to use the promotion
     * @return true if the promotion can be used, false if limits are exceeded
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public boolean validatePromotionUsageLimits(String promotionCode, Integer userId) {
        logger.info("Validating usage limits for promotion: {} by user: {}", promotionCode, userId);

        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(promotionCode);
        if (!promotionOpt.isPresent()) {
            logger.warn("Promotion not found or inactive: {}", promotionCode);
            return false;
        }

        Promotion promotion = promotionOpt.get();

        // Check global usage limit
        if (promotion.getTotalUsageLimit() != null && promotion.getCurrentUsageCount() != null) {
            if (promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
                logger.warn("Promotion {} has reached global usage limit: {}/{}",
                           promotionCode, promotion.getCurrentUsageCount(), promotion.getTotalUsageLimit());
                return false;
            }
        }

        // Check per-user usage limit
        if (promotion.getUsageLimitPerUser() != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                long userUsageCount = promotionUsageRepository.countByPromotionAndUser(promotion, user);
                if (userUsageCount >= promotion.getUsageLimitPerUser()) {
                    logger.warn("User {} has reached usage limit for promotion {}: {}/{}",
                               userId, promotionCode, userUsageCount, promotion.getUsageLimitPerUser());
                    return false;
                }
            }
        }

        logger.info("Promotion usage validation passed for: {} by user: {}", promotionCode, userId);
        return true;
    }

    /**
     * Atomically reserve a promotion usage slot to prevent race conditions
     * This method should be called before order processing to ensure usage limits are respected
     *
     * @param promotionCode the promotion code to reserve
     * @param userId the user ID attempting to use the promotion
     * @return true if reservation successful, false if limits would be exceeded
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public boolean reservePromotionUsage(String promotionCode, Integer userId) {
        logger.info("Attempting to reserve promotion usage for: {} by user: {}", promotionCode, userId);

        // Re-validate limits within the serializable transaction
        if (!validatePromotionUsageLimits(promotionCode, userId)) {
            logger.warn("Promotion usage validation failed during reservation for: {} by user: {}", promotionCode, userId);
            return false;
        }

        // Increment usage count atomically
        Optional<Promotion> promotionOpt = promotionRepository.findByCodeAndIsActiveTrue(promotionCode);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();

            // Double-check limits one more time within the same transaction
            if (promotion.getTotalUsageLimit() != null && promotion.getCurrentUsageCount() != null) {
                if (promotion.getCurrentUsageCount() >= promotion.getTotalUsageLimit()) {
                    logger.warn("Promotion {} usage limit exceeded during reservation", promotionCode);
                    return false;
                }
            }

            // Increment the usage count to reserve the slot
            if (promotion.getCurrentUsageCount() == null) {
                promotion.setCurrentUsageCount(1);
            } else {
                promotion.setCurrentUsageCount(promotion.getCurrentUsageCount() + 1);
            }

            promotionRepository.save(promotion);
            logger.info("Successfully reserved promotion usage for: {} by user: {}, new count: {}",
                       promotionCode, userId, promotion.getCurrentUsageCount());
            return true;
        }

        logger.error("Promotion not found during reservation: {}", promotionCode);
        return false;
    }

    /**
     * Save a promotion entity
     */
    @Transactional
    public Promotion save(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    /**
     * Find promotion by ID
     */
    public Optional<Promotion> findById(Integer id) {
        return promotionRepository.findById(id);
    }
}