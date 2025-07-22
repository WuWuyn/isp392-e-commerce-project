package com.example.isp392.model.enums;

/**
 * Enum representing the reference type for wallet transactions
 */
public enum WalletReferenceType {
    ORDER_REFUND,           // Refund from cancelled order
    MANUAL_ADD,             // Manual addition by admin
    PURCHASE_DEDUCTION,     // Deduction for purchase
    SYSTEM_ADJUSTMENT,      // System adjustment
    PROMOTION_CREDIT,       // Credit from promotions
    WITHDRAWAL,             // Withdrawal to external account
    ORDER_PAYMENT           // Payment to seller for delivered order
}
