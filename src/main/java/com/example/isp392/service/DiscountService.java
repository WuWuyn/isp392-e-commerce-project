package com.example.isp392.service;

import com.example.isp392.dto.DiscountValidationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class DiscountService {
    
    public DiscountValidationResponse validateDiscountCode(String code, BigDecimal subtotal) {
        // TODO: Implement actual discount validation logic with database
        // This is just a sample implementation
        DiscountValidationResponse response = new DiscountValidationResponse();
        
        if (code == null || code.trim().isEmpty()) {
            response.setValid(false);
            response.setMessage("Please enter a discount code");
            return response;
        }

        // Sample validation logic
        switch (code.toUpperCase()) {
            case "WELCOME10":
                if (isValidWelcomeCode()) {
                    response.setValid(true);
                    response.setMessage("10% discount applied successfully");
                    response.setAmount(subtotal.multiply(new BigDecimal("0.10")));
                } else {
                    response.setValid(false);
                    response.setMessage("Welcome code has expired");
                }
                break;
            case "SAVE20":
                if (subtotal.compareTo(new BigDecimal("1000000")) >= 0) {
                    response.setValid(true);
                    response.setMessage("20% discount applied successfully");
                    response.setAmount(subtotal.multiply(new BigDecimal("0.20")));
                } else {
                    response.setValid(false);
                    response.setMessage("Minimum order amount of 1,000,000 VND required for this code");
                }
                break;
            default:
                response.setValid(false);
                response.setMessage("Invalid discount code");
        }
        
        return response;
    }
    
    private boolean isValidWelcomeCode() {
        // Add your welcome code validation logic here
        // For example, check if it's within valid date range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        return now.isBefore(endDate);
    }
} 