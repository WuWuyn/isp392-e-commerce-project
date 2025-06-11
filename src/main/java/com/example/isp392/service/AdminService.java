package com.example.isp392.service;

import com.example.isp392.model.User;
import com.example.isp392.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.Optional;

/**
 * Service for admin-specific operations
 * This service provides functionality specifically for admin users
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     *
     * @param userRepository repository for user data access
     */
    public AdminService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Get the currently authenticated admin user
     *
     * @return Optional containing the admin user if found and authenticated
     */
    @Transactional(readOnly = true)
    public Optional<User> getCurrentAdminUser() {
        // Get the current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }

        // Find the user by email
        return userRepository.findByEmail(auth.getName());
    }

    /**
     * Extract the first name from a full name
     *
     * @param fullName the full name to extract from
     * @return the first name (first word) or the full name if no spaces
     */
    public String extractFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }

        // Split by space and return the first part
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
    }

    public void addAdminInfoToModel(Model model) {
        getCurrentAdminUser().ifPresent(adminUser -> {
            model.addAttribute("user", adminUser);
            model.addAttribute("roles", userService.getUserRoles(adminUser));
            model.addAttribute("firstName", extractFirstName(adminUser.getFullName()));
        });
    }
}
