package com.example.isp392.config;

import com.example.isp392.model.Cart;
import com.example.isp392.service.CartService;
import com.example.isp392.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;

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
    @Autowired
    private SystemSettingService systemSettingService;

    @ModelAttribute("globalSettings")
    public Map<String, String> addGlobalSettingsToModel() {
        // Liệt kê tất cả các key cài đặt mà bạn muốn truy cập toàn cục (ví dụ: trong footer)
        List<String> settingKeys = List.of(
                // Cài đặt footer (đã có)
                "contact_email",
                "social_facebook",
                "social_instagram",
                "social_zalo",

                // THÊM MỚI: Cài đặt Hero Banner
                "hero_background_image",
                "hero_title",
                "hero_description",
                "hero_button_text",
                "hero_button_link"
        );
        return systemSettingService.getSettings(settingKeys);
    }
} 