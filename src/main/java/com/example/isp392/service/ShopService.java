package com.example.isp392.service;

import com.example.isp392.dto.ShopDTO;
import com.example.isp392.dto.ShopFormDTO;
import com.example.isp392.model.Shop;
import com.example.isp392.model.User;
import com.example.isp392.repository.ShopRepository;
import com.example.isp392.repository.UserRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for shop operations
 */
@Service // Mark as Spring service component
public class ShopService {
    
    // Repository dependencies
    private final ShopRepository shopRepository; // For shop data access
    private final UserRepository userRepository; // For user data access
    private final String UPLOAD_DIR = "src/main/resources/static/uploads/shop/";
    
    /**
     * Constructor for dependency injection
     * @param shopRepository repository for shop data
     * @param userRepository repository for user data
     */
    public ShopService(ShopRepository shopRepository, UserRepository userRepository) {
        // Constructor injection instead of using @Autowired
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        // Ensure the upload directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
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

    /**
     * Update existing shop information from ShopFormDTO
     * @param shopForm DTO with updated shop information
     * @return updated shop entity
     * @throws IOException if there's an error during file upload
     */
    @Transactional
    public Shop updateShop(ShopFormDTO shopForm) throws IOException {
        Shop shop = shopRepository.findById(shopForm.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found with ID: " + shopForm.getShopId()));

        // Update basic fields
        shop.setShopName(shopForm.getShopName());
        shop.setDescription(shopForm.getDescription());
        shop.setShopDetailAddress(shopForm.getShopDetailAddress());
        shop.setShopWard(shopForm.getShopWardName());
        shop.setShopDistrict(shopForm.getShopDistrictName());
        shop.setShopProvince(shopForm.getShopProvinceName());
        shop.setContactEmail(shopForm.getContactEmail());
        shop.setContactPhone(shopForm.getContactPhone());
        shop.setTaxCode(shopForm.getTaxCode());

        // Set isActive status based on form submission
        if (shopForm.getIsActive() != null) {
            shop.setIsActive(shopForm.getIsActive());
        } else {
            // Default to false if checkbox is not checked (and thus not present in request params)
            shop.setIsActive(false);
        }

        // Handle logo file upload
        if (shopForm.getLogoFile() != null && !shopForm.getLogoFile().isEmpty()) {
            String logoUrl = saveFile(shopForm.getLogoFile(), "logo");
            shop.setLogoUrl(logoUrl);
        } else if (shopForm.getExistingLogoUrl() == null || shopForm.getExistingLogoUrl().isEmpty()) {
            // If no new file and no existing URL, set to null
            shop.setLogoUrl(null);
        }

        // Handle cover image file upload
        if (shopForm.getCoverImageFile() != null && !shopForm.getCoverImageFile().isEmpty()) {
            String coverImageUrl = saveFile(shopForm.getCoverImageFile(), "cover");
            shop.setCoverImageUrl(coverImageUrl);
        } else if (shopForm.getExistingCoverImageUrl() == null || shopForm.getExistingCoverImageUrl().isEmpty()) {
            shop.setCoverImageUrl(null);
        }

        // Handle identification file upload
        if (shopForm.getIdentificationFile() != null && !shopForm.getIdentificationFile().isEmpty()) {
            String identificationUrl = saveFile(shopForm.getIdentificationFile(), "identification");
            shop.setIdentificationFileUrl(identificationUrl);
        } else if (shopForm.getExistingIdentificationFileUrl() == null || shopForm.getExistingIdentificationFileUrl().isEmpty()) {
            shop.setIdentificationFileUrl(null);
        }

        return shopRepository.save(shop);
    }

    private String saveFile(MultipartFile file, String type) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath);
        // Return the relative path for web access
        return "/uploads/shop/" + uniqueFileName;
    }
} 