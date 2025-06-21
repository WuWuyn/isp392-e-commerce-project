package com.example.isp392.controller;

import com.example.isp392.model.User;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser") && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
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
        
        return "admin/products";
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
     * Display blog management page
     * This page is only accessible to authenticated users with ADMIN role
     * 
     * @param model Model to add attributes
     * @return the blog management view
     */
    @GetMapping("/blog")
    public String showBlogManagementPage(Model model) {
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
        model.addAttribute("activeMenu", "blog");
        
        return "admin/blog-management";
    }
    
    /**
     * Default admin page - redirects to product management
     * 
     * @return redirect to product management page
     */
    @GetMapping({"", "/"})
    public String defaultAdminPage(Model model) {
        model.addAttribute("activeMenu", "product");
        return "redirect:/admin/dashboard";
    }

    // user management
    @GetMapping("/users")
    public String showUserManagementPage(
            @RequestParam(name = "page", defaultValue = "1") int pageNo,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        adminService.addAdminInfoToModel(model);

        // Tạo đối tượng Pageable. Chuyển pageNo từ 1-based sang 0-based
        Pageable pageable = PageRequest.of(pageNo - 1, size, Sort.by("userId").ascending());

        // Gọi phương thức searchUsers của bạn với Pageable
        Page<User> userPage = userService.searchUsers(keyword, role, pageable);


        model.addAttribute("userPage", userPage); // Đối tượng Page chính
        model.addAttribute("currentPage", pageNo); // Số trang hiện tại (1-based)
        model.addAttribute("totalPages", userPage.getTotalPages()); // Tổng số trang
        model.addAttribute("totalItems", userPage.getTotalElements()); // Tổng số user
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleFilter", role);
        model.addAttribute("statusFilter", status);

        // Đặt menu active
        model.addAttribute("activeMenu", "users");

        return "admin/user/user-list";
    }

    @GetMapping("/users/details/{id}")
    public String showUserDetailsPage(@PathVariable("id") Integer userId, Model model) {
        adminService.addAdminInfoToModel(model);
        try {
            User user = userService.findUserById(userId);
            model.addAttribute("user", user);
            model.addAttribute("activeMenu", "user");
            model.addAttribute("activeSubMenu", "user-details");
            return "admin/user/user-details";
        } catch (RuntimeException e) {
            return "redirect:/admin/users?error=UserNotFound";
        }
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer userId, Model model, RedirectAttributes redirectAttributes, Authentication authentication) { // Thêm Authentication
        try {
            User user = userService.findUserById(userId);
            adminService.addAdminInfoToModel(model); // Giả định phương thức này đã thêm thông tin admin hiện tại vào model

            // Lấy thông tin admin đang đăng nhập để so sánh ở frontend
            UserDetails loggedInUserDetails = (UserDetails) authentication.getPrincipal();
            User loggedInAdmin = userService.findByEmailDirectly(loggedInUserDetails.getUsername());
            model.addAttribute("loggedInAdminId", loggedInAdmin.getUserId());


            Set<String> currentUserRoles = user.getUserRoles().stream()
                    .map(ur -> ur.getRole().getRoleName())
                    .collect(Collectors.toSet());
            model.addAttribute("allRoles", userService.getAllRoles());
            model.addAttribute("currentUserRoles", currentUserRoles);
            model.addAttribute("user", user);
            model.addAttribute("activeMenu", "user");
            return "admin/user/edit-user";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found!");
            return "redirect:/admin/users";
        }
    }


    @PostMapping("/users/update/{id}")
    public String updateUserByAdmin(@PathVariable("id") Integer userId,
                                    @RequestParam(name = "isActive", required = false) Boolean isActive, // Thay đổi thành Boolean để xử lý trường hợp bị disable
                                    @RequestParam(name = "roles", required = false) List<String> roles,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        UserDetails loggedInUser = (UserDetails) authentication.getPrincipal();
        User adminUser = userService.findByEmailDirectly(loggedInUser.getUsername());

        // Lấy thông tin user cần cập nhật
        User userToUpdate = userService.findUserById(userId);

        // *** LOGIC BẢO VỆ MỚI ***
        // Nếu admin đang cố gắng chỉnh sửa chính mình
        if (userToUpdate.getUserId().equals(adminUser.getUserId())) {
            redirectAttributes.addFlashAttribute("warningMessage", "You cannot change your own roles or status.");
            // Chỉ cập nhật các thông tin khác nếu có, trong trường hợp này không có nên ta chuyển hướng luôn
            return "redirect:/admin/users/edit/" + userId;
        }


        // Kiểm tra nếu không phải tự sửa, vai trò phải được cung cấp
        if (roles == null || roles.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: A user must have at least one role.");
            return "redirect:/admin/users/edit/" + userId;
        }

        try {
            // isActive sẽ là null nếu checkbox bị disable, nên cần kiểm tra
            boolean newIsActiveStatus = (isActive != null) ? isActive : userToUpdate.isActive();

            userService.updateUserActivationStatus(userId, newIsActiveStatus);
            userService.updateUserRoles(userId, roles);

            redirectAttributes.addFlashAttribute("successMessage", "User information updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
