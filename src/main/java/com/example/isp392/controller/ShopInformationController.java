package com.example.isp392.controller;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.model.Book;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.service.ShopService;
import com.example.isp392.service.UserService;
import com.example.isp392.service.BookService;
import com.example.isp392.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
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
    private final BookService bookService;
    private final FileStorageService fileStorageService;

    /**
     * Constructor for dependency injection
     */
    public ShopInformationController(ShopService shopService, UserService userService,
                                     UserRepository userRepository, HttpSession httpSession, BookService bookService, FileStorageService fileStorageService) {
        this.shopService = shopService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.httpSession = httpSession;
        this.bookService = bookService;
        this.fileStorageService = fileStorageService;
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


    @PostMapping("/shop-information/save")
    public String saveShopInformation(
            @Valid @ModelAttribute("shop") ShopDTO shopDTO,
            BindingResult bindingResult,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            // Bỏ RequestParam cho identificationFile vì nó không có trên form edit
            RedirectAttributes redirectAttributes,
            Model model) {

        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            // Trả về đúng view edit nếu có lỗi
            return "seller/edit-shop-information";
        }

        try {
            // Ensure userId is set from current user (not from form)
            shopDTO.setUserId(user.getUserId());

            // Validate file uploads before processing
            if (logoFile != null && !logoFile.isEmpty()) {
                if (logoFile.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    model.addAttribute("errorMessage", "Logo file size must be less than 5MB");
                    model.addAttribute("user", user);
                    model.addAttribute("roles", userService.getUserRoles(user));
                    return "seller/edit-shop-information";
                }
                if (!isValidImageFile(logoFile)) {
                    model.addAttribute("errorMessage", "Logo must be a valid image file (JPEG, PNG, GIF, WEBP)");
                    model.addAttribute("user", user);
                    model.addAttribute("roles", userService.getUserRoles(user));
                    return "seller/edit-shop-information";
                }
            }

            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                if (coverImageFile.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    model.addAttribute("errorMessage", "Cover image file size must be less than 5MB");
                    model.addAttribute("user", user);
                    model.addAttribute("roles", userService.getUserRoles(user));
                    return "seller/edit-shop-information";
                }
                if (!isValidImageFile(coverImageFile)) {
                    model.addAttribute("errorMessage", "Cover image must be a valid image file (JPEG, PNG, GIF, WEBP)");
                    model.addAttribute("user", user);
                    model.addAttribute("roles", userService.getUserRoles(user));
                    return "seller/edit-shop-information";
                }
            }

            // Update shop information using service
            shopService.updateShop(shopDTO.getShopId(), shopDTO, logoFile, coverImageFile);

            redirectAttributes.addFlashAttribute("successMessage", "Shop information updated successfully!");
            return "redirect:/seller/shop-information";

        } catch (IOException e) {
            log.error("File upload error", e);
            model.addAttribute("errorMessage", "Error uploading file: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            return "seller/edit-shop-information";
        } catch (Exception e) {
            log.error("Error saving shop information", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            return "seller/edit-shop-information";
        }
    }

    /**
     * Validate if uploaded file is a valid image
     */
    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png");
    }

// File upload is now handled by ShopService using FileStorageService

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

    @GetMapping("/shop-information/edit")
    public String showEditShopInformationForm(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        Shop shop = shopService.getShopByUserId(user.getUserId());
        if (shop == null) {
            // Nếu chưa có shop, chuyển hướng về trang tạo mới (trang thông tin chính)
            return "redirect:/seller/shop-information";
        }

        ShopDTO shopDTO = new ShopDTO();
        BeanUtils.copyProperties(shop, shopDTO);

        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        model.addAttribute("shop", shopDTO);
        model.addAttribute("activeMenu", "shop-information"); // Để highlight sidebar

        return "seller/edit-shop-information"; // Trả về trang edit mới
    }
} 