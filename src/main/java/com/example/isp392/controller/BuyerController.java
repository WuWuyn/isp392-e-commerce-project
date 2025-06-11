package com.example.isp392.controller;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.User;
import com.example.isp392.service.CartService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for buyer-related operations
 * Handles both local and OAuth2 (Google) authentication
 */
@Controller
@RequestMapping("/buyer")
public class BuyerController {

    private static final Logger log = LoggerFactory.getLogger(BuyerController.class);

    private final UserService userService;
    private final CartService cartService;

    /**
     * Constructor with explicit dependency injection
     * This is preferred over field injection with @Autowired as it makes dependencies clear,
     * ensures they're required, and makes testing easier
     *
     * @param userService Service for user-related operations
     * @param cartService Service for cart-related operations
     */
    public BuyerController(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    /**
     * Display login page
     * @return login page view
     */
    @GetMapping("/login")
    public String showLoginPage() {
        // Check if user is already authenticated
        if (isUserAuthenticated()) {

            return "redirect:/buyer/account-info";
        }
        return "buyer/login";
    }

    /**
     * Display signup page
     * @param model Model to add attributes
     * @return signup page view
     */
    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        // Check if user is already authenticated
        if (isUserAuthenticated()) {
            return "redirect:/buyer/account-info";
        }

        // Add registration DTO to model if not already present
        if (!model.containsAttribute("userRegistrationDTO")) {
            model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        }

        return "buyer/signup";
    }

    /**
     * Process registration form submission
     * @param userRegistrationDTO user registration data
     * @param bindingResult validation result
     * @param redirectAttributes for flash attributes
     * @return redirect to login page or back to signup page with errors
     */
    @PostMapping("/register")
    public String registerBuyer(
            @Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // Check if passwords match
        if (!userRegistrationDTO.getPassword().equals(userRegistrationDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.userRegistrationDTO", "Passwords do not match");
        }

        // If there are validation errors, return to signup page
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userRegistrationDTO", bindingResult);
            redirectAttributes.addFlashAttribute("userRegistrationDTO", userRegistrationDTO);
            return "redirect:/buyer/signup";
        }

        try {
            // Register the buyer and log the registration success
            userService.registerBuyer(userRegistrationDTO);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login with your credentials.");
            return "redirect:/buyer/login";
        } catch (Exception e) {
            // Handle registration errors
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("userRegistrationDTO", userRegistrationDTO);
            return "redirect:/buyer/signup";
        }
    }

