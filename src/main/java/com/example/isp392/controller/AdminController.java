package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.model.BookReview;
import com.example.isp392.model.BlogComment;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.isp392.service.FileStorageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import com.example.isp392.model.Blog;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Controller for handling admin-related requests
 * This controller manages the admin login page and admin panel pages
 */
@Controller
@RequestMapping("/admin")
public class
AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final UserService userService;
    private final AdminService adminService;
    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ShopService shopService;
    private final OrderService orderService;
    private final BlogService blogService;
    private final CategoryService categoryService;
    private final PublisherService publisherService;
    private final ModerationService moderationService;
    private final SystemSettingService systemSettingService;
    private final FileStorageService fileStorageService;

    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     *
     * @param userService  service for user-related operations
     * @param adminService service for admin-specific operations
     */
    public AdminController(UserService userService,
                           AdminService adminService,
                           BookService bookService,
                           CategoryRepository categoryRepository,
                           PublisherRepository publisherRepository,
                           ShopService shopService,
                           OrderService orderService, BlogService blogService, CategoryService categoryService, PublisherService publisherService
            , ModerationService moderationService, SystemSettingService systemSettingService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.adminService = adminService;
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.shopService = shopService;
        this.orderService = orderService;
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.publisherService = publisherService;
        this.moderationService = moderationService;
        this.systemSettingService = systemSettingService;
        this.fileStorageService = fileStorageService;

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
            return "redirect:/admin/dashboard";
        }

        // No active menu for login page
        model.addAttribute("activeMenu", "");

        return "admin/admin-login";
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
        try {
            // Use AdminService to get the current admin user
            Optional<User> adminUserOpt = adminService.getCurrentAdminUser();
            if (adminUserOpt.isPresent()) {
                User adminUser = adminUserOpt.get();
                model.addAttribute("user", adminUser);
                model.addAttribute("roles", userService.getUserRoles(adminUser));
                String firstName = adminService.extractFirstName(adminUser.getFullName());
                model.addAttribute("firstName", firstName);
            }

            // === Dashboard statistics ===
            // 1. Total users count
            long totalUsers = userService.countAllUsers();
            model.addAttribute("totalUsers", totalUsers);

            // 2. New users (registered in the last 30 days)
            long newUsers = adminService.countNewUsers(30);
            model.addAttribute("newUsers", newUsers);

            // 3. Buyers count
            long buyerCount = adminService.countUsersByRole("BUYER");
            model.addAttribute("buyerCount", buyerCount);

            // 4. Sellers count
            long sellerCount = adminService.countUsersByRole("SELLER");
            model.addAttribute("sellerCount", sellerCount);

            // 5. Active sellers (count of shops with APPROVED status and active)
            long activeSellers = shopService.countActiveSellers();
            model.addAttribute("activeSellers", activeSellers);

            // 6. Total products
            long totalProducts = bookService.countAllBooks();
            model.addAttribute("totalProducts", totalProducts);

            // 7. New products listed (in last 30 days)
            long newProducts = adminService.countNewProducts(30);
            model.addAttribute("newProducts", newProducts);

            // 8. Pending seller approvals count
            long pendingSellerCount = shopService.countPendingShops();
            model.addAttribute("newSellerRegistrations", pendingSellerCount);

            // 9. Platform revenue (commission from orders) - Assuming 10% commission
            BigDecimal platformRevenue = adminService.calculateTotalPlatformRevenue();
            model.addAttribute("totalRevenue", platformRevenue);

            // NEW: Total orders across the platform
            long totalPlatformOrders = adminService.countAllOrders();
            model.addAttribute("totalPlatformOrders", totalPlatformOrders);

            // NEW: Average order value across the platform
            BigDecimal averageOrderValue = adminService.calculateAverageOrderValue();
            model.addAttribute("averageOrderValue", averageOrderValue);

            // NEW: Total platform views
            int totalPlatformViews = adminService.getTotalPlatformViews();
            model.addAttribute("totalPlatformViews", totalPlatformViews);

            // NEW: Top viewed products
            List<Map<String, Object>> topViewedProducts = adminService.getTopViewedProducts(5);
            model.addAttribute("topViewedProducts", topViewedProducts);
            // For chart.js: push product titles and views as JSON arrays
            List<String> productTitles = new ArrayList<>();
            List<Integer> productViewsCounts = new ArrayList<>();
            for (Map<String, Object> pv : topViewedProducts) {
                productTitles.add((String) pv.get("title"));
                productViewsCounts.add(pv.get("viewsCount") != null ? ((Number) pv.get("viewsCount")).intValue() : 0);
            }
            model.addAttribute("productViewsLabelsJson", safeConvertToJsonArray(productTitles));
            model.addAttribute("productViewsDataJson", safeConvertToJsonArray(productViewsCounts));

            // 10. Daily revenue data for the last 7 days
            List<Map<String, Object>> dailyRevenueData = adminService.getDailyPlatformRevenue();
            List<String> dayLabels = new ArrayList<>();
            List<BigDecimal> dailyRevenueValues = new ArrayList<>();
            BigDecimal totalRevenueLast7Days = BigDecimal.ZERO;

            for (Map<String, Object> data : dailyRevenueData) {
                dayLabels.add((String) data.get("day"));
                dailyRevenueValues.add((BigDecimal) data.get("revenue"));
                totalRevenueLast7Days = totalRevenueLast7Days.add((BigDecimal) data.get("revenue"));
            }

            model.addAttribute("dailyLabelsJson", safeConvertToJsonArray(dayLabels));
            model.addAttribute("dailyRevenueJson", safeConvertToJsonArray(dailyRevenueValues));
            model.addAttribute("totalRevenueLast7Days", totalRevenueLast7Days);

            // 11. User distribution data for chart
            List<Integer> userDistribution = new ArrayList<>();
            userDistribution.add(Long.valueOf(buyerCount).intValue());
            userDistribution.add(Long.valueOf(sellerCount).intValue());
            userDistribution.add(adminService.countUsersByRole("ADMIN").intValue());
            model.addAttribute("userDistributionJson", safeConvertToJsonArray(userDistribution));

            // 12. Top products by sales
            List<Map<String, Object>> topProducts = adminService.getTopSellingProducts(5);
            model.addAttribute("topProducts", topProducts);

            // 13. Top sellers by revenue
            List<Map<String, Object>> topSellers = adminService.getTopSellers(5);
            model.addAttribute("topSellers", topSellers);

            // 14. Recent orders
            List<Map<String, Object>> recentOrders = adminService.getRecentOrders(5);
            model.addAttribute("recentOrders", recentOrders);

            // 15. System alerts (mocked for now)
            int systemAlerts = 0;
            model.addAttribute("systemAlerts", systemAlerts);

            // 16. Recent activities
            List<Map<String, Object>> recentActivities = adminService.getRecentActivities(10);
            model.addAttribute("recentActivities", recentActivities);

            log.debug("Dashboard loaded with stats: users={}, products={}, activeSellers={}, pendingApprovals={}",
                    totalUsers, totalProducts, activeSellers, pendingSellerCount);

            // Add active menu information for sidebar highlighting
            model.addAttribute("activeMenu", "dashboard");

            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading admin dashboard: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading dashboard data: " + e.getMessage());

            // Add empty data to prevent JavaScript errors
            model.addAttribute("totalUsers", 0);
            model.addAttribute("newUsers", 0);
            model.addAttribute("buyerCount", 0);
            model.addAttribute("sellerCount", 0);
            model.addAttribute("activeSellers", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("newProducts", 0);
            model.addAttribute("newSellerRegistrations", 0);
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("totalRevenueLast7Days", BigDecimal.ZERO);
            model.addAttribute("dailyLabelsJson", "[]");
            model.addAttribute("dailyRevenueJson", "[]");
            model.addAttribute("topProducts", new ArrayList<>());
            model.addAttribute("topSellers", new ArrayList<>());
            model.addAttribute("recentOrders", new ArrayList<>());
            model.addAttribute("systemAlerts", 0);
            model.addAttribute("recentActivities", new ArrayList<>());
            model.addAttribute("activeMenu", "dashboard");

            return "admin/dashboard";
        }
    }

    /**
     * Convert a list to a JSON array string safely using the standard Jackson library
     *
     * @param <T>  Type of list elements
     * @param list List to convert
     * @return JSON array string
     */
    private <T> String safeConvertToJsonArray(List<T> list) {
        if (list == null) {
            return "[]";
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error converting list to JSON array: {}", e.getMessage());
            return "[]"; // Fallback on error
        }
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
        Page<User> userPage = userService.searchUsers(keyword, role, status, pageable);


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
            boolean newIsActiveStatus = (isActive != null);

            userService.updateUserActivationStatus(userId, newIsActiveStatus);
            userService.updateUserRoles(userId, roles);

            redirectAttributes.addFlashAttribute("successMessage", "User information updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    //  VÙNG QUẢN LÝ SẢN PHẨM (PRODUCTS)

    @GetMapping("/products")
    public String showProductList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "title") String sortField,
            @RequestParam(name = "sortDir", defaultValue = "ASC") String sortDir,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Integer categoryId, // <<< THÊM THAM SỐ NÀY
            Model model) {

        adminService.addAdminInfoToModel(model);

        // Chuyển categoryId thành List<Integer> để tương thích với service
        List<Integer> categoryIds = null;
        if (categoryId != null) {
            categoryIds = List.of(categoryId);
        }

        // Gọi phương thức findBooks hợp nhất, truyền categoryIds vào
        Page<Book> bookPage = bookService.findBooks(keyword, categoryIds, null, null, null, null, page, size, sortField, sortDir, false); // <<< THÊM 'false'

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("activeMenu", "product");
        model.addAttribute("activeSubMenu", "product-list");
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("reverseSortDir", sortDir.equals("ASC") ? "DESC" : "ASC");

        // <<< THÊM CÁC THUỘC TÍNH MỚI CHO VIEW >>>
        model.addAttribute("allCategories", categoryRepository.findAll(Sort.by("categoryName"))); // Lấy tất cả category để đổ ra dropdown
        model.addAttribute("selectedCategoryId", categoryId); // Giữ lại category đã chọn để hiển thị

        return "admin/product/product-management";
    }



    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Integer bookId, Model model) {
        adminService.addAdminInfoToModel(model);
        Optional<Book> bookOptional = bookService.getBookById(bookId);
        if (bookOptional.isPresent()) {
            model.addAttribute("book", bookOptional.get());
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("publishers", publisherRepository.findAll());
            model.addAttribute("activeMenu", "product");
            return "admin/product/edit-product";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable("id") Integer bookId,
                                @ModelAttribute("book") Book bookFromForm,
                                @RequestParam("coverImageFile") MultipartFile coverImageFile, // Nhận tệp tải lên
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "admin/product/edit-product";
        }

        Optional<Book> existingBookOpt = bookService.getBookById(bookId);

        if (existingBookOpt.isPresent()) {
            Book existingBook = existingBookOpt.get();

            existingBook.setTitle(bookFromForm.getTitle());
            existingBook.setStockQuantity(bookFromForm.getStockQuantity());
            existingBook.setAuthors(bookFromForm.getAuthors());
            existingBook.setDescription(bookFromForm.getDescription());
            existingBook.setSellingPrice(bookFromForm.getSellingPrice());
            existingBook.setOriginalPrice(bookFromForm.getOriginalPrice());
            existingBook.setPublisher(bookFromForm.getPublisher());
            existingBook.setCategories(bookFromForm.getCategories());
            existingBook.setActive(bookFromForm.getActive());
            existingBook.setSellingPrice(bookFromForm.getSellingPrice());
            existingBook.setOriginalPrice(bookFromForm.getOriginalPrice());

            // Xử lý upload ảnh mới nếu có
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                try {
                    // Use FileStorageService for consistent upload handling
                    String imageUrl = fileStorageService.storeFile(coverImageFile, "book-covers");

                    // Cập nhật đường dẫn ảnh mới cho sách
                    existingBook.setCoverImgUrl(imageUrl);

                } catch (IOException e) {
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("errorMessage", "Error uploading image: " + e.getMessage());
                    return "redirect:/admin/products/edit/" + bookId;
                }
            }
            // Nếu không có tệp mới, đường dẫn ảnh cũ sẽ được giữ nguyên

            bookService.save(existingBook);
            redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Book not found!");
        }

        return "redirect:/admin/products";
    }

    @GetMapping("/products/detail/{id}")
    public String showProductDetail(@PathVariable("id") Integer bookId, Model model) {
        adminService.addAdminInfoToModel(model);
        Optional<Book> bookOptional = bookService.getBookById(bookId);

        if (bookOptional.isPresent()) {
            model.addAttribute("book", bookOptional.get());
            model.addAttribute("activeMenu", "product");

            // ĐẢM BẢO DÒNG NÀY TRẢ VỀ CHÍNH XÁC NHƯ SAU
            return "admin/product/product-detail";
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Get current authenticated user to check ownership if needed (optional but good practice)
            // For now, we will just perform the action
            bookService.deleteBook(id); // This now deactivates the book
            redirectAttributes.addFlashAttribute("successMessage", "Product has been hidden successfully!");
        } catch (Exception e) {
            log.error("Error hiding product: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error hiding product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // đăng kí seller
    @GetMapping("/seller-approvals")
    public String showSellerApprovalQueue(Model model) {
        adminService.addAdminInfoToModel(model);
        List<Shop> pendingShops = shopService.getPendingShops();
        model.addAttribute("pendingShops", pendingShops);
        model.addAttribute("activeMenu", "seller");
        model.addAttribute("activeSubMenu", "seller-approval");
        List<Shop> rejectedShops = shopService.getRejectedShops();
        model.addAttribute("rejectedShops", rejectedShops);
        return "admin/seller-approval";
    }

    @GetMapping("/shops/detail/{id}")
    public String showShopDetailPage(@PathVariable("id") Integer shopId, Model model) {
        adminService.addAdminInfoToModel(model);
        Shop shop = shopService.getShopById(shopId); // Assuming shopService has a getShopById method
        if (shop == null) return "redirect:/admin/seller-approvals";

        model.addAttribute("shop", shop);
        model.addAttribute("activeMenu", "seller");
        return "admin/shop-detail";
    }


    // === CÁC PHƯƠNG THỨC CẦN SỬA ĐỔI ===

    @PostMapping("/seller-approvals/approve/{id}")
    public String approveSeller(@PathVariable("id") Integer shopId,
                                @RequestParam("returnUrl") String returnUrl, // <<< THÊM THAM SỐ NÀY
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        try {
            // Lấy thông tin admin đang đăng nhập
            User currentAdmin = adminService.getCurrentAdminUser()
                    .orElseThrow(() -> new IllegalStateException("Admin user not found in session. Please login again."));

            // Gọi service với đủ tham số
            shopService.approveShop(shopId, currentAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "Shop has been approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    @PostMapping("/seller-approvals/reject/{id}")
    public String rejectSeller(@PathVariable("id") Integer shopId,
                               @RequestParam("reason") String reason,
                               @RequestParam("returnUrl") String returnUrl, // <<< THÊM THAM SỐ NÀY
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Reason for rejection cannot be empty.");
                return "redirect:/admin/seller-approvals";
            }

            // Lấy thông tin admin đang đăng nhập
            User currentAdmin = adminService.getCurrentAdminUser()
                    .orElseThrow(() -> new IllegalStateException("Admin user not found in session. Please login again."));

            // Gọi service với đủ tham số
            shopService.rejectShop(shopId, reason, currentAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "The shop application has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:" + returnUrl;
    }

    // ==================== BLOG MANAGEMENT (MỚI) =====================
    // ==================================================================

    @GetMapping("/blogs")
    public String showBlogListPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sortField", defaultValue = "createdDate") String sortField,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        adminService.addAdminInfoToModel(model);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Blog> blogPage = blogService.findBlogsForAdmin(keyword, status, pageable);

        model.addAttribute("blogPage", blogPage);
        model.addAttribute("activeMenu", "blog");
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", "asc".equals(sortDir) ? "desc" : "asc");
        return "admin/blog/blog-list";
    }

    @GetMapping("/blogs/detail/{id}")
    public String showBlogDetailPage(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        adminService.addAdminInfoToModel(model);
        Optional<Blog> blogOpt = blogService.getBlogById(id);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Blog post not found.");
            return "redirect:/admin/blogs";
        }
        model.addAttribute("blog", blogOpt.get());
        model.addAttribute("activeMenu", "blog");
        return "admin/blog/blog-detail";
    }

    @GetMapping("/blogs/edit/{id}")
    public String showEditBlogForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        adminService.addAdminInfoToModel(model);
        Optional<Blog> blogOpt = blogService.getBlogById(id);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Blog post not found.");
            return "redirect:/admin/blogs";
        }
        model.addAttribute("blog", blogOpt.get());
        model.addAttribute("activeMenu", "blog");
        return "admin/blog/blog-edit";
    }

    @PostMapping("/blogs/edit/{id}")
    public String updateBlogPost(@PathVariable("id") Integer id, @ModelAttribute("blog") Blog blog, RedirectAttributes redirectAttributes) {
        try {
            blogService.updateBlog(id, blog);
            redirectAttributes.addFlashAttribute("successMessage", "Blog post updated successfully!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/blogs/delete/{id}")
    public String deleteBlogPost(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            blogService.deleteBlogById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Blog post deleted successfully.");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/blogs/toggle-pin/{id}")
    public String togglePinBlogPost(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            String message = blogService.togglePinStatus(id) ? "Post has been pinned." : "Post has been unpinned.";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/blogs/toggle-lock/{id}")
    public String toggleLockBlogPost(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            String message = blogService.toggleLockStatus(id) ? "Post has been locked." : "Post has been unlocked.";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }


    // ==================== REVIEW & COMMENT MODERATION =====================

    @GetMapping("/product-reviews")
    public String showProductReviewsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "rating", required = false) Integer rating,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "sortField", defaultValue = "createdDate") String sortField,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        adminService.addAdminInfoToModel(model);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortField));
        Page<BookReview> reviews = moderationService.findPaginatedReviews(search, rating, startDate, endDate, pageable);

        model.addAttribute("reviews", reviews);
        model.addAttribute("activeMenu", "product-reviews"); // Đặt menu active
        // Giữ lại trạng thái lọc, tìm kiếm, sắp xếp
        model.addAttribute("search", search);
        model.addAttribute("rating", rating);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", "asc".equals(sortDir) ? "desc" : "asc");

        return "admin/product-reviews";
    }

    @GetMapping("/blog-comments")
    public String showBlogCommentsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "sortField", defaultValue = "createdDate") String sortField,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        adminService.addAdminInfoToModel(model);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortField));
        Page<BlogComment> comments = moderationService.findPaginatedComments(search, startDate, endDate, pageable);

        model.addAttribute("comments", comments);
        model.addAttribute("activeMenu", "blog-comments"); // Đặt menu active
        // Giữ lại trạng thái lọc, tìm kiếm, sắp xếp
        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", "asc".equals(sortDir) ? "desc" : "asc");

        return "admin/blog-comments";
    }

    @PostMapping("/reviews/approve/{id}")
    public String approveReview(@PathVariable("id") Integer reviewId, RedirectAttributes redirectAttributes) {
        if (moderationService.approveReview(reviewId)) {
            redirectAttributes.addFlashAttribute("successMessage", "Review approved successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
        }
        return "redirect:/admin/product-reviews"; // Chuyển hướng về trang product-reviews
    }

    @PostMapping("/reviews/delete/{id}")
    public String deleteReview(@PathVariable("id") Integer reviewId, RedirectAttributes redirectAttributes) {
        try {
            moderationService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "Review deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting review.");
        }
        return "redirect:/admin/product-reviews"; // Chuyển hướng về trang product-reviews
    }

    @PostMapping("/comments/approve/{id}")
    public String approveComment(@PathVariable("id") Integer commentId, RedirectAttributes redirectAttributes) {
        if (moderationService.approveComment(commentId)) {
            redirectAttributes.addFlashAttribute("successMessage", "Comment approved successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Comment not found.");
        }
        return "redirect:/admin/blog-comments"; // Chuyển hướng về trang blog-comments
    }

    @PostMapping("/comments/delete/{id}")
    public String deleteComment(@PathVariable("id") Integer commentId, RedirectAttributes redirectAttributes) {
        try {
            moderationService.deleteComment(commentId);
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting comment.");
        }
        return "redirect:/admin/blog-comments"; // Chuyển hướng về trang blog-comments
    }

    // Các phương thức xem chi tiết vẫn giữ nguyên, chúng không bị ảnh hưởng
    @GetMapping("/moderation/reviews/{id}")
    public String showReviewDetailPage(@PathVariable("id") Integer reviewId, Model model, RedirectAttributes redirectAttributes) {
        adminService.addAdminInfoToModel(model);
        Optional<BookReview> reviewOpt = moderationService.getReviewById(reviewId);

        if (reviewOpt.isPresent()) {
            model.addAttribute("review", reviewOpt.get());
            model.addAttribute("contentType", "review");
            model.addAttribute("activeMenu", "product-reviews"); // Cập nhật menu active
            return "admin/moderation-detail";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Review not found.");
            return "redirect:/admin/product-reviews";
        }
    }

    @GetMapping("/moderation/comments/{id}")
    public String showCommentDetailPage(@PathVariable("id") Integer commentId, Model model, RedirectAttributes redirectAttributes) {
        adminService.addAdminInfoToModel(model);
        Optional<BlogComment> commentOpt = moderationService.getCommentById(commentId);

        if (commentOpt.isPresent()) {
            model.addAttribute("comment", commentOpt.get());
            model.addAttribute("contentType", "comment");
            model.addAttribute("activeMenu", "blog-comments"); // Cập nhật menu active
            return "admin/moderation-detail";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Comment not found.");
            return "redirect:/admin/blog-comments";
        }
    }

    /**
     * Display admin revenue reports page with filtering and analytics
     *
     * @param model Model to add attributes
     * @param period Period for analytics (daily, weekly, monthly, yearly)
     * @param startDate Start date for custom period
     * @param endDate End date for custom period
     * @param sellerId Specific seller ID to filter by
     * @param compareMode Compare mode (previous, year)
     * @return reports page view
     */
    @GetMapping("/reports")
    public String showReportsPage(
            Model model,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(defaultValue = "previous") String compareMode) {

        try {
            // Add admin info to model
            adminService.addAdminInfoToModel(model);

            // Set default date range if not provided
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                switch (period) {
                    case "daily":
                        startDate = endDate.minusDays(30); // Last 30 days
                        break;
                    case "weekly":
                        startDate = endDate.minusWeeks(12); // Last 12 weeks
                        break;
                    case "yearly":
                        startDate = endDate.minusYears(3); // Last 3 years
                        break;
                    default: // monthly
                        startDate = endDate.minusMonths(12); // Last 12 months
                        break;
                }
            }

            // Get revenue analytics data
            Map<String, Object> revenueData = adminService.getRevenueAnalytics(startDate, endDate, period, sellerId, compareMode);

            // Add all data to model
            model.addAllAttributes(revenueData);
            model.addAttribute("period", period);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("sellerId", sellerId);
            model.addAttribute("compareMode", compareMode);
            model.addAttribute("activeMenu", "reports");

            log.debug("Reports page loaded for period: {}, startDate: {}, endDate: {}", period, startDate, endDate);

        } catch (Exception e) {
            log.error("Error loading reports page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading reports data: " + e.getMessage());

            // Add empty data to prevent template errors
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("revenueGrowth", 0.0);
            model.addAttribute("topSellers", new ArrayList<>());
            model.addAttribute("revenueChartLabels", "[]");
            model.addAttribute("revenueChartData", "[]");
            model.addAttribute("activeMenu", "reports");
        }

        return "admin/reports";
    }

    /**
     * Display consolidated reports page with comprehensive analytics
     *
     * @param model Model to add attributes
     * @param period Period for analytics (daily, weekly, monthly, yearly)
     * @param startDate Start date for custom period
     * @param endDate End date for custom period
     * @return consolidated reports page view
     */
    @GetMapping("/consolidated-reports")
    public String showConsolidatedReportsPage(
            Model model,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Add admin info to model
            adminService.addAdminInfoToModel(model);

            // Set default date range if not provided
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                switch (period) {
                    case "daily":
                        startDate = endDate.minusDays(30); // Last 30 days
                        break;
                    case "weekly":
                        startDate = endDate.minusWeeks(12); // Last 12 weeks
                        break;
                    case "yearly":
                        startDate = endDate.minusYears(3); // Last 3 years
                        break;
                    default: // monthly
                        startDate = endDate.minusMonths(12); // Last 12 months
                        break;
                }
            }

            // Get consolidated reports data
            Map<String, Object> consolidatedData = adminService.getConsolidatedReports(startDate, endDate, period);

            // Add all data to model
            model.addAllAttributes(consolidatedData);
            model.addAttribute("period", period);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("activeMenu", "consolidated-reports");

            log.debug("Consolidated reports page loaded for period: {}, startDate: {}, endDate: {}", period, startDate, endDate);

        } catch (Exception e) {
            log.error("Error loading consolidated reports page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading consolidated reports data: " + e.getMessage());

            // Add empty data to prevent template errors
            model.addAttribute("orders", new HashMap<>());
            model.addAttribute("users", new HashMap<>());
            model.addAttribute("products", new HashMap<>());
            model.addAttribute("forum", new HashMap<>());
            model.addAttribute("revenue", new HashMap<>());
            model.addAttribute("platform", new HashMap<>());
            model.addAttribute("activeMenu", "consolidated-reports");
        }

        return "admin/consolidated-reports";
    }

    @GetMapping("/settings")
    public String showSettingsPage(Model model) {
        adminService.addAdminInfoToModel(model);

        // Đảm bảo danh sách này chứa đủ 12 khóa
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

        Map<String, String> settings = systemSettingService.getSettings(settingKeys);

        model.addAttribute("settings", settings);
        model.addAttribute("activeMenu", "settings");
        return "admin/system-settings";
    }

    /**
     * Lưu các thay đổi từ trang cài đặt hệ thống
     */
    @PostMapping("/settings/save")
    public String saveSettings(
            @RequestParam Map<String, String> allParams,
            @RequestParam("heroImageFile") MultipartFile heroImageFile,
            RedirectAttributes redirectAttributes) {

        try {
            // --- XỬ LÝ UPLOAD ẢNH BẰNG FILESTORAGESERVICE ---
            if (heroImageFile != null && !heroImageFile.isEmpty()) {
                // Gọi service để lưu file vào thư mục con "settings"
                String fileUrl = fileStorageService.storeFile(heroImageFile, "settings");

                // Lưu đường dẫn web-accessible vào map để cập nhật vào DB
                allParams.put("hero_background_image", fileUrl);
            }
            // --- KẾT THÚC XỬ LÝ UPLOAD ẢNH ---

            systemSettingService.saveSettings(allParams);
            redirectAttributes.addFlashAttribute("successMessage", "Settings saved successfully!");

        } catch (IOException e) {
            log.error("Error saving settings or uploading file: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving settings: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving settings: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving settings.");
        }

        return "redirect:/admin/settings";
    }

}
