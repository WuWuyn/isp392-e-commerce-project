package com.example.isp392.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ShopFormDTO {
    private Integer shopId;

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 150, message = "Shop name must be between 2 and 150 characters")
    private String shopName;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private MultipartFile logoFile;
    private String existingLogoUrl; // To hold the URL of the existing logo

    private MultipartFile coverImageFile;
    private String existingCoverImageUrl; // To hold the URL of the existing cover image

    @NotBlank(message = "Detail address is required")
    @Size(min = 5, max = 500, message = "Detail address must be between 5 and 500 characters")
    private String shopDetailAddress;

    private String shopWardName;
    private String shopDistrictName;
    private String shopProvinceName;

    private String shopProvinceCode;
    private String shopDistrictCode;
    private String shopWardCode;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Contact email cannot exceed 255 characters")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^(\\+84|84|0)[35789][0-9]{8}$", message = "Invalid Vietnamese phone number format")
    private String contactPhone;

    @NotBlank(message = "Tax code is required")
    @Size(min = 10, max = 15, message = "Tax code must be between 10 and 15 characters")
    private String taxCode;

    private Boolean isActive;

    private MultipartFile identificationFile;
    private String existingIdentificationFileUrl; // To hold the URL of the existing identification file
} 