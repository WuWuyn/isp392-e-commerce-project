package com.example.isp392.controller;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.service.ShopService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing shop information
 * Handles operations related to shop profile for sellers
 */
@Controller
@RequestMapping("/seller")
public class ShopInformationController {

    private static final Logger log = LoggerFactory.getLogger(ShopInformationController.class);
    
    private final ShopService shopService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final HttpSession httpSession;
    
    /**
     * Constructor for dependency injection
     */
    public ShopInformationController(ShopService shopService, UserService userService, 
                                    UserRepository userRepository, HttpSession httpSession) {
        this.shopService = shopService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.httpSession = httpSession;
    }
    
    /**
     * Display shop information page
     * @param model the Spring MVC model
     * @return the shop information view
     */
    @GetMapping("/shop-information")
    public String viewShopInformation(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        
        // Get user's roles for sidebar
        List<String> roles = userService.getUserRoles(user);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        
        // Get shop information if exists
        ShopDTO shopDTO = new ShopDTO();
        Shop shop = shopService.getShopByUserId(user.getUserId());
        
        if (shop != null) {
            // Copy properties from entity to DTO
            BeanUtils.copyProperties(shop, shopDTO);
            shopDTO.setUserId(user.getUserId());
            model.addAttribute("shop", shopDTO);
            model.addAttribute("isEdit", true);
        } else {
            // No shop exists yet, provide empty form
            shopDTO.setUserId(user.getUserId());
            shopDTO.setContactEmail(user.getEmail());
            model.addAttribute("shop", shopDTO);
            model.addAttribute("isEdit", false);
        }
        
        return "seller/shop-information";
    }
    
    /**
     * Process form to create/update shop information
     * @param shopDTO the shop data from form
     * @param bindingResult validation results
     * @param logoFile shop logo file
     * @param coverImage shop cover image file
     * @param identificationFile identification document file
     * @param redirectAttributes attributes for redirect
     * @param model the Spring MVC model
     * @return redirect to shop information page or form with errors
     */
    @PostMapping("/shop-information/save")
    public String saveShopInformation(
            @Valid @ModelAttribute("shop") ShopDTO shopDTO,
            BindingResult bindingResult,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImage,
            @RequestParam(value = "identificationFile", required = false) MultipartFile identificationFile,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Add user and roles to model for sidebar
            List<String> roles = userService.getUserRoles(user);
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("isEdit", shopDTO.getShopId() != null);
            return "seller/shop-information"; // Return to form with errors
        }
        
        try {
            // Set the user ID
            shopDTO.setUserId(user.getUserId());
            
            // Handle file uploads if provided
            if (logoFile != null && !logoFile.isEmpty()) {
                String logoUrl = uploadFile(logoFile);
                shopDTO.setLogoUrl(logoUrl);
            }
            
            if (coverImage != null && !coverImage.isEmpty()) {
                String coverImageUrl = uploadFile(coverImage);
                shopDTO.setCoverImageUrl(coverImageUrl);
            }
            
            if (identificationFile != null && !identificationFile.isEmpty()) {
                String identificationFileUrl = uploadFile(identificationFile);
                shopDTO.setIdentificationFileUrl(identificationFileUrl);
            }
            
            // Set request date for new shops
            if (shopDTO.getShopId() == null) {
                shopDTO.setRequestAt(LocalDateTime.now());
                shopDTO.setApprovalStatus(Shop.ApprovalStatus.PENDING);
            }
            
            // Save shop information
            shopService.saveShopInformation(shopDTO);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Shop information saved successfully! " +
                (Shop.ApprovalStatus.PENDING.equals(shopDTO.getApprovalStatus()) ? 
                "Your information is pending approval." : ""));
                
            return "redirect:/seller/shop-information?success";
        } catch (Exception e) {
            log.error("Error saving shop information", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/seller/shop-information?error";
        }
    }
    
    /**
     * Mock method for file uploads - in a real application, this would handle proper file storage
     * @param file the uploaded file
     * @return URL to the uploaded file
     */
    private String uploadFile(MultipartFile file) throws IOException {
        // In a real application, this would upload to a file storage service or server
        // For this example, we'll just return a mock URL
        return "/uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
    }
    
    /**
     * Helper method to get the current authenticated user
     * @return User object or null if not authenticated or not found
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        
        // First try to get user ID from session (set by OAuth2LoginSuccessHandler)
        Integer userId = (Integer) httpSession.getAttribute("USER_ID");
        if (userId != null) {
            Optional<User> userById = userRepository.findById(userId);
            if (userById.isPresent()) {
                User user = userById.get();
                log.debug("Found user from session USER_ID: id={}, name={}", user.getUserId(), user.getFullName());
                return user;
            }
        }
        
        String email;
        
        // Check if authentication is from OAuth2 (Google)
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
            log.debug("Getting OAuth2 user with email: {}", email);
        } else {
            // Regular form login user
            email = auth.getName();
            log.debug("Getting regular user with email: {}", email);
        }
        
        if (email == null) {
            log.warn("Could not extract email from authentication: {}", auth.getPrincipal());
            return null;
        }
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("No user found for email: {}", email);
            return null;
        }
        
        User user = userOptional.get();
        log.debug("Found user: id={}, name={}", user.getUserId(), user.getFullName());
        return user;
    }
} 