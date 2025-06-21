package com.example.isp392.service;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.ShopRepository;
import com.example.isp392.repository.UserRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for shop operations
 */
@Service // Mark as Spring service component
public class ShopService {
    
    // Repository dependencies
    private final ShopRepository shopRepository; // For shop data access
    private final UserRepository userRepository; // For user data access
    
    /**
     * Constructor for dependency injection
     * @param shopRepository repository for shop data
     * @param userRepository repository for user data
     */
    public ShopService(ShopRepository shopRepository, UserRepository userRepository) {
        // Constructor injection instead of using @Autowired
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get a shop by user ID
     * @param userId the user ID
     * @return the shop or null if not found
     */
    public Shop getShopByUserId(Integer userId) {
        // Find shop by the user ID using repository
        return shopRepository.findByUserUserId(userId);
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
} 