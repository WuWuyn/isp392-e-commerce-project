package com.example.isp392.controller;

import com.example.isp392.model.User;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Controller for handling admin-related requests
 * This controller manages the admin login page and admin panel pages
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final UserService userService;
    private final AdminService adminService;
    
    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     * 
     * @param userService service for user-related operations
     * @param adminService service for admin-specific operations
     */
    public AdminController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }
    
    /**
     * Display admin login page
     * This page is accessible to everyone, but only admin users can successfully login
     * 
     * @return the admin login view
     */
    @GetMapping("/login")
    public String showAdminLoginPage(Model model) {
        // Check if user is already authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            // If user is authenticated and has ADMIN role, redirect to product management
            return "redirect:/admin/products";
        }
        
        // No active menu for login page
        model.addAttribute("activeMenu", "");
        
        return "admin/admin-login";
    }
    
    /**
     * Display product management page
     * This page is only accessible to authenticated users with ADMIN role
     * 
     * @param model Model to add attributes
     * @return the product management view
     */
    @GetMapping("/products")
    public String showProductManagementPage(Model model) {
        // Use AdminService to get the current admin user
        Optional<User> adminUserOpt = adminService.getCurrentAdminUser();
        
        if (adminUserOpt.isPresent()) {
            User adminUser = adminUserOpt.get();
            // Add user information to model for display in admin topbar
            model.addAttribute("user", adminUser);
            model.addAttribute("roles", userService.getUserRoles(adminUser));
            
            // Extract first name for convenience (though the topbar will do this via Thymeleaf)
            String firstName = adminService.extractFirstName(adminUser.getFullName());
            model.addAttribute("firstName", firstName);
        }
        
        // Add active menu information for sidebar highlighting
        model.addAttribute("activeMenu", "product");
        model.addAttribute("activeSubMenu", "product-list");
        
        // Add any product-related data here
        // model.addAttribute("products", productService.getAllProducts());
        
        return "admin/product/product-management";
    }
    
    /**
     * Display dashboard page
     * This page is only accessible to authenticated users with ADMIN role
     * 
     * @param model Model to add attributes
     * @return the dashboard view
     */
    @GetMapping("/dashboard")
    public String showDashboardPage(Model model) {
        // Use AdminService to get the current admin user
        Optional<User> adminUserOpt = adminService.getCurrentAdminUser();
        
        if (adminUserOpt.isPresent()) {
            User adminUser = adminUserOpt.get();
            model.addAttribute("user", adminUser);
            model.addAttribute("roles", userService.getUserRoles(adminUser));
            String firstName = adminService.extractFirstName(adminUser.getFullName());
            model.addAttribute("firstName", firstName);
        }
        
        // Add active menu information for sidebar highlighting
        model.addAttribute("activeMenu", "dashboard");
        
        return "admin/dashboard";
    }
    
    /**
     * Default admin page - redirects to product management
     * 
     * @return redirect to product management page
     */
    @GetMapping({"", "/"})
    public String defaultAdminPage(Model model) {
        model.addAttribute("activeMenu", "product");
        return "redirect:/admin/products";
    }
}
