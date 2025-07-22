package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user's digital wallet
 */
@Getter
@Setter
@Entity
@Table(name = "wallets")
public class Wallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "balance", nullable = false, precision = 18, scale = 0)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletTransaction> transactions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if wallet has sufficient balance for a transaction
     * @param amount Amount to check
     * @return true if sufficient balance, false otherwise
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance != null && amount != null && 
               balance.compareTo(amount) >= 0;
    }
    
    /**
     * Add amount to wallet balance
     * @param amount Amount to add
     */
    public void addBalance(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }
    
    /**
     * Deduct amount from wallet balance
     * @param amount Amount to deduct
     * @throws IllegalArgumentException if insufficient balance
     */
    public void deductBalance(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (!hasSufficientBalance(amount)) {
                throw new IllegalArgumentException("Insufficient wallet balance");
            }
            this.balance = this.balance.subtract(amount);
        }
    }
}
