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
    private final ShopRepository shopRepository;
    private final ShopService shopService;

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

    @GetMapping("/users")
    public String showUserManagementPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int size,
            Model model) {

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
    // === VÙNG QUẢN LÝ SẢN PHẨM (PRODUCTS) -

    @GetMapping("/products")
    public String showProductList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "title") String sortField,
            @RequestParam(name = "sortDir", defaultValue = "ASC") String sortDir,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        adminService.addAdminInfoToModel(model);

        // Gọi phương thức findBooks hợp nhất, truyền null cho các tham số không dùng ở trang admin
        Page<Book> bookPage = bookService.findBooks(keyword, null, null, null, null, null, page, size, sortField, sortDir);

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("activeMenu", "product");
        model.addAttribute("activeSubMenu", "product-list");
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("keyword", keyword);

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

        Shop defaultShop = shopRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Error: Default Shop with ID 1 not found in the database!"));
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

            // Cập nhật các trường thông tin từ form
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

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Integer bookId, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteById(bookId);
            redirectAttributes.addFlashAttribute("successMessage", "Book with ID " + bookId + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting book. It might be referenced by other records (e.g., orders).");
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

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer userId, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findUserById(userId);
            adminService.addAdminInfoToModel(model);
            model.addAttribute("user", user);
            model.addAttribute("activeMenu", "user");
            return "admin/user/edit-user"; // Trả về trang edit-user.html
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found!");
            return "redirect:/admin/users";
        }
    }

    // PHƯƠNG THỨC 2: XỬ LÝ VIỆC CẬP NHẬT
    @PostMapping("/users/update/{id}")
    public String updateUserByAdmin(@PathVariable("id") Integer userId,
                                    @RequestParam("isActive") boolean isActive,
                                    Authentication authentication, // Thêm tham số này
                                    RedirectAttributes redirectAttributes) {

        // === THÊM LỚP BẢO VỆ ĐỂ NGĂN ADMIN TỰ VÔ HIỆU HÓA ===
        UserDetails loggedInUser = (UserDetails) authentication.getPrincipal();
        String loggedInUsername = loggedInUser.getUsername();

        User userToUpdate = userService.findUserById(userId);

        // Nếu người dùng đang cố gắng tự vô hiệu hóa chính mình
        if (userToUpdate.getEmail().equals(loggedInUsername) && !isActive) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot deactivate your own account.");
            return "redirect:/admin/users";
        }

        try {
            userService.updateUserActivationStatus(userId, isActive);
            redirectAttributes.addFlashAttribute("successMessage", "User status has been updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer userId, Authentication authentication, RedirectAttributes redirectAttributes) {

        // Lấy thông tin admin đang đăng nhập
        UserDetails loggedInUser = (UserDetails) authentication.getPrincipal();
        String loggedInUsername = loggedInUser.getUsername();

        // Lấy thông tin user sắp bị xóa
        User userToDelete = userService.findUserById(userId);

        // KIỂM TRA NGĂN ADMIN TỰ XÓA
        if (userToDelete.getEmail().equals(loggedInUsername)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: You cannot delete your own admin account.");
            return "redirect:/admin/users";
        }

        try {
            userService.deleteUserById(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + userToDelete.getFullName() + "' has been permanently deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
    @GetMapping("/seller-approvals")
    public String showSellerApprovalQueue(Model model) {
        adminService.addAdminInfoToModel(model);
        List<Shop> pendingShops = shopService.getPendingShops();
        model.addAttribute("pendingShops", pendingShops);
        model.addAttribute("activeMenu", "seller");
        model.addAttribute("activeSubMenu", "seller-approval");
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
    public String approveSeller(@PathVariable("id") Integer shopId, RedirectAttributes redirectAttributes, Authentication authentication) {
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
        return "redirect:/admin/seller-approvals";
    }

    @PostMapping("/seller-approvals/reject/{id}")
    public String rejectSeller(@PathVariable("id") Integer shopId, @RequestParam("reason") String reason, RedirectAttributes redirectAttributes, Authentication authentication) {
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
        return "redirect:/admin/seller-approvals";
    }

}
