package com.example.isp392.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DiscountValidationResponse {
    private boolean valid;
    private String message;
    private BigDecimal amount;
} 