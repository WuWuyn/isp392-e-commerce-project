package com.example.isp392.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for Wallet Balance response
 */
@Data
public class WalletBalanceDTO {
    private BigDecimal balance;
    private String formattedBalance;
    private boolean hasWallet;
    
    public WalletBalanceDTO(BigDecimal balance, boolean hasWallet) {
        this.balance = balance;
        this.hasWallet = hasWallet;
        this.formattedBalance = formatBalance(balance);
    }
    
    private String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", balance);
    }
}
