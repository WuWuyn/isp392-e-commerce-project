package com.example.isp392.config;

import com.example.isp392.model.Cart;
import com.example.isp392.model.User;
import com.example.isp392.service.CartService;
import com.example.isp392.service.SystemSettingService;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @ModelAttribute("cart")
    public Cart addCartToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            // Assuming your CartService has a method to get the cart for the authenticated user
            // You might need to adjust this based on how your CartService identifies the user
            return cartService.getCartForCurrentUser();
        }
        return new Cart(); // Return an empty cart if not authenticated or anonymous
    }

    @ModelAttribute("cartTotalQuantity")
    public int addCartTotalQuantityToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                Optional<User> userOpt = userService.findByEmail(authentication.getName());
                if (userOpt.isPresent()) {
                    return cartService.getUniqueItemCount(userOpt.get());
                }
            } catch (Exception e) {
                log.warn("Error calculating cart unique item count: {}", e.getMessage());
            }
        }
        return 0;
    }

    @Autowired
    private SystemSettingService systemSettingService;

    @ModelAttribute("globalSettings")
    public Map<String, String> addGlobalSettingsToModel() {
        try {
            // Đảm bảo danh sách này giống hệt với danh sách trong AdminController
            List<String> settingKeys = List.of(
                    "hero_background_image",
                    "hero_title",
                    "hero_description",
                    "hero_button_text",
                    "hero_button_link",
                    "contact_email",
                    "contact_province",
                    "contact_district",
                    "contact_ward",
                    "social_facebook",
                    "social_instagram",
                    "social_zalo"
            );
            return systemSettingService.getSettings(settingKeys);
        } catch (Exception e) {
            log.warn("System settings not available (table may not exist yet): {}", e.getMessage());
            // Return default settings to prevent template errors
            Map<String, String> defaultSettings = new HashMap<>();
            defaultSettings.put("contact_email", "contact@readhub.com");
            defaultSettings.put("contact_province", "Ho Chi Minh City");
            defaultSettings.put("contact_district", "District 1");
            defaultSettings.put("contact_ward", "Ben Nghe Ward");
            defaultSettings.put("social_facebook", "https://facebook.com/readhub");
            defaultSettings.put("social_instagram", "https://instagram.com/readhub");
            defaultSettings.put("social_zalo", "https://zalo.me/readhub");
            defaultSettings.put("hero_title", "Welcome to ReadHub");
            defaultSettings.put("hero_description", "Discover your next favorite book from our vast collection");
            defaultSettings.put("hero_button_text", "Shop Now");
            defaultSettings.put("hero_button_link", "/books");
            defaultSettings.put("hero_background_image", "/images/hero-bg.jpg");
            return defaultSettings;
        }
    }
    @ModelAttribute("currentUser")
    public User addCurrentUserToModel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String username = "";
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            // Fallback for cases where principal is just the username string
            username = principal.toString();
        }

        if (username.isEmpty()) {
            return null;
        }

        return userService.findByEmail(username).orElse(null);
    }

    /**
     * Adds a list of roles for the currently authenticated user to the model.
     * This makes the 'currentUserRoles' list available in all Thymeleaf templates.
     *
     * @param currentUser The User object provided by the addCurrentUserToModel method.
     * @return A list of role names (e.g., "ADMIN", "SELLER"), or an empty list.
     */
    @ModelAttribute("currentUserRoles")
    public List<String> addCurrentUserRolesToModel(@ModelAttribute("currentUser") User currentUser) {
        if (currentUser != null) {
            return userService.getUserRoles(currentUser);
        }
        return Collections.emptyList();
    }
    @ModelAttribute("_csrf")
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
} 