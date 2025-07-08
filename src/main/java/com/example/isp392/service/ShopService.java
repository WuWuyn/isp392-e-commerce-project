package com.example.isp392.service;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.ShopApprovalHistory;
import com.example.isp392.model.User;
import com.example.isp392.model.TokenType;
import com.example.isp392.repository.OrderRepository;
import com.example.isp392.repository.ShopApprovalHistoryRepository;
import com.example.isp392.repository.ShopRepository;
import com.example.isp392.repository.UserRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for shop operations
 */
@Service // Mark as Spring service component
public class ShopService {

    // Repository dependencies
    private final ShopRepository shopRepository; // For shop data access
    private final UserRepository userRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;// For user data access
    private final ShopApprovalHistoryRepository historyRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final BookService bookService;

    /**
     * Constructor for dependency injection
     * @param shopRepository repository for shop data
     * @param userRepository repository for user data
     */
    public ShopService(ShopRepository shopRepository, UserRepository userRepository, UserService userService, FileStorageService fileStorageService, ShopApprovalHistoryRepository historyRepository, OrderRepository orderRepository, EmailService emailService, OtpService otpService, BookService bookService) {
        // Constructor injection instead of using @Autowired
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.historyRepository = historyRepository;
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.otpService = otpService;
        this.bookService = bookService;
    }

    /**
     * Get a shop by user ID
     * @param userId the user ID
     * @return the shop or null if not found
     */
    public Shop getShopByUserId(Integer userId) {
        return shopRepository.findByUserUserId(userId).orElse(null);
    }
    
    /**
     * Save or update shop information
     * @param shopDTO the shop data to save
     * @return the updated shop
     */
    @Transactional // Ensure transaction management
    public Shop saveShopInformation(ShopDTO shopDTO) {
        // Find existing shop or create new one
        Shop shop = shopDTO.getShopId() != null ?
                shopRepository.findById(shopDTO.getShopId())
                        .orElseThrow(() -> new RuntimeException("Shop not found")) :
                new Shop();
        
        // Find the user
        User user = userRepository.findById(shopDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Copy properties from DTO to entity
        BeanUtils.copyProperties(shopDTO, shop, "user", "books", "approval_status", "registrationDate");
        
        // Set specific fields that need manual mapping
        shop.setUser(user);
        
        // If it's a new shop
        if (shop.getShopId() == null) {
            // Set pending status for new shops
            shop.setApproval_status(Shop.ApprovalStatus.PENDING);
            // Set request and registration dates
            shop.setRequestAt(LocalDateTime.now());
            shop.setRegistrationDate(LocalDateTime.now());
        } else if (shopDTO.getApprovalStatus() != null) {
            // Only update status if explicitly set and shop exists
            shop.setApproval_status(shopDTO.getApprovalStatus());
        }
        
        // Save the shop
        return shopRepository.save(shop);
    }

    public Shop getShopById(Integer shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with ID: " + shopId));
    }
    public List<Shop> getPendingShops() {
        return shopRepository.findByApprovalStatus(Shop.ApprovalStatus.PENDING);
    }
    @Transactional
    public void approveShop(Integer shopId, User adminUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with ID: " + shopId));

        shop.setApproval_status(Shop.ApprovalStatus.APPROVED);
        shop.setAdminApproverId(adminUser);
        shop.setActive(true);
        shopRepository.save(shop);

        // Ghi lại lịch sử
        historyRepository.save(new ShopApprovalHistory(shop, adminUser, Shop.ApprovalStatus.APPROVED, "Shop approved."));

        userService.upgradeUserToSeller(shop.getUser().getUserId());
    }
    public long countPendingShops() {
        return shopRepository.countByApprovalStatus(Shop.ApprovalStatus.PENDING);
    }
    @Transactional
    public void rejectShop(Integer shopId, String reason, User adminUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with ID: " + shopId));

        shop.setApproval_status(Shop.ApprovalStatus.REJECTED);
        shop.setReasonForStatus(reason); // Vẫn lưu lý do vào shop để hiển thị khi user đăng ký lại
        shop.setAdminApproverId(adminUser);
        shopRepository.save(shop);

        // Ghi lại lịch sử
        historyRepository.save(new ShopApprovalHistory(shop, adminUser, Shop.ApprovalStatus.REJECTED, reason));
    }
    public Optional<Shop> findShopByUserId(Integer userId) {
        return shopRepository.findByUserUserId(userId);
    }
    public Shop registerNewShop(Shop shop, User user, MultipartFile logoFile, MultipartFile coverImageFile, MultipartFile idFile) throws IOException {
        // Xử lý upload file (chỉ upload nếu file mới được cung cấp)
        if (logoFile != null && !logoFile.isEmpty()) {
            shop.setLogoUrl(fileStorageService.storeFile(logoFile, "shop-logos"));
        }
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            shop.setCoverImageUrl(fileStorageService.storeFile(coverImageFile, "shop-covers"));
        }
        if (idFile != null && !idFile.isEmpty()) {
            shop.setIdentificationFileUrl(fileStorageService.storeFile(idFile, "shop-identifications"));
        }

        shop.setUser(user);

        // === THAY ĐỔI QUAN TRỌNG: RESET TRẠNG THÁI KHI NỘP LẠI ===
        shop.setApproval_status(Shop.ApprovalStatus.PENDING); // Luôn set lại là PENDING
        shop.setRequestAt(LocalDateTime.now());         // Cập nhật thời gian yêu cầu
        shop.setReasonForStatus(null);                  // Xóa lý do từ chối cũ
        shop.setAdminApproverId(null);                  // Xóa người duyệt cũ
        shop.setActive(false);                          // Shop chưa hoạt động

        // Chỉ đặt ngày đăng ký nếu đây là lần đầu
        if (shop.getRegistrationDate() == null) {
            shop.setRegistrationDate(LocalDateTime.now());
        }

        return shopRepository.save(shop);
    }
    public List<Shop> getRejectedShops() {
        return shopRepository.findByApprovalStatus(Shop.ApprovalStatus.REJECTED);
    }
    public LocalDateTime getRegistrationDateByShopId(Integer shopId) {
        return shopRepository.getRegistrationDateByShopId(shopId);
    }
    public long countActiveSellers() {
        return shopRepository.countByApprovalStatus(com.example.isp392.model.Shop.ApprovalStatus.APPROVED);
    }

