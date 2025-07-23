package com.example.isp392.dto;

import com.example.isp392.model.enums.WalletReferenceType;
import com.example.isp392.model.enums.WalletTransactionType;
import com.example.isp392.model.enums.WalletTransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Wallet Transaction information
 */
@Data
public class WalletTransactionDTO {
    private Integer transactionId;
    private Integer walletId;
    private WalletTransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private WalletReferenceType referenceType;
    private Integer referenceId;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private String createdByName;
    private WalletTransactionStatus status;
}
