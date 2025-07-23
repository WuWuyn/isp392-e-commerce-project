package com.example.isp392.model;

import com.example.isp392.model.enums.WalletReferenceType;
import com.example.isp392.model.enums.WalletTransactionType;
import com.example.isp392.model.enums.WalletTransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a wallet transaction for audit trail
 */
@Getter
@Setter
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private WalletTransactionType transactionType;
    
    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false, precision = 18, scale = 0)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false, precision = 18, scale = 0)
    private BigDecimal balanceAfter;
    
    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private WalletReferenceType referenceType;
    
    @Column(name = "reference_id")
    private Integer referenceId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Integer createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalletTransactionStatus status = WalletTransactionStatus.COMPLETED;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * Create a credit transaction
     */
    public static WalletTransaction createCredit(Wallet wallet, BigDecimal amount,
                                               BigDecimal balanceBefore, BigDecimal balanceAfter,
                                               String description, WalletReferenceType referenceType,
                                               Integer referenceId, Integer createdBy) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(WalletTransactionType.CREDIT);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);
        transaction.setStatus(WalletTransactionStatus.COMPLETED);
        return transaction;
    }
    
    /**
     * Create a debit transaction
     */
    public static WalletTransaction createDebit(Wallet wallet, BigDecimal amount,
                                              BigDecimal balanceBefore, BigDecimal balanceAfter,
                                              String description, WalletReferenceType referenceType,
                                              Integer referenceId, Integer createdBy) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(WalletTransactionType.DEBIT);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);
        transaction.setStatus(WalletTransactionStatus.COMPLETED);
        return transaction;
    }
}
