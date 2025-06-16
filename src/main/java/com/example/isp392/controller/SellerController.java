package com.example.isp392.controller;

import com.example.isp392.dto.BookFormDTO;
import com.example.isp392.dto.ShopFormDTO;
import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private static final Logger log = LoggerFactory.getLogger(SellerController.class);
    private final UserService userService;
    private final BookService bookService;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final PublisherService publisherService;
    private final OrderService orderService;

    /**
     * Constructor for dependency injection
     * @param userService Service for user-related operations
     * @param bookService Service for book-related operations
     * @param shopService Service for shop-related operations
     * @param categoryService Service for category-related operations
     * @param publisherService Service for publisher-related operations
     * @param orderService Service for order-related operations
     */
    public SellerController(UserService userService, BookService bookService, ShopService shopService, 
                           CategoryService categoryService, PublisherService publisherService, OrderService orderService) {
        this.userService = userService;
        this.bookService = bookService;
        this.shopService = shopService;
        this.categoryService = categoryService;
        this.publisherService = publisherService;
        this.orderService = orderService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "seller/seller-login";
    }

    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        return "seller/seller-signup";
    }

//    @PostMapping("/signup")
//    public String registerSeller(
//            @Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
//            BindingResult bindingResult,
//            RedirectAttributes redirectAttributes) {
//        if (bindingResult.hasErrors()) {
//            return "seller/seller-signup";
//        }
//        try {
//            userService.registerNewUser(userRegistrationDTO, "SELLER");
//            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
//            return "redirect:/seller/login";
//        } catch (Exception e) {
//            log.error("Error registering seller: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/seller/signup";
//        }
//    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/dashboard";
    }

    /**
     * Display account info page
     *
     * @param model Model to add attributes
     * @return account info page view
     */
    @GetMapping("/account-info")
    public String showAccountInfo(Model model, Authentication authentication) {
        // Get user with OAuth2 support
        User user = getCurrentUser(authentication);
        if (user == null) {
            log.warn("No user found in showAccountInfo");
            return "redirect:/seller/login";
        }

        // Check if this is an OAuth2 authentication
        boolean isOAuth2User = authentication instanceof OAuth2AuthenticationToken;
        model.addAttribute("isOAuth2User", isOAuth2User);

        // If OAuth2 user, add OAuth2 user details to model
        if (isOAuth2User) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            model.addAttribute("oauth2User", oauth2User);

            // Log OAuth2 attributes for debugging
            log.debug("OAuth2 user attributes: {}", oauth2User.getAttributes());
        }

        // Add user and roles to model
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));

        log.debug("Showing account info for user: id={}, name={}",
                user.getUserId(), user.getFullName());

        return "seller/account-info";
    }

    @GetMapping("/edit-info")
    public String showEditInfoPage(Model model) {
        // Get authenticated user with OAuth2 support
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Add user and roles to model
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));

        // Check authentication type
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = authentication instanceof OAuth2AuthenticationToken;
        model.addAttribute("isOAuth2User", isOAuth2User);

        return "seller/account-edit-info";
    }

    /**
     * Process update user info form submission
     *
     * @param fullName           user's full name
     * @param phone              user's phone number
     * @param gender             user's gender (0: Male, 1: Female, 2: Other)
     * @param redirectAttributes for flash attributes
     * @return redirect to account info page
     */
    @PostMapping("/update-info")
    public String updateUserInfo(
            @ModelAttribute("fullName") String fullName,
            @ModelAttribute("phone") String phone,
            @ModelAttribute("gender") int gender,
            @ModelAttribute("dateOfBirth") String dateOfBirth,
            @RequestParam(value = "profilePictureFile", required = false) MultipartFile profilePictureFile,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            // Get authenticated user with OAuth2 support
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found. Please login again.");
                return "redirect:/seller/login";
            }

            String email = currentUser.getEmail();
            log.debug("Updating info for user: {}", email);

            // Parse date from string
            LocalDate parsedDate = null;
            try {
                if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                    parsedDate = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            } catch (Exception e) {
                log.warn("Error parsing date: {}", e.getMessage());
                // Continue with null date if parsing fails
            }

            // Process profile picture if uploaded
            String profilePicUrl = null;
            if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
                try {
                    // Generate unique filename
                    String originalFilename = profilePictureFile.getOriginalFilename();
                    String fileName = System.currentTimeMillis() + "_" +
                            (originalFilename != null ? originalFilename : "profile.jpg");

                    // Get upload directory path - use the same path configured in FileUploadConfig
                    String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/profile-pictures/";
                    File uploadDirectory = new File(uploadDir);
                    if (!uploadDirectory.exists()) {
                        uploadDirectory.mkdirs();
                    }

                    // Save file to server
                    File destFile = new File(uploadDir + File.separator + fileName);
                    profilePictureFile.transferTo(destFile);

                    // Set profile picture URL that will be mapped by our resource handler
                    profilePicUrl = "/uploads/profile-pictures/" + fileName;
                    log.debug("Profile picture saved: {}", profilePicUrl);
                } catch (Exception e) {
                    // Log error but continue with other user info updates
                    log.error("Error uploading profile picture: {}", e.getMessage());
                }
            }

            // Update user info with profile picture
            userService.updateUserInfo(email, fullName, phone, gender, parsedDate, profilePicUrl);
            log.info("User info updated successfully for: {}", email);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Your information has been updated successfully.");
            return "redirect:/seller/account-info";
        } catch (Exception e) {
            // Handle update errors
            log.error("Error updating user info: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/edit-info";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
            model.addAttribute("isOAuth2User", isOAuth2User);
            String email;
            if (isOAuth2User) {
                OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
                email = oauth2User.getAttribute("email");
                log.debug("OAuth2 seller accessing change password page: {}", email);
            } else {
                email = auth.getName();
                log.debug("Regular seller accessing change password page: {}", email);
            }
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/account-change-password";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying change password form: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(
            @ModelAttribute("currentPassword") String currentPassword,
            @ModelAttribute("newPassword") String newPassword,
            @ModelAttribute("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOAuth2User = auth instanceof OAuth2AuthenticationToken;
        if (isOAuth2User) {
            log.warn("Google OAuth2 seller attempted to change password");
            redirectAttributes.addFlashAttribute("errorMessage", "Google account users cannot change their password here. Please use your Google account settings.");
            return "redirect:/seller/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return "redirect:/seller/change-password";
        }
        try {
            String email = auth.getName();
            boolean updated = userService.updatePassword(email, currentPassword, newPassword);
            if (updated) {
                redirectAttributes.addFlashAttribute("successMessage", "Your password has been updated successfully.");
                return "redirect:/seller/change-password";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
                return "redirect:/seller/change-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/change-password";
        }
    }

    /**
     * Display list of seller's products with pagination and sorting
     *
     * @param model Model to add attributes
     * @param page Page number (0-based)
     * @param size Page size
     * @param searchQuery Search query for product title
     * @param sortField Field to sort by
     * @param sortDir Sort direction (asc or desc)
     * @return seller products view
     */
    @GetMapping("/products")
    public String showProductsPage(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "dateAdded") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                // Redirect to shop creation page if shop doesn't exist
                model.addAttribute("errorMessage", "You need to set up your shop first before managing products.");
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/shop-information";
            }

            // Create sort object based on parameters
            Sort sort = Sort.by(sortDir.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
            
            // Create pageable object for pagination
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Get books based on shop ID
            Page<Book> bookPage;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                // Search by title within seller's books
                bookPage = bookService.searchBooksByShopAndTitle(shop.getShopId(), searchQuery, pageable);
            } else {
                // Get all seller's books
                bookPage = bookService.findByShopId(shop.getShopId(), pageable);
            }
            
            // Add attributes for view
            model.addAttribute("bookPage", bookPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookPage.getTotalPages());
            model.addAttribute("totalItems", bookPage.getTotalElements());
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
            model.addAttribute("searchQuery", searchQuery != null ? searchQuery : "");
            
            // For pagination
            if (bookPage.getTotalPages() > 0) {
                List<Integer> pageNumbers = IntStream.rangeClosed(1, bookPage.getTotalPages())
                        .boxed()
                        .collect(Collectors.toList());
                model.addAttribute("pageNumbers", pageNumbers);
            }
            
            // Add user information for navigation
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            
            log.debug("Displaying product list for shop ID: {}, found {} products", 
                      shop.getShopId(), bookPage.getTotalElements());
            
            return "seller/products";
            
        } catch (Exception e) {
            log.error("Error displaying seller products: {}", e.getMessage());
            return "redirect:/seller/dashboard";
        }
    }

    /**
     * Display form to add a new product
     *
     * @param model Model to add attributes
     * @return add product form view
     */
    @GetMapping("/products/add")
    public String showAddProductForm(Model model) {
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                // Redirect to shop creation page if shop doesn't exist
                model.addAttribute("errorMessage", "You need to set up your shop first before adding products.");
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/shop-information";
            }
            
            // Create empty BookFormDTO
            BookFormDTO bookForm = new BookFormDTO();
            bookForm.setShopId(shop.getShopId());
            bookForm.setPublicationDate(LocalDate.now()); // Default to today
            bookForm.setStockQuantity(0); // Default stock
            
            // Add attributes to the model
            model.addAttribute("bookForm", bookForm);
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("publishers", publisherService.findAll());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            
            log.debug("Displaying add product form for shop ID: {}", shop.getShopId());
            
            return "seller/seller-add-product";
            
        } catch (Exception e) {
            log.error("Error displaying add product form: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            return "redirect:/seller/products";
        }
    }

    /**
     * Process form to add a new product
     *
     * @param bookForm DTO with product information
     * @param bindingResult Validation results
     * @param redirectAttributes For flash attributes
     * @return redirect to products list or back to form with errors
     */
    @PostMapping("/products/add")
    public String processAddProduct(
            @Valid @ModelAttribute("bookForm") BookFormDTO bookForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Debug log to help with form validation issues
            if (bindingResult.hasErrors()) {
                log.debug("Validation errors: {}", bindingResult.getAllErrors());
                bindingResult.getFieldErrors().forEach(error -> 
                    log.debug("Field error: {} - {}", error.getField(), error.getDefaultMessage())
                );
            }
            
            // Validation failed, return to form with errors
            if (bindingResult.hasErrors()) {
                // Add necessary attributes for the form
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                model.addAttribute("categories", categoryService.findAllActive());
                model.addAttribute("publishers", publisherService.findAll());
                
                // Add validation error summary
                model.addAttribute("validationErrors", bindingResult.getAllErrors());
                
                return "seller/seller-add-product";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Shop not found.");
                return "redirect:/seller/dashboard";
            }
            
            // Ensure the shop ID in the form matches the current user's shop
            bookForm.setShopId(shop.getShopId());
            
            // Check if cover image was uploaded
            if (bookForm.getCoverImageFile() == null || bookForm.getCoverImageFile().isEmpty()) {
                model.addAttribute("errorMessage", "Cover image is required.");
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                model.addAttribute("categories", categoryService.findAllActive());
                model.addAttribute("publishers", publisherService.findAll());
                return "seller/seller-add-product";
            }
            
            // Handle cover image upload
            String coverImageUrl;
            try {
                coverImageUrl = handleFileUpload(bookForm.getCoverImageFile(), "book-covers");
                log.debug("Cover image uploaded successfully: {}", coverImageUrl);
            } catch (IOException e) {
                log.error("Error uploading cover image: {}", e.getMessage());
                model.addAttribute("errorMessage", "Error uploading cover image: " + e.getMessage());
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                model.addAttribute("categories", categoryService.findAllActive());
                model.addAttribute("publishers", publisherService.findAll());
                return "seller/seller-add-product";
            }
            
            // Create the book
            Book createdBook = bookService.createBook(bookForm, coverImageUrl);
            
            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
            return "redirect:/seller/products";
            
        } catch (Exception e) {
            log.error("Error adding product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding product: " + e.getMessage());
            return "redirect:/seller/products/add";
        }
    }

    /**
     * Display form to edit a product
     *
     * @param id Book ID to edit
     * @param model Model to add attributes
     * @return edit product form view or error redirect
     */
    @GetMapping("/products/{id}/edit")
    public String showEditProductForm(@PathVariable("id") Integer id, Model model) {
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                model.addAttribute("errorMessage", "Shop not found.");
                return "redirect:/seller/dashboard";
            }
            
            // Get the book by ID
            Optional<Book> bookOpt = bookService.getBookById(id);
            if (bookOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Product not found.");
                return "redirect:/seller/products";
            }
            
            Book book = bookOpt.get();
            
            // Check if the book belongs to the seller's shop
            if (!book.getShop().getShopId().equals(shop.getShopId())) {
                model.addAttribute("errorMessage", "You don't have permission to edit this product.");
                return "redirect:/seller/products";
            }
            
            // Create BookFormDTO from the book entity
            BookFormDTO bookForm = new BookFormDTO();
            bookForm.setShopId(shop.getShopId());
            bookForm.setTitle(book.getTitle());
            bookForm.setAuthors(book.getAuthors());
            bookForm.setDescription(book.getDescription());
            bookForm.setIsbn(book.getIsbn());
            bookForm.setDimensions(book.getDimensions());
            bookForm.setNumberOfPages(book.getNumberOfPages());
            bookForm.setOriginalPrice(book.getOriginalPrice());
            bookForm.setSellingPrice(book.getSellingPrice());
            bookForm.setPublicationDate(book.getPublicationDate());
            bookForm.setSku(book.getSku());
            bookForm.setStockQuantity(book.getStockQuantity());
            
            // Set publisher ID if available
            if (book.getPublisher() != null) {
                bookForm.setPublisherId(book.getPublisher().getPublisherId());
            }
            
            // Set selected categories
            List<Integer> selectedCategoryIds = new ArrayList<>();
            for (Category category : book.getCategories()) {
                selectedCategoryIds.add(category.getCategoryId());
            }
            bookForm.setCategoryIds(selectedCategoryIds);
            
            // Add attributes to the model
            model.addAttribute("bookForm", bookForm);
            model.addAttribute("book", book);
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("publishers", publisherService.findAll());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            
            return "seller/seller-edit-product";
            
        } catch (Exception e) {
            log.error("Error displaying edit product form: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading product: " + e.getMessage());
            return "redirect:/seller/products";
        }
    }

    /**
     * Process form to update a product
     *
     * @param id Book ID to update
     * @param bookForm DTO with updated product information
     * @param bindingResult Validation results
     * @param redirectAttributes For flash attributes
     * @return redirect to products list or back to form with errors
     */
    @PostMapping("/products/{id}/edit")
    public String processEditProduct(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("bookForm") BookFormDTO bookForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Validation failed, return to form with errors
            if (bindingResult.hasErrors()) {
                // Add necessary attributes for the form
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                model.addAttribute("categories", categoryService.findAllActive());
                model.addAttribute("publishers", publisherService.findAll());
                Optional<Book> bookOpt = bookService.getBookById(id);
                if (bookOpt.isPresent()) {
                    model.addAttribute("book", bookOpt.get());
                }
                
                return "seller/seller-edit-product";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Shop not found.");
                return "redirect:/seller/dashboard";
            }
            
            // Get the book by ID
            Optional<Book> bookOpt = bookService.getBookById(id);
            if (bookOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                return "redirect:/seller/products";
            }
            
            Book book = bookOpt.get();
            
            // Check if the book belongs to the seller's shop
            if (!book.getShop().getShopId().equals(shop.getShopId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to edit this product.");
                return "redirect:/seller/products";
            }
            
            // Handle cover image upload if a new one is provided
            String coverImageUrl = book.getCoverImgUrl(); // Keep existing image by default
            if (bookForm.getCoverImageFile() != null && !bookForm.getCoverImageFile().isEmpty()) {
                try {
                    coverImageUrl = handleFileUpload(bookForm.getCoverImageFile(), "book-covers");
                } catch (IOException e) {
                    log.error("Error uploading cover image: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("errorMessage", "Error uploading cover image: " + e.getMessage());
                    return "redirect:/seller/products/" + id + "/edit";
                }
            }
            
            // Update and save the book
            Book updatedBook = bookService.updateBook(id, bookForm, coverImageUrl);
            
            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
            return "redirect:/seller/products";
            
        } catch (Exception e) {
            log.error("Error updating product: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
            return "redirect:/seller/products/" + id + "/edit";
        }
    }

    /**
     * Display product details
     *
     * @param id Book ID to view
     * @param model Model to add attributes
     * @return product details view or error redirect
     */
    @GetMapping("/products/{id}")
    public String viewProductDetails(@PathVariable("id") Integer id, Model model) {
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                model.addAttribute("errorMessage", "Shop not found.");
                return "redirect:/seller/dashboard";
            }
            
            // Get the book by ID
            Optional<Book> bookOpt = bookService.getBookById(id);
            if (bookOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Product not found.");
                return "redirect:/seller/products";
            }
            
            Book book = bookOpt.get();
            
            // Check if the book belongs to the seller's shop
            if (!book.getShop().getShopId().equals(shop.getShopId())) {
                model.addAttribute("errorMessage", "You don't have permission to view this product.");
                return "redirect:/seller/products";
            }
            
            // Add attributes to the model
            model.addAttribute("book", book);
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            
            return "seller/seller-product-details";
            
        } catch (Exception e) {
            log.error("Error displaying product details: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading product: " + e.getMessage());
            return "redirect:/seller/products";
        }
    }

    /**
     * Delete a product
     *
     * @param id Book ID to delete
     * @param redirectAttributes For flash attributes
     * @return redirect to products list
     */
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Get current authenticated user
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }
            
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Shop not found.");
                return "redirect:/seller/dashboard";
            }
            
            // Get the book by ID
            Optional<Book> bookOpt = bookService.getBookById(id);
            if (bookOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                return "redirect:/seller/products";
            }
            
            Book book = bookOpt.get();
            
            // Check if the book belongs to the seller's shop
            if (!book.getShop().getShopId().equals(shop.getShopId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to delete this product.");
                return "redirect:/seller/products";
            }
            
            // Delete the book
            bookService.deleteBook(id);
            
            // Add success message
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
            return "redirect:/seller/products";
            
        } catch (Exception e) {
            log.error("Error deleting product: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
            return "redirect:/seller/products";
        }
    }

    /**
     * Handle file upload for product images
     *
     * @param file File to upload
     * @param subDirectory Subdirectory to save the file in
     * @return URL of the uploaded file
     * @throws IOException If file upload fails
     */
    private String handleFileUpload(MultipartFile file, String subDirectory) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        String fileName = System.currentTimeMillis() + "_" +
                (originalFilename != null ? originalFilename : "file.jpg");
        
        // Get upload directory path
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/" + subDirectory + "/";
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        // Check if image resizing is needed
        byte[] fileBytes = file.getBytes();
        if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
            // Determine image format
            String format = extension;
            if (format.isEmpty() || !isValidImageFormat(format)) {
                format = "jpg"; // Default to jpg if unknown format
            }
            
            try {
                // Try to resize the image
                log.debug("Attempting to resize image: {} ({} bytes)", fileName, fileBytes.length);
                fileBytes = com.example.isp392.config.FileUploadConfig.resizeImage(fileBytes, format);
                log.debug("Image resized: {} ({} bytes)", fileName, fileBytes.length);
            } catch (Exception e) {
                // If resize fails, use original
                log.error("Error resizing image: {}", e.getMessage());
            }
        }
        
        // Save file to server
        java.nio.file.Path destPath = java.nio.file.Path.of(uploadDir + File.separator + fileName);
        java.nio.file.Files.write(destPath, fileBytes);
        log.debug("File saved to: {}", destPath);
        
        // Return URL that will be mapped by resource handler
        return "/uploads/" + subDirectory + "/" + fileName;
    }
    
    /**
     * Check if the format is a valid image format
     * 
     * @param format File extension or format string
     * @return true if valid image format
     */
    private boolean isValidImageFormat(String format) {
        String[] validFormats = {"jpg", "jpeg", "png", "gif", "webp"};
        for (String validFormat : validFormats) {
            if (validFormat.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    @GetMapping("/orders")
    public String showOrdersPage(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Get seller's shop to ensure they are a seller
        Shop shop = shopService.getShopByUserId(user.getUserId());
        if (shop == null) {
            model.addAttribute("errorMessage", "You need to set up your shop first to view orders.");
            return "redirect:/seller/shop-information";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> ordersPage = orderService.findSellerOrders(user.getUserId(), status, dateFrom, dateTo, pageable);

        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramDateFrom", dateFrom);
        model.addAttribute("paramDateTo", dateTo);

        return "seller/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String viewSellerOrderDetail(@PathVariable Integer orderId, Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }

        // Get seller's shop to ensure they are a seller
        Shop shop = shopService.getShopByUserId(user.getUserId());
        if (shop == null) {
            model.addAttribute("errorMessage", "You need to set up your shop first to view order details.");
            return "redirect:/seller/shop-information";
        }

        // Find order and check if any item in the order belongs to this seller's shop
        Optional<Order> orderOpt = orderService.findOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return "redirect:/seller/orders";
        }

        Order order = orderOpt.get();
        boolean isSellerOrder = order.getOrderItems().stream()
                                    .anyMatch(item -> item.getBook().getShop().getUser().getUserId().equals(user.getUserId()));
        
        if (!isSellerOrder) {
            // If the order does not contain any items from this seller's shop, deny access
            return "redirect:/seller/orders";
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        model.addAttribute("order", order);
        return "seller/order-detail";
    }

    @GetMapping("/cart")
    public String showCart(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                return "seller/cart";
            } else {
                return "redirect:/seller/login";
            }
        } catch (Exception e) {
            log.error("Error displaying seller cart: {}", e.getMessage());
            return "redirect:/seller/login";
        }
    }

    @GetMapping("/shop-information/edit")
    public String showEditShopForm(Model model) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }

            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                model.addAttribute("errorMessage", "Shop information not found. Please register your shop first.");
                return "redirect:/seller/shop-information";
            }

            ShopFormDTO shopForm = new ShopFormDTO();
            shopForm.setShopId(shop.getShopId());
            shopForm.setShopName(shop.getShopName());
            shopForm.setDescription(shop.getDescription());
            shopForm.setShopDetailAddress(shop.getShopDetailAddress());
            shopForm.setShopWardName(shop.getShopWard());
            shopForm.setShopDistrictName(shop.getShopDistrict());
            shopForm.setShopProvinceName(shop.getShopProvince());
            shopForm.setContactEmail(shop.getContactEmail());
            shopForm.setContactPhone(shop.getContactPhone());
            shopForm.setTaxCode(shop.getTaxCode());
            shopForm.setExistingLogoUrl(shop.getLogoUrl());
            shopForm.setExistingCoverImageUrl(shop.getCoverImageUrl());
            shopForm.setExistingIdentificationFileUrl(shop.getIdentificationFileUrl());
            shopForm.setIsActive(shop.isActive());

            model.addAttribute("shopForm", shopForm);
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));

            return "seller/seller-edit-shop";
        } catch (Exception e) {
            log.error("Error displaying edit shop form: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading shop information: " + e.getMessage());
            return "redirect:/seller/shop-information";
        }
    }

    @PostMapping("/shop-information/edit")
    public String processEditShop(
            @Valid @ModelAttribute("shopForm") ShopFormDTO shopForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            User user = getCurrentUser();
            if (user == null) {
                return "redirect:/seller/login";
            }

            if (bindingResult.hasErrors()) {
                log.debug("Validation errors: {}", bindingResult.getAllErrors());
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                // Re-add existing file URLs if validation fails and no new files were uploaded
                Shop existingShop = shopService.getShopByUserId(user.getUserId());
                if (existingShop != null) {
                    shopForm.setExistingLogoUrl(existingShop.getLogoUrl());
                    shopForm.setExistingCoverImageUrl(existingShop.getCoverImageUrl());
                    shopForm.setExistingIdentificationFileUrl(existingShop.getIdentificationFileUrl());
                }
                return "seller/seller-edit-shop";
            }
            
            shopService.updateShop(shopForm);
            redirectAttributes.addFlashAttribute("successMessage", "Shop information updated successfully!");
            return "redirect:/seller/shop-information";
        } catch (IllegalArgumentException e) {
            log.error("Error updating shop: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.error("File upload error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating shop: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
        }
        // In case of error, redirect back to the edit form or shop info page
        return "redirect:/seller/shop-information";
    }

    // Helper methods for authentication
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUser(auth);
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = null;
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) auth).getPrincipal();
            email = oauth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }
        if (email == null) return null;
        Optional<User> userOpt = userService.findByEmail(email);
        return userOpt.orElse(null);
    }
}