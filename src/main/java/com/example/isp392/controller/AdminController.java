package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.BookService;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.ShopService;
import com.example.isp392.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import com.example.isp392.service.BlogService;
import com.example.isp392.service.CategoryService;
import com.example.isp392.service.PublisherService;
import jakarta.persistence.EntityNotFoundException;

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
import com.example.isp392.service.DataImportExportService;
/**
 * Controller for handling admin-related requests
 * This controller manages the admin login page and admin panel pages
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    
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
    private final DataImportExportService dataImportExportService;

    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     * 
     * @param userService service for user-related operations
     * @param adminService service for admin-specific operations
     */
    public AdminController(UserService userService,
                          AdminService adminService,
                          BookService bookService,
                          CategoryRepository categoryRepository,
                          PublisherRepository publisherRepository,
                          ShopService shopService,
                          OrderService orderService, BlogService blogService, CategoryService categoryService, PublisherService publisherService, DataImportExportService dataImportExportService) {
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
        this.dataImportExportService = dataImportExportService;
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
     * @param <T> Type of list elements
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
        Page<Book> bookPage = bookService.findBooks(keyword, categoryIds, null, null, null, null, page, size, sortField, sortDir);

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
    @GetMapping("/products/add")
    public String showAddProductForm(Model model) {
        adminService.addAdminInfoToModel(model);
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("publishers", publisherRepository.findAll());
        model.addAttribute("activeMenu", "product");
        model.addAttribute("activeSubMenu", "product-add");
        return "admin/product/add-product";
    }

    @PostMapping("/products/add")
    public String saveProduct(@ModelAttribute("book") Book book, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/product/add-product";
        }

        Shop defaultShop = shopService.getShopById(1);

        book.setShop(defaultShop);
        book.setDateAdded(LocalDate.now());
        bookService.save(book);

        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully!");
        return "redirect:/admin/products";
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

            // Xử lý upload ảnh mới nếu có
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                try {
                    String uploadDir = "src/main/resources/static/images/uploads/";
                    Path uploadPath = Paths.get(uploadDir);

                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String uniqueFilename = System.currentTimeMillis() + "_" + coverImageFile.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFilename);

                    try (InputStream inputStream = coverImageFile.getInputStream()) {
                        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Cập nhật đường dẫn ảnh mới cho sách
                    existingBook.setCoverImgUrl("/images/uploads/" + uniqueFilename);

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
        if(shop == null) return "redirect:/admin/seller-approvals";

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

    // NEW: Admin Data Import/Export Tools
    @GetMapping("/data-management")
    public String showDataManagementPage(Model model) {
        adminService.addAdminInfoToModel(model);
        model.addAttribute("activeMenu", "data-management");
        return "admin/data-management"; // Create this new Thymeleaf template
    }

    @PostMapping("/data-management/import/users")
    public String importUsers(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a CSV file to upload.");
            return "redirect:/admin/data-management";
        }
        try {
            // Assuming your service handles CSV parsing and saving
            dataImportExportService.importUsersFromCsv(file);
            redirectAttributes.addFlashAttribute("successMessage", "Users imported successfully!");
        } catch (IOException e) {
            log.error("Error importing users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error importing users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing users: " + e.getMessage());
        }
        return "redirect:/admin/data-management";
    }

    @PostMapping("/data-management/import/products")
    public String importProducts(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a CSV file to upload.");
            return "redirect:/admin/data-management";
        }
        try {
            dataImportExportService.importBooksFromCsv(file);
            redirectAttributes.addFlashAttribute("successMessage", "Products imported successfully!");
        } catch (IOException e) {
            log.error("Error importing products: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error importing products: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing products: " + e.getMessage());
        }
        return "redirect:/admin/data-management";
    }

    @GetMapping("/data-management/export/users")
    public void exportUsers(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"users_export_" + LocalDate.now() + ".csv\"");
        try {
            dataImportExportService.exportUsersToCsv(response.getWriter());
            redirectAttributes.addFlashAttribute("successMessage", "Users exported successfully!");
        } catch (IOException e) {
            log.error("Error exporting users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting users: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error exporting users: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting users: " + e.getMessage());
        }
    }

    @GetMapping("/data-management/export/products")
    public void exportProducts(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"products_export_" + LocalDate.now() + ".csv\"");
        try {
            dataImportExportService.exportBooksToCsv(response.getWriter());
            redirectAttributes.addFlashAttribute("successMessage", "Products exported successfully!");
        } catch (IOException e) {
            log.error("Error exporting products: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting products: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error exporting products: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting products: " + e.getMessage());
        }
    }

    @GetMapping("/data-management/export/orders")
    public void exportOrders(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"orders_export_" + LocalDate.now() + ".csv\"");
        try {
            dataImportExportService.exportOrdersToCsv(response.getWriter());
            redirectAttributes.addFlashAttribute("successMessage", "Orders exported successfully!");
        } catch (IOException e) {
            log.error("Error exporting orders: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting orders: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error exporting orders: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error exporting orders: " + e.getMessage());
        }
    }
}
