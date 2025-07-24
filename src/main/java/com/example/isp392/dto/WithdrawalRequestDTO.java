package com.example.isp392.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for wallet withdrawal requests
 */
@Data
public class WithdrawalRequestDTO {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum withdrawal amount is 10,000 VND")
    @DecimalMax(value = "50000000", message = "Maximum withdrawal amount is 50,000,000 VND")
    private BigDecimal amount;
    
    @NotBlank(message = "Bank name is required")
    @Size(min = 2, max = 100, message = "Bank name must be between 2 and 100 characters")
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6,20}", message = "Account number must be 6-20 digits")
    private String accountNumber;
    
    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s]+$", message = "Account holder name can only contain letters and spaces")
    private String accountHolderName;
    
    /**
     * Get formatted amount for display
     * @return Formatted amount string
     */
    public String getFormattedAmount() {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,d VND", amount.longValue());
    }
}
