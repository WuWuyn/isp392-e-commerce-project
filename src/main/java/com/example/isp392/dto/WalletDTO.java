package com.example.isp392.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Wallet information
 */
@Data
public class WalletDTO {
    private Integer walletId;
    private Integer userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal balance;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
