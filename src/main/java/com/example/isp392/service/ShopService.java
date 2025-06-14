// src/main/java/com/example/isp392/service/ShopService.java
package com.example.isp392.service;

import com.example.isp392.model.ApprovalStatus;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ShopService(ShopRepository shopRepository, UserService userService, FileStorageService fileStorageService) {
        this.shopRepository = shopRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
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
        shop.setApprovalStatus(ApprovalStatus.PENDING); // Luôn set lại là PENDING
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

    public List<Shop> getPendingShops() {
        return shopRepository.findByApprovalStatus(ApprovalStatus.PENDING);
    }

    public void approveShop(Integer shopId, User adminApprover) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with id: " + shopId));

        shop.setApprovalStatus(ApprovalStatus.APPROVED);
        shop.setAdminApproverId(adminApprover);
        shop.setRegistrationDate(LocalDateTime.now());
        shop.setActive(true); // Kích hoạt shop
        shopRepository.save(shop);

        userService.upgradeUserToSeller(shop.getUser().getUserId());
    }

    public void rejectShop(Integer shopId, String reason, User adminApprover) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with id: " + shopId));

        shop.setApprovalStatus(ApprovalStatus.REJECTED);
        shop.setReasonForStatus(reason);
        shop.setAdminApproverId(adminApprover);
        shop.setActive(false);
        shopRepository.save(shop);
    }
}