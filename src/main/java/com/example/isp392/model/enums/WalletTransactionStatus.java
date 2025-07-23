package com.example.isp392.model.enums;

/**
 * Enum representing the status of wallet transactions
 */
public enum WalletTransactionStatus {
    PENDING,     // Transaction is being processed
    COMPLETED,   // Transaction completed successfully
    FAILED,      // Transaction failed
    CANCELLED    // Transaction was cancelled
}