    /**
     * Display account info page
     * @param model Model to add attributes
     * @return account info page view
     */
    @GetMapping("/account-info")
    public String showAccountInfo(Model model, Authentication authentication) {
        // Get user with OAuth2 support
        User user = getCurrentUser(authentication);
        if (user == null) {
            log.warn("No user found in showAccountInfo");
            return "redirect:/buyer/login";
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

        return "buyer/account-info";
    }

    /**
     * Handle OAuth2 login success
     * @param authentication OAuth2 authentication object
     * @param model Model to add attributes
     * @return redirect to account info page
     */
    @GetMapping("/oauth2-login-success")
    public String handleOAuth2LoginSuccess(Authentication authentication, Model model) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            // Log the successful OAuth2 login with detailed attributes
            log.info("OAuth2 login success for user: email={}, name={}", email, name);
            log.debug("OAuth2 user attributes: {}", oauth2User.getAttributes());

            // Find user in database to verify proper creation/update
            Optional<User> userOptional = userService.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                log.info("User found in database: id={}, name={}",
                         user.getUserId(), user.getFullName());
            } else {
                log.warn("OAuth2 user not found in database after login success");
            }
        } else {
            log.warn("Non-OAuth2 authentication in OAuth2 success handler");
        }

        return "redirect:/buyer/account-info";
    }

    /**
     * Display edit account info page
     * @param model Model to add attributes
     * @return edit account info page view
     */
    @GetMapping("/edit-info")
    public String showEditInfoPage(Model model) {
        // Get authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/buyer/login";
        }

        // Add user and roles to model
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));

        // Check authentication type
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = authentication instanceof OAuth2AuthenticationToken;
        model.addAttribute("isOAuth2User", isOAuth2User);

        return "buyer/account-edit-info";
    }

    /**
     * Process update user info form submission
     * @param fullName user's full name
     * @param phone user's phone number
     * @param gender user's gender (0: Male, 1: Female, 2: Other)
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
                return "redirect:/buyer/login";
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
            return "redirect:/buyer/account-info";
        } catch (Exception e) {
            // Handle update errors
            log.error("Error updating user info: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/edit-info";
        }
    }


    /**
     * Display change password page
     * @param model Model to add attributes
     * @return change password page view
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        try {
            // Get authentication from SecurityContextHolder
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Check if this is an OAuth2 authentication
            boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
            model.addAttribute("isOAuth2User", isOAuth2User);

            String email;
            if (isOAuth2User) {
                OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
                email = oauth2User.getAttribute("email");
                log.debug("OAuth2 user accessing change password page: {}", email);
            } else {
                email = auth.getName();
                log.debug("Regular user accessing change password page: {}", email);
            }

            // Get user by email
            Optional<User> userOpt = userService.findByEmail(email);

            if(userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "buyer/account-change-password";
            } else {
                // Redirect to login if user not found (shouldn't happen with proper authentication)
                return "redirect:/buyer/login";
            }
        } catch (Exception e) {
            log.error("Error displaying change password form: {}", e.getMessage());
            // Handle any errors
            return "redirect:/buyer/login";
        }
    }
    /**
     * Process change password form submission
     * @param currentPassword user's current password
     * @param newPassword user's new password
     * @param confirmPassword confirmation of new password
     * @param redirectAttributes for flash attributes
     * @return redirect to account info page or back to change password page with errors
     */
    @PostMapping("/update-password")
    public String updatePassword(
            @ModelAttribute("currentPassword") String currentPassword,
            @ModelAttribute("newPassword") String newPassword,
            @ModelAttribute("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        // Check if user is authenticated with OAuth2
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;

        // Prevent Google users from changing passwords
        if (isOAuth2User) {
            log.warn("Google OAuth2 user attempted to change password");
            redirectAttributes.addFlashAttribute("errorMessage", "Google account users cannot change their password here. Please use your Google account settings.");
            return "redirect:/buyer/change-password";
        }

        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/buyer/change-password";
        }

        try {
            // Use the existing auth variable
            String email = auth.getName();

            // Update password
            boolean updated = userService.updatePassword(email, currentPassword, newPassword);

            if (updated) {
                // Add success message
                redirectAttributes.addFlashAttribute("successMessage", "Your password has been updated successfully.");
                return "redirect:/buyer/change-password";
            } else {
                // If password not updated, redirect to change password page
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
                return "redirect:/buyer/change-password";
            }
        } catch (Exception e) {
            // Handle update errors
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/change-password";
        }
    }

    /**
     * Display orders page (placeholder)
     * @param model Model to add attributes
     * @return orders page view
     */
    @GetMapping("/orders")
    public String showOrdersPage(Model model) {
        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("activeTab", "orders");
            // This would normally include order data from an OrderService
            return "buyer/orders";
        } else {
            return "redirect:/buyer/login";
        }
    }

    /**
     * Display the shopping cart page
     * This page is only accessible to authenticated users
     *
     * @param model Model to add attributes to the view
     * @return the cart view or redirect to login if not authenticated
     */
    @GetMapping("/cart")
    public String showCart(Model model) {
        try {
            // Use the existing auth variable
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            // Get user by email
            Optional<User> userOpt = userService.findByEmail(email);

            if(userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                model.addAttribute("cart", cartService.getCartForUser(user));

                return "buyer/cart";
            } else {
                return "redirect:/buyer/login";
            }
        } catch (Exception e) {
            log.error("Error displaying cart: {}", e.getMessage());
            return "redirect:/buyer/login";
        }
    }

    // Helper methods for authentication
    
    /**
     * Check if user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }
    
    /**
     * Get current user from SecurityContextHolder
     * @return User object or null if not authenticated
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUser(auth);
    }
    
    /**
     * Get user from Authentication object with OAuth2 support
     * @param auth Authentication object
     * @return User object or null if not authenticated
     */
    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        
        String email = null;
        
        // Handle different authentication types
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }
        
        if (email == null) {
            log.warn("Email is null for authenticated user");
            return null;
        }
        
        // Find user by email
        Optional<User> userOptional = userService.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            log.warn("User not found in database: {}", email);
        }
        
        return userOptional.orElse(null);
    }
}
