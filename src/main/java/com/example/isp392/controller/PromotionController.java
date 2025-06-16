package com.example.isp392.controller;

import com.example.isp392.dto.PromotionDTO;
import com.example.isp392.service.PromotionService;
import com.example.isp392.service.UserService;
import com.example.isp392.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {
    private final PromotionService promotionService;
    private final UserService userService;

    public PromotionController(PromotionService promotionService, UserService userService) {
        this.promotionService = promotionService;
        this.userService = userService;
    }

    @GetMapping
    public String listPromotions(Model model) {
        List<PromotionDTO> promotions = promotionService.getAllPromotions();
        model.addAttribute("promotions", promotions);
        model.addAttribute("activeMenu", "promotions");
        return "admin/promotions";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<PromotionDTO> getPromotion(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PostMapping
    public String savePromotion(@Valid @ModelAttribute PromotionDTO promotionDTO,
                               RedirectAttributes redirectAttributes) {
        try {
            if (promotionDTO.getPromotionId() == null) {
                // New promotion: set createdByAdminId from authenticated user
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String currentUserName = authentication.getName();
                Optional<User> currentUser = userService.findByEmail(currentUserName);

                if (currentUser.isPresent()) {
                    promotionDTO.setCreatedByAdminId(currentUser.get().getUserId());
                } else {
                    throw new IllegalStateException("Authenticated user not found.");
                }
                promotionService.createPromotion(promotionDTO);
                redirectAttributes.addFlashAttribute("successMessage", "Promotion created successfully!");
            } else {
                promotionService.updatePromotion(promotionDTO.getPromotionId(), promotionDTO);
                redirectAttributes.addFlashAttribute("successMessage", "Promotion updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/disable")
    public String disablePromotion(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.disablePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion disabled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
} 