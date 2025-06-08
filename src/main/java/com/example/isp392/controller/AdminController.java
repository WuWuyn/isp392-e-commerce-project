package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.User;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.service.AdminService;
import com.example.isp392.service.BookService;
import com.example.isp392.service.UserService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
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
    }

    @GetMapping("/login")
    public String showAdminLoginPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser") && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        return "admin/admin-login";
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showDashboardPage(Model model) {
        adminService.addAdminInfoToModel(model);
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }

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
        book.setDateAdded(new Date());
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

}