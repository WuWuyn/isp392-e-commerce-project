package com.example.isp392.controller.buyer;

import com.example.isp392.model.Promotion;
import com.example.isp392.model.User;
import com.example.isp392.repository.PromotionRepository;
import com.example.isp392.repository.PromotionUsageRepository;
import com.example.isp392.service.PromotionService;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for buyer-facing promotion listing and details
 */
@Controller
@RequestMapping("/buyer/promotions")
public class BuyerPromotionController {

    private static final Logger logger = LoggerFactory.getLogger(BuyerPromotionController.class);

    private final PromotionService promotionService;
    private final UserService userService;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionRepository promotionRepository;

    public BuyerPromotionController(PromotionService promotionService,
                                   UserService userService,
                                   PromotionUsageRepository promotionUsageRepository,
                                   PromotionRepository promotionRepository) {
        this.promotionService = promotionService;
        this.userService = userService;
        this.promotionUsageRepository = promotionUsageRepository;
        this.promotionRepository = promotionRepository;
    }

    /**
     * Display all active promotions for buyers
     */
    @GetMapping
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                @RequestParam(defaultValue = "startDate") String sortBy,
                                @RequestParam(defaultValue = "desc") String sortDir,
                                Authentication authentication,
                                Model model) {
        
        logger.info("Displaying promotions list for buyer - page: {}, size: {}", page, size);

        if (authentication == null) {
            logger.warn("Unauthenticated user trying to access promotions");
            return "redirect:/buyer/login";
        }

        try {
            // Get current user
            User currentUser = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Create pageable with sorting
            Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get active promotions
            Page<Promotion> activePromotions = getActivePromotions(pageable);



            // Calculate usage statistics for each promotion (simplified)
            Map<Integer, PromotionUsageStats> usageStatsMap = new HashMap<>();
            if (activePromotions.hasContent()) {
                for (Promotion promotion : activePromotions.getContent()) {
                    usageStatsMap.put(promotion.getPromotionId(), createSimpleUsageStats(promotion, currentUser));
                }
            }

            // Add attributes to model
            model.addAttribute("promotions", activePromotions);
            model.addAttribute("usageStats", usageStatsMap);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", activePromotions.getTotalPages());
            model.addAttribute("totalElements", activePromotions.getTotalElements());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            return "buyer/promotions";

        } catch (Exception e) {
            logger.error("Error displaying promotions list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load promotions. Please try again later.");
            return "buyer/promotions";
        }
    }



    /**
     * Display detailed view of a specific promotion
     */
    @GetMapping("/{id}")
    public String viewPromotionDetails(@PathVariable Integer id,
                                     Authentication authentication,
                                     Model model) {
        
        logger.info("Displaying promotion details for ID: {}", id);

        if (authentication == null) {
            logger.warn("Unauthenticated user trying to access promotion details");
            return "redirect:/buyer/login";
        }

        try {
            // Get current user
            User currentUser = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Get promotion details
            Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
            if (promotionOpt.isEmpty()) {
                logger.warn("Promotion not found with ID: {}", id);
                model.addAttribute("errorMessage", "Promotion not found.");
                return "buyer/promotions";
            }

            Promotion promotion = promotionOpt.get();

            // Check if promotion is active and available
            if (!promotion.getIsActive() || !promotion.canBeUsed()) {
                logger.warn("Inactive or unavailable promotion accessed: {}", id);
                model.addAttribute("errorMessage", "This promotion is no longer available.");
                return "buyer/promotions";
            }

            // Calculate usage statistics for this promotion (simplified)
            PromotionUsageStats usageStats = createSimpleUsageStats(promotion, currentUser);

            // Add attributes to model
            model.addAttribute("promotion", promotion);
            model.addAttribute("usageStats", usageStats);
            model.addAttribute("currentUser", currentUser);

            return "buyer/promotion-detail";

        } catch (Exception e) {
            logger.error("Error displaying promotion details for ID {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Unable to load promotion details. Please try again later.");
            return "buyer/promotions";
        }
    }

    /**
     * Get active promotions with filtering
     */
    private Page<Promotion> getActivePromotions(Pageable pageable) {
        // Get promotions that are active, not expired, and can be used
        Page<Promotion> result = promotionService.getActivePromotions(pageable);



        return result;
    }

    /**
     * Create simplified usage statistics to avoid complex calculations
     */
    private PromotionUsageStats createSimpleUsageStats(Promotion promotion, User user) {
        try {
            // Get user's usage count for this promotion (simplified)
            int userUsageCount = 0;
            if (user != null && promotion != null && promotion.getPromotionId() != null) {
                userUsageCount = promotionUsageRepository.countByUserIdAndPromotionId(
                    user.getUserId(), promotion.getPromotionId());
            }

            // Calculate remaining uses for the user (simplified)
            Integer remainingUses = null;
            if (promotion.getUsageLimitPerUser() != null) {
                remainingUses = Math.max(0, promotion.getUsageLimitPerUser() - userUsageCount);
            }

            // Simplified - don't calculate complex global usage stats
            boolean canBeUsed = promotion.getIsActive() &&
                               (promotion.getUsageLimitPerUser() == null || userUsageCount < promotion.getUsageLimitPerUser());

            return new PromotionUsageStats(
                userUsageCount,
                promotion.getUsageLimitPerUser(),
                remainingUses,
                0, // Simplified - don't show total usage count
                null, // Simplified - don't show total usage limit
                null, // Simplified - don't show total remaining uses
                canBeUsed
            );
        } catch (Exception e) {
            logger.warn("Error creating usage stats for promotion {}: {}",
                       promotion != null ? promotion.getPromotionId() : "null", e.getMessage());
            return createDefaultUsageStats();
        }
    }

    /**
     * Create default usage statistics when calculation fails
     */
    private PromotionUsageStats createDefaultUsageStats() {
        return new PromotionUsageStats(0, null, null, 0, null, null, false);
    }

    /**
     * Inner class to hold promotion usage statistics
     */
    public static class PromotionUsageStats {
        private final int userUsageCount;
        private final Integer usageLimitPerUser;
        private final Integer remainingUsesForUser;
        private final int totalUsageCount;
        private final Integer totalUsageLimit;
        private final Integer totalRemainingUses;
        private final boolean canBeUsed;

        public PromotionUsageStats(int userUsageCount, Integer usageLimitPerUser, Integer remainingUsesForUser,
                                 int totalUsageCount, Integer totalUsageLimit, Integer totalRemainingUses,
                                 boolean canBeUsed) {
            this.userUsageCount = userUsageCount;
            this.usageLimitPerUser = usageLimitPerUser;
            this.remainingUsesForUser = remainingUsesForUser;
            this.totalUsageCount = totalUsageCount;
            this.totalUsageLimit = totalUsageLimit;
            this.totalRemainingUses = totalRemainingUses;
            this.canBeUsed = canBeUsed;
        }

        // Getters
        public int getUserUsageCount() { return userUsageCount; }
        public Integer getUsageLimitPerUser() { return usageLimitPerUser; }
        public Integer getRemainingUsesForUser() { return remainingUsesForUser; }
        public int getTotalUsageCount() { return totalUsageCount; }
        public Integer getTotalUsageLimit() { return totalUsageLimit; }
        public Integer getTotalRemainingUses() { return totalRemainingUses; }
        public boolean isCanBeUsed() { return canBeUsed; }
    }
}
