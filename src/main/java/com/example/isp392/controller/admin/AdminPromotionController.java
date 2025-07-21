package com.example.isp392.controller.admin;

import com.example.isp392.dto.PromotionFormDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.CustomNumberEditor;

@Controller
@RequestMapping("/admin/promotions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPromotionController.class);
    
    private final PromotionService promotionService;
    private final CategoryService categoryService;
    private final BookService bookService;
    private final ShopService shopService;
    private final UserService userService;

    public AdminPromotionController(PromotionService promotionService,
                                   CategoryService categoryService,
                                   BookService bookService,
                                   ShopService shopService,
                                   UserService userService) {
        this.promotionService = promotionService;
        this.categoryService = categoryService;
        this.bookService = bookService;
        this.shopService = shopService;
        this.userService = userService;
    }

    /**
     * Display promotion list page with filtering and pagination
     */
    @GetMapping
    public String listPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Promotion.PromotionStatus status,
            @RequestParam(required = false) Promotion.ScopeType scopeType,
            @RequestParam(required = false) Boolean isActive,
            Model model) {

        logger.info("Listing promotions - page: {}, size: {}, search: {}", page, size, search);

        Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Promotion> promotions = promotionService.getPromotionsWithFilters(
            search, status, scopeType, isActive, pageable);

        model.addAttribute("promotions", promotions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", promotions.getTotalPages());
        model.addAttribute("totalElements", promotions.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("scopeType", scopeType);
        model.addAttribute("isActive", isActive);
        
        // Add enum values for filters
        model.addAttribute("statusOptions", Promotion.PromotionStatus.values());
        model.addAttribute("scopeTypeOptions", Promotion.ScopeType.values());
        model.addAttribute("activeMenu", "promotions");

        return "admin/promotions/list";
    }

    /**
     * Display promotion details page
     */
    @GetMapping("/{id}")
    public String viewPromotion(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Viewing promotion with ID: {}", id);

        Optional<Promotion> promotionOpt = promotionService.getPromotionByIdWithDetails(id);
        if (promotionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Promotion not found");
            return "redirect:/admin/promotions";
        }

        model.addAttribute("promotion", promotionOpt.get());
        model.addAttribute("activeMenu", "promotions");
        return "admin/promotions/detail";
    }

    /**
     * Display create promotion form
     */
    @GetMapping("/create")
    public String createPromotionForm(Model model) {
        logger.info("Displaying create promotion form");

        // Create DTO with sensible defaults for simplified system
        PromotionFormDTO promotionForm = new PromotionFormDTO();
        promotionForm.setStatus(Promotion.PromotionStatus.ACTIVE);
        promotionForm.setIsActive(true);
        promotionForm.setScopeType(Promotion.ScopeType.SITE_WIDE); // Default to site-wide
        promotionForm.setPromotionType(Promotion.PromotionType.PERCENTAGE_DISCOUNT); // Default to percentage

        model.addAttribute("promotionForm", promotionForm);
        model.addAttribute("isEdit", false);
        model.addAttribute("activeMenu", "promotions");
        addFormAttributes(model);
        return "admin/promotions/form";
    }

    /**
     * Handle create promotion form submission
     */
    @PostMapping("/create")
    public String createPromotion(@Valid @ModelAttribute("promotionForm") PromotionFormDTO promotionForm,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        logger.info("Creating promotion with code: {}", promotionForm.getCode());
        logger.info("Received form data - maxDiscountAmount: {}, minOrderValue: {}",
                   promotionForm.getMaxDiscountAmount(), promotionForm.getMinOrderValue());

        // Only do custom validation if basic validation passes
        if (!bindingResult.hasErrors()) {
            validatePromotionFields(promotionForm, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in promotion form: {}", bindingResult.getAllErrors());
            bindingResult.getAllErrors().forEach(error ->
                logger.warn("Validation error: {}", error.getDefaultMessage()));
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "promotions");
            addFormAttributes(model);
            return "admin/promotions/form";
        }

        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            if (currentUser == null) {
                throw new RuntimeException("Current user not found");
            }

            // Set defaults for simplified promotion system
            if (promotionForm.getStatus() == null) {
                promotionForm.setStatus(Promotion.PromotionStatus.ACTIVE);
            }
            if (promotionForm.getIsActive() == null) {
                promotionForm.setIsActive(true);
            }

            Promotion promotion = promotionService.createPromotion(promotionForm, currentUser);
            
            redirectAttributes.addFlashAttribute("success", 
                "Promotion '" + promotion.getName() + "' created successfully");
            return "redirect:/admin/promotions/" + promotion.getPromotionId();

        } catch (IllegalArgumentException e) {
            logger.error("Error creating promotion: {}", e.getMessage());
            bindingResult.rejectValue("code", "error.code", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeMenu", "promotions");
            addFormAttributes(model);
            return "admin/promotions/form";
        } catch (Exception e) {
            logger.error("Unexpected error creating promotion", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
            return "redirect:/admin/promotions";
        }
    }

    /**
     * Display edit promotion form
     */
    @GetMapping("/{id}/edit")
    public String editPromotionForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit form for promotion ID: {}", id);

        Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
        if (promotionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Promotion not found");
            return "redirect:/admin/promotions";
        }

        Promotion promotion = promotionOpt.get();
        PromotionFormDTO promotionForm = mapEntityToDto(promotion);
        
        model.addAttribute("promotionForm", promotionForm);
        model.addAttribute("promotion", promotion);
        model.addAttribute("isEdit", true);
        model.addAttribute("activeMenu", "promotions");
        addFormAttributes(model);

        return "admin/promotions/form";
    }

    /**
     * Handle edit promotion form submission
     */
    @PostMapping("/{id}/edit")
    public String updatePromotion(@PathVariable Integer id,
                                 @Valid @ModelAttribute("promotionForm") PromotionFormDTO promotionForm,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        logger.info("Updating promotion with ID: {}", id);

        // Only do custom validation if basic validation passes
        if (!bindingResult.hasErrors()) {
            validatePromotionFields(promotionForm, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in promotion form: {}", bindingResult.getAllErrors());
            Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
            if (promotionOpt.isPresent()) {
                model.addAttribute("promotion", promotionOpt.get());
                model.addAttribute("isEdit", true);
            }
            model.addAttribute("activeMenu", "promotions");
            addFormAttributes(model);
            return "admin/promotions/form";
        }

        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            if (currentUser == null) {
                throw new RuntimeException("Current user not found");
            }

            Promotion promotion = promotionService.updatePromotion(id, promotionForm, currentUser);
            
            redirectAttributes.addFlashAttribute("success", 
                "Promotion '" + promotion.getName() + "' updated successfully");
            return "redirect:/admin/promotions/" + promotion.getPromotionId();

        } catch (IllegalArgumentException e) {
            logger.error("Error updating promotion: {}", e.getMessage());
            bindingResult.rejectValue("code", "error.code", e.getMessage());
            Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
            if (promotionOpt.isPresent()) {
                model.addAttribute("promotion", promotionOpt.get());
                model.addAttribute("isEdit", true);
            }
            model.addAttribute("activeMenu", "promotions");
            addFormAttributes(model);
            return "admin/promotions/form";
        } catch (Exception e) {
            logger.error("Unexpected error updating promotion", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
            return "redirect:/admin/promotions";
        }
    }

    /**
     * Toggle promotion active status
     */
    @PostMapping("/{id}/toggle-status")
    public String togglePromotionStatus(@PathVariable Integer id,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        logger.info("Toggling status for promotion ID: {}", id);

        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            if (currentUser == null) {
                throw new RuntimeException("Current user not found");
            }

            Promotion promotion = promotionService.togglePromotionStatus(id, currentUser);
            
            String statusMessage = promotion.getIsActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("success", 
                "Promotion '" + promotion.getName() + "' " + statusMessage + " successfully");

        } catch (IllegalArgumentException e) {
            logger.error("Error toggling promotion status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error toggling promotion status", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
        }

        return "redirect:/admin/promotions/" + id;
    }

    /**
     * Delete promotion
     */
    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Integer id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Deleting promotion with ID: {}", id);

        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            if (currentUser == null) {
                throw new RuntimeException("Current user not found");
            }

            promotionService.deletePromotion(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Promotion deleted successfully");

        } catch (IllegalArgumentException e) {
            logger.error("Error deleting promotion: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting promotion", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred");
        }

        return "redirect:/admin/promotions";
    }

    // ==================== Helper Methods ====================

    /**
     * Add common form attributes to model - simplified for essential fields only
     */
    private void addFormAttributes(Model model) {
        model.addAttribute("promotionTypes", Promotion.PromotionType.values());
        model.addAttribute("scopeTypes", Promotion.ScopeType.values());
        model.addAttribute("statusOptions", Promotion.PromotionStatus.values());

        // Add data for scope selection - only categories in simplified system
        model.addAttribute("categories", categoryService.getAllCategories());
    }

    /**
     * Map entity to DTO for editing
     */
    private PromotionFormDTO mapEntityToDto(Promotion promotion) {
        PromotionFormDTO dto = new PromotionFormDTO();
        dto.setPromotionId(promotion.getPromotionId());
        dto.setName(promotion.getName());
        dto.setCode(promotion.getCode());
        dto.setDescription(promotion.getDescription());
        dto.setPromotionType(promotion.getPromotionType());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        dto.setMinOrderValue(promotion.getMinOrderValue());
        dto.setScopeType(promotion.getScopeType());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setIsActive(promotion.getIsActive());
        dto.setStatus(promotion.getStatus());
        dto.setUsageLimitPerUser(promotion.getUsageLimitPerUser());
        dto.setTotalUsageLimit(promotion.getTotalUsageLimit());
        
        // Map scope-specific relationships - only categories in simplified system
        if (!promotion.getApplicableCategories().isEmpty()) {
            dto.setCategoryIds(promotion.getApplicableCategories().stream()
                .map(Category::getCategoryId).toList());
        }
        
        return dto;
    }

    /**
     * Get promotion edit status information (AJAX endpoint)
     */
    @GetMapping("/{id}/edit-status")
    @ResponseBody
    public Map<String, Object> getPromotionEditStatus(@PathVariable Integer id) {
        logger.info("Getting edit status for promotion ID: {}", id);
        return promotionService.getPromotionEditStatus(id);
    }

    /**
     * Toggle promotion active status (AJAX endpoint)
     */
    @PostMapping("/{id}/toggle-active")
    @ResponseBody
    public Map<String, Object> togglePromotionActive(@PathVariable Integer id, Authentication authentication) {
        logger.info("Toggling active status for promotion ID: {}", id);

        Map<String, Object> result = Map.of("success", false);

        try {
            User currentUser = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Optional<Promotion> optionalPromotion = promotionService.findById(id);
            if (optionalPromotion.isPresent()) {
                Promotion promotion = optionalPromotion.get();

                // Toggle active status
                promotion.setIsActive(!promotion.getIsActive());
                promotion.setUpdatedBy(currentUser);

                promotionService.save(promotion);

                result = Map.of(
                    "success", true,
                    "id", promotion.getPromotionId(),
                    "isActive", promotion.getIsActive(),
                    "message", "Promotion " + (promotion.getIsActive() ? "activated" : "deactivated") + " successfully"
                );
            }
        } catch (Exception e) {
            logger.error("Error toggling promotion active status: {}", e.getMessage(), e);
            result = Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }

        return result;
    }

    /**
     * Custom validation for promotion fields
     */
    private void validatePromotionFields(PromotionFormDTO promotionForm, BindingResult bindingResult) {
        // Validate percentage discount constraints
        if (promotionForm.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
            // For percentage discounts, discount value should be between 0.01 and 100
            if (promotionForm.getDiscountValue() != null &&
                promotionForm.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                bindingResult.rejectValue("discountValue", "error.discountValue",
                    "Percentage discount cannot exceed 100%");
            }
        }

        // Validate that max discount amount makes sense for percentage discounts
        if (promotionForm.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT &&
            promotionForm.getMaxDiscountAmount() != null &&
            promotionForm.getMinOrderValue() != null &&
            promotionForm.getDiscountValue() != null) {

            // Calculate what the discount would be at minimum order value
            BigDecimal percentageAsDecimal = promotionForm.getDiscountValue().divide(new BigDecimal("100"));
            BigDecimal discountAtMinOrder = promotionForm.getMinOrderValue().multiply(percentageAsDecimal);

            // If max discount is less than what would be applied at min order, it's probably wrong
            if (promotionForm.getMaxDiscountAmount().compareTo(discountAtMinOrder) < 0) {
                bindingResult.rejectValue("maxDiscountAmount", "error.maxDiscountAmount",
                    "Max discount amount seems too low compared to minimum order value and discount percentage");
            }
        }

        // Validate fixed amount discount constraints
        if (promotionForm.getPromotionType() == Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT &&
            promotionForm.getMinOrderValue() != null &&
            promotionForm.getDiscountValue() != null) {

            // Fixed discount shouldn't exceed minimum order value
            if (promotionForm.getDiscountValue().compareTo(promotionForm.getMinOrderValue()) >= 0) {
                bindingResult.rejectValue("discountValue", "error.discountValue",
                    "Fixed discount amount should be less than minimum order value");
            }
        }
    }
}
