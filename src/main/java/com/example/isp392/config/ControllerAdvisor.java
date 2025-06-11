package com.example.isp392.config;

import com.example.isp392.model.User;
import com.example.isp392.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

/**
 * Controller Advisor to automatically add common attributes to all controllers' models
 * This ensures user information is available in all templates, including the header
 */
@ControllerAdvice
public class ControllerAdvisor {

    private final UserService userService;

    public ControllerAdvisor(UserService userService) {
        this.userService = userService;
    }

    /**
     * Add authenticated user data to all models automatically
     * This ensures the header can always display the user's full name and roles
     * 
     * @param model The model to add attributes to
     */
    private static final Logger logger = LoggerFactory.getLogger(ControllerAdvisor.class);
    
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
            
            String email = null;
            
            // Get email - which could be in the authentication name directly or in OAuth2 attributes
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                // For OAuth2 authentication
                org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                        (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                
                // Get email from attributes
                email = oauth2User.getAttribute("email");
                logger.info("OAuth2 authentication detected. Email from attributes: {}", email);
            } else {
                // For form-based authentication
                email = authentication.getName();
                logger.info("Form-based authentication detected. Email: {}", email);
            }
            
            if (email == null) {
                logger.warn("Could not determine email for authenticated user. Auth name: {}", authentication.getName());
                return;
            }
            
            try {
                // Look up user by email in both cases
                Optional<User> userOptional = userService.findByEmail(email);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    logger.info("User found: {} ({})", user.getFullName(), user.getEmail());
                    model.addAttribute("user", user);
                    model.addAttribute("userRoles", userService.getUserRoles(user));
                } else {
                    logger.warn("No user found with email: {}", email);
                }
            } catch (Exception e) {
                logger.error("Error loading user data: {}", e.getMessage(), e);
            }
        }
    }
}
