package com.example.isp392.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Address information
 * Used for creating and updating user addresses
 */
@Data // Lombok annotation to generate getters, setters, equals, hashCode, and toString methods
@NoArgsConstructor // Generate a no-args constructor
@AllArgsConstructor // Generate a constructor with all args
public class AddressDTO {
    
    // Primary key for address
    private Integer addressId;
    
    // Recipient name with validation for required field and length
    @NotBlank(message = "Recipient name is required")
    @Size(min = 2, max = 100, message = "Recipient name must be between 2 and 100 characters")
    private String recipientName;
    
    // Phone number with validation for required field and Vietnamese phone number format
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+84|84|0)[3|5|7|8|9][0-9]{8}$", message = "Invalid phone number format")
    private String recipientPhone;
    
    // Province/City with validation for required field
    @NotBlank(message = "Province/City is required")
    private String province;
    
    // District with validation for required field
    @NotBlank(message = "District is required")
    private String district;
    
    // Ward with validation for required field
    @NotBlank(message = "Ward is required")
    private String ward;
    
    // Address details with validation for required field and max length
    @NotBlank(message = "Address details are required")
    @Size(max = 500, message = "Address details must be less than 500 characters")
    private String addressDetail;
    
    // Optional company name
    @Size(max = 255, message = "Company name must be less than 255 characters")
    private String company;
    
    // Address type with validation for required field (0: Residential, 1: Company)
    @NotNull(message = "Address type is required")
    private Integer addressType;
    
    // Flag to indicate if this is the default address
    private boolean isDefault;
    
    /**
     * Generate the full address string representation
     * @return formatted full address string
     */
    public String getFullAddress() {
        StringBuilder builder = new StringBuilder();
        builder.append(addressDetail);
        
        if (ward != null && !ward.isEmpty()) {
            builder.append(", ").append(ward);
        }
        
        if (district != null && !district.isEmpty()) {
            builder.append(", ").append(district);
        }
        
        if (province != null && !province.isEmpty()) {
            builder.append(", ").append(province);
        }
        
        return builder.toString();
    }
}
