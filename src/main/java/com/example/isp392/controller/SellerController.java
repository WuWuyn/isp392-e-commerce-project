package com.example.isp392.controller;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.User;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private static final Logger log = LoggerFactory.getLogger(SellerController.class);
    private final UserService userService;

    public SellerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "seller/seller-login";
    }

    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        return "seller/seller-signup";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/dashboard";
    }

    @GetMapping("/account")
    public String showAccountInfo(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/account-info";
    }

    @GetMapping("/edit-info")
    public String showEditInfoPage(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        return "seller/account-edit-info";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
            model.addAttribute("isOAuth2User", isOAuth2User);
            String email;
            if (isOAuth2User) {
                OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
                email = oauth2User.getAttribute("email");
                log.debug("OAuth2 seller accessing change password page: {}", email);
            } else {
                email = auth.getName();
                log.debug("Regular seller accessing change password page: {}", email);
            }
            Optional<User> userOpt = userService.findByEmail(email);
            if(userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/account-change-password";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying change password form: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(
            @ModelAttribute("currentPassword") String currentPassword,
            @ModelAttribute("newPassword") String newPassword,
            @ModelAttribute("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
        if (isOAuth2User) {
            log.warn("Google OAuth2 seller attempted to change password");
            redirectAttributes.addFlashAttribute("errorMessage", "Google account users cannot change their password here. Please use your Google account settings.");
            return "redirect:/seller/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/seller/change-password";
        }
        try {
            String email = auth.getName();
            boolean updated = userService.updatePassword(email, currentPassword, newPassword);
            if (updated) {
                redirectAttributes.addFlashAttribute("successMessage", "Your password has been updated successfully.");
                return "redirect:/seller/change-password";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
                return "redirect:/seller/change-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/change-password";
        }
    }

    @GetMapping("/orders")
    public String showOrdersPage(Model model) {
        // Placeholder for seller orders
        return "seller/orders";
    }

    @GetMapping("/cart")
    public String showCart(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if(userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                return "seller/cart";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying seller cart: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    // Helper methods for authentication
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUser(auth);
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = null;
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }
        if (email == null) return null;
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.orElse(null);
    }
}
