package com.example.isp392.controller;


import com.example.isp392.dto.ProductDTO;
import com.example.isp392.model.*;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.BookService;
import com.example.isp392.service.UserService;

import jakarta.validation.Valid;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;
    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    public AdminController(UserService userService, AdminService adminService, BookService bookService, CategoryRepository categoryRepository, PublisherRepository publisherRepository) {
        this.userService = userService;
        this.adminService = adminService;
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        // *** KHỞI TẠO SHOP SERVICE ***
    }

    @GetMapping("/login")
    public String showAdminLoginPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser") && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard"; // Chuyển hướng về dashboard cho nhất quán
        }
        return "admin/admin-login";
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showDashboardPage(Model model) {
        // <<< TỐI ƯU: Gọi service để thêm thông tin admin, tránh lặp code
        adminService.addAdminInfoToModel(model);
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String showUserManagementPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int size, // Hiển thị 4 user mỗi trang
            Model model) {

        // <<< TỐI ƯU: Gọi service để thêm thông tin admin, tránh lặp code
        adminService.addAdminInfoToModel(model);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("userId").ascending());
        Page<User> userPage = userService.searchUsers(keyword, role, pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("activeMenu", "user");
        model.addAttribute("activeSubMenu", "user-list");

        return "admin/user/user-list";
    }

    @GetMapping("/users/details/{id}")
    public String showUserDetailsPage(@PathVariable("id") Integer userId, Model model) {
        // <<< TỐI ƯU: Gọi service để thêm thông tin admin, tránh lặp code
        adminService.addAdminInfoToModel(model);

        try {
            User user = userService.findUserById(userId);
            model.addAttribute("user", user);
            model.addAttribute("activeMenu", "user");
            // Đặt sub-menu active để người dùng biết họ đang ở trang chi tiết
            model.addAttribute("activeSubMenu", "user-details");
            return "admin/user/user-details";
        } catch (RuntimeException e) {
            return "redirect:/admin/users?error=UserNotFound";
        }
    }

    // Ví dụ cho trang quản lý sản phẩm


}