package com.example.isp392.dto;

import com.example.isp392.model.Shop.ApprovalStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for Shop operations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopDTO {
    
    private Integer shopId;
    
    private Integer userId;
    
    @NotBlank(message = "Shop name is required")
    @Size(min = 3, max = 150, message = "Shop name must be between 3 and 150 characters")
    private String shopName;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    private String logoUrl;
    
    private String coverImageUrl;
    
    @NotBlank(message = "Shop address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String shopDetailAddress;
    
    @NotBlank(message = "Ward is required")
    private String shopWard;
    
    @NotBlank(message = "District is required")
    private String shopDistrict;
    
    @NotBlank(message = "Province is required")
    private String shopProvince;
    
    @NotBlank(message = "Contact email is required")
    @Email(message = "Please provide a valid email address")
    private String contactEmail;
    
    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Please provide a valid phone number (10-15 digits)")
    private String contactPhone;
    
    @NotBlank(message = "Tax code is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Please provide a valid tax code (10-15 digits)")
    private String taxCode;
    
    private String identificationFileUrl;
    
    private ApprovalStatus approvalStatus;
    
    private String reasonForStatus;
    
    private LocalDateTime requestAt;
    
    private LocalDateTime registrationDate;
    
    private boolean isActive = true;
    
    private Integer adminApproverId;
} 