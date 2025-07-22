package com.example.isp392.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Wallet Transaction History response
 */
@Data
public class WalletTransactionHistoryDTO {
    private BigDecimal currentBalance;
    private String formattedBalance;
    private Page<WalletTransactionDTO> transactions;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public WalletTransactionHistoryDTO(BigDecimal currentBalance, Page<WalletTransactionDTO> transactions) {
        this.currentBalance = currentBalance;
        this.formattedBalance = formatBalance(currentBalance);
        this.transactions = transactions;
        this.totalPages = transactions.getTotalPages();
        this.totalElements = transactions.getTotalElements();
        this.hasNext = transactions.hasNext();
        this.hasPrevious = transactions.hasPrevious();
    }
    
    private String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", balance);
    }
}
