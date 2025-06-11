package com.example.isp392.controller;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.User;
import com.example.isp392.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

//    @PostMapping("/signup")
//    public String registerSeller(
//            @Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
//            BindingResult bindingResult,
//            RedirectAttributes redirectAttributes) {
//        if (bindingResult.hasErrors()) {
//            return "seller/seller-signup";
//        }
//        try {
//            userService.registerNewUser(userRegistrationDTO, "SELLER");
//            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
//            return "redirect:/seller/login";
//        } catch (Exception e) {
//            log.error("Error registering seller: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/seller/signup";
//        }
//    }

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

    /**
     * Display account info page
     *
     * @param model Model to add attributes
     * @return account info page view
     */
    @GetMapping("/account-info")
    public String showAccountInfo(Model model, Authentication authentication) {
        // Get user with OAuth2 support
        User user = getCurrentUser(authentication);
        if (user == null) {
            log.warn("No user found in showAccountInfo");
            return "redirect:/seller/login";
        }

        // Check if this is an OAuth2 authentication
        boolean isOAuth2User = authentication instanceof OAuth2AuthenticationToken;
        model.addAttribute("isOAuth2User", isOAuth2User);

        // If OAuth2 user, add OAuth2 user details to model
        if (isOAuth2User) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            model.addAttribute("oauth2User", oauth2User);

            // Log OAuth2 attributes for debugging
            log.debug("OAuth2 user attributes: {}", oauth2User.getAttributes());
        }

        // Add user and roles to model
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));

        log.debug("Showing account info for user: id={}, name={}",
                user.getUserId(), user.getFullName());

        return "seller/account-info";
    }

    @GetMapping("/edit-info")
    public String showEditInfoPage(Model model) {
        // Get authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Add user and roles to model
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));

        // Check authentication type
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = authentication instanceof OAuth2AuthenticationToken;
        model.addAttribute("isOAuth2User", isOAuth2User);

        return "seller/account-edit-info";
    }

    /**
     * Process update user info form submission
     *
     * @param fullName           user's full name
     * @param phone              user's phone number
     * @param gender             user's gender (0: Male, 1: Female, 2: Other)
     * @param redirectAttributes for flash attributes
     * @return redirect to account info page
     */
    @PostMapping("/update-info")
    public String updateUserInfo(
            @ModelAttribute("fullName") String fullName,
            @ModelAttribute("phone") String phone,
            @ModelAttribute("gender") int gender,
            @ModelAttribute("dateOfBirth") String dateOfBirth,
            @RequestParam(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            // Get authenticated user with OAuth2 support
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found. Please login again.");
                return "redirect:/seller/login";
            }

            String email = currentUser.getEmail();
            log.debug("Updating info for user: {}", email);

            // Parse date from string
            LocalDate parsedDate = null;
            try {
                if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                    parsedDate = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            } catch (Exception e) {
                log.warn("Error parsing date: {}", e.getMessage());
                // Continue with null date if parsing fails
            }

            // Process profile picture if uploaded
            String profilePicUrl = null;
            if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
                try {
                    // Generate unique filename
                    String originalFilename = profilePictureFile.getOriginalFilename();
                    String fileName = System.currentTimeMillis() + "_" +
                            (originalFilename != null ? originalFilename : "profile.jpg");

                    // Get upload directory path - use the same path configured in FileUploadConfig
                    String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/profile-pictures/";
                    File uploadDirectory = new File(uploadDir);
                    if (!uploadDirectory.exists()) {
                        uploadDirectory.mkdirs();
                    }

                    // Save file to server
                    File destFile = new File(uploadDir + File.separator + fileName);
                    profilePictureFile.transferTo(destFile);

                    // Set profile picture URL that will be mapped by our resource handler
                    profilePicUrl = "/uploads/profile-pictures/" + fileName;
                    log.debug("Profile picture saved: {}", profilePicUrl);
                } catch (Exception e) {
                    // Log error but continue with other user info updates
                    log.error("Error uploading profile picture: {}", e.getMessage());
                }
            }

            // Update user info with profile picture
            userService.updateUserInfo(email, fullName, phone, gender, parsedDate, profilePicUrl);
            log.info("User info updated successfully for: {}", email);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Your information has been updated successfully.");
            return "redirect:/seller/account-info";
        } catch (Exception e) {
            // Handle update errors
            log.error("Error updating user info: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/edit-info";
        }
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
            if (userOpt.isPresent()) {
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
            if (userOpt.isPresent()) {
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
