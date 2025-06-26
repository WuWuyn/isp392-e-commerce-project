package com.example.isp392.service;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.ShopApprovalHistory;
import com.example.isp392.model.User;
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
    /**
     * Constructor for dependency injection
     * @param shopRepository repository for shop data
     * @param userRepository repository for user data
     */
    public ShopService(ShopRepository shopRepository, UserRepository userRepository, UserService userService, FileStorageService fileStorageService, ShopApprovalHistoryRepository historyRepository) {
        // Constructor injection instead of using @Autowired
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.historyRepository = historyRepository;
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
} 