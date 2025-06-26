package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.repository.ShopRepository;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.BookService;
import com.example.isp392.service.ShopService;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
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
    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ShopService shopService;
    private final ShopRepository shopRepository;

    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     * 
     * @param userService service for user-related operations
     * @param adminService service for admin-specific operations
     */
    public AdminController(UserService userService, AdminService adminService, BookService bookService,
                           CategoryRepository categoryRepository, PublisherRepository publisherRepository,
                           ShopRepository shopRepository, ShopService shopService) {
        this.userService = userService;
        this.adminService = adminService;
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.shopRepository = shopRepository;
        this.shopService = shopService;
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
        long pendingSellerCount = shopService.countPendingShops();
        if (adminUserOpt.isPresent()) {
            User adminUser = adminUserOpt.get();
            model.addAttribute("newSellerRegistrations", pendingSellerCount);
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
        Shop shop = shopRepository.findById(shopId).orElse(null);
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
}
