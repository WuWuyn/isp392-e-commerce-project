package com.example.isp392.service;

import com.example.isp392.dto.PromotionFormDTO;
import com.example.isp392.model.*;
import com.example.isp392.repository.*;
import org.springframework.transaction.annotation.Transactional;
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
    private final BookRepository bookRepository;
    private final ShopRepository shopRepository;

    public PromotionService(PromotionRepository promotionRepository,
                           CategoryRepository categoryRepository,
                           BookRepository bookRepository,
                           ShopRepository shopRepository) {
        this.promotionRepository = promotionRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.shopRepository = shopRepository;
    }
    
    // ==================== CRUD Operations ====================

    /**
     * Get all promotions with pagination and filtering
     */
    public Page<Promotion> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable);
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
            // Force initialization of lazy collections
            promotion.getApplicableCategories().size();
            promotion.getApplicableBooks().size();
            promotion.getApplicableShops().size();

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

        // Check if promotion can be updated (business rules)
        if (existingPromotion.getCurrentUsageCount() > 0 &&
            !dto.getCode().equals(existingPromotion.getCode())) {
            throw new IllegalArgumentException("Cannot change code of a promotion that has been used");
        }

        mapDtoToEntity(dto, existingPromotion, currentUser);
        existingPromotion.setUpdatedBy(currentUser);
        existingPromotion.setUpdatedAt(LocalDateTime.now());

        // Update scope-specific relationships
        setScopeRelationships(existingPromotion, dto);

        existingPromotion = promotionRepository.save(existingPromotion);
        logger.info("Successfully updated promotion with ID: {}", id);

        return existingPromotion;
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
        // Clear existing relationships
        promotion.getApplicableCategories().clear();
        promotion.getApplicableBooks().clear();
        promotion.getApplicableShops().clear();

        switch (dto.getScopeType()) {
            case CATEGORY:
                if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
                    Set<Category> categories = categoryRepository.findAllById(dto.getCategoryIds())
                        .stream().collect(Collectors.toSet());
                    promotion.setApplicableCategories(categories);
                }
                break;
            case PRODUCT:
                if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {
                    Set<Book> books = bookRepository.findAllById(dto.getBookIds())
                        .stream().collect(Collectors.toSet());
                    promotion.setApplicableBooks(books);
                }
                break;
            case SHOP:
                if (dto.getShopIds() != null && !dto.getShopIds().isEmpty()) {
                    Set<Shop> shops = shopRepository.findAllById(dto.getShopIds())
                        .stream().collect(Collectors.toSet());
                    promotion.setApplicableShops(shops);
                }
                break;
            case SITE_WIDE:
            default:
                // No specific relationships needed for site-wide promotions
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

    /**
     * Find promotions applicable to a specific book and shop
     */
    public List<Promotion> findApplicablePromotions(Book book, Shop shop) {
        List<Promotion> applicablePromotions = new ArrayList<>();

        // Add site-wide promotions
        applicablePromotions.addAll(promotionRepository.findSiteWidePromotions());

        // Add book-specific promotions
        if (book != null) {
            applicablePromotions.addAll(promotionRepository.findBookPromotions(book.getBookId()));
            applicablePromotions.addAll(promotionRepository.findCategoryPromotions(book.getBookId()));
        }

        // Add shop-specific promotions
        if (shop != null) {
            applicablePromotions.addAll(promotionRepository.findShopPromotions(shop.getShopId()));
        }

        // Remove duplicates and return
        return applicablePromotions.stream().distinct().collect(Collectors.toList());
    }
}