    /**
     * Check if a shop has any active orders.
     * 'Active' means PENDING, PROCESSING, or SHIPPED.
     * @param shopId the ID of the shop to check
     * @return true if the shop has active orders, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveOrdersForShop(Integer shopId) {
        long activeOrderCount = orderRepository.countActiveOrdersByShopId(shopId);
        return activeOrderCount > 0;
    }

    /**
     * Performs a soft delete on a shop.
     * Sets the shop's isActive status to false and deactivates all its books.
     * @param shopId the ID of the shop to deactivate
     * @throws RuntimeException if the shop is not found
     */
    @Transactional
    public void softDeleteShop(Integer shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with ID: " + shopId));
        shop.setActive(false);
        shopRepository.save(shop);
        bookService.deactivateBooksByShopId(shopId);
        // Optionally, set approval status to a 'DELETED' or 'INACTIVE' enum if needed
        // shop.setApproval_status(Shop.ApprovalStatus.INACTIVE);
        // shopRepository.save(shop);
        // log.info("Shop with ID {} has been soft-deleted (deactivated) and its products deactivated.", shopId);
    }

    /**
     * Initiates the shop deletion process by sending a confirmation email.
     * @param shop The shop to be deleted.
     * @param seller The user (seller) associated with the shop.
     * @param baseUrl The base URL for generating the confirmation link.
     * @return true if the email was sent, false otherwise.
     */
    @Transactional
    public boolean requestShopDeletion(Shop shop, User seller, String baseUrl) {
        String confirmationToken = otpService.generateToken(seller, TokenType.SHOP_DELETION);
        String confirmationLink = baseUrl + "/seller/shop-delete-confirm?token=" + confirmationToken;
        return emailService.sendShopDeletionConfirmationEmail(seller.getEmail(), confirmationLink);
    }

    /**
     * Confirms the shop deletion based on a valid token.
     * If the token is valid, the shop and its associated user are soft-deleted,
     * and an email is sent to the seller.
     * @param tokenString The token received in the confirmation link.
     * @return The User object of the seller if deletion is successful, null otherwise.
     */
    @Transactional
    public User confirmShopDeletion(String tokenString) {
        Optional<User> userOptional = otpService.validateToken(tokenString, TokenType.SHOP_DELETION);
        if (userOptional.isEmpty()) {
            return null;
        }

        User seller = userOptional.get();
        Shop shop = shopRepository.findByUserUserId(seller.getUserId()).orElse(null);

        if (shop == null) {
            // This should ideally not happen if the token is valid and associated with a seller
            return null;
        }

        // Perform soft deletion of the shop and its books
        softDeleteShop(shop.getShopId());

        // Soft delete the user (seller) associated with the shop
        // Assuming userService.deactivateUser exists and handles soft deletion of user
        userService.deactivateUser(seller.getEmail());

        return seller;
    }
} 