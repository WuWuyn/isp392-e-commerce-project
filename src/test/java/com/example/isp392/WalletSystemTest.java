package com.example.isp392;

import com.example.isp392.model.*;
import com.example.isp392.model.enums.WalletReferenceType;
import com.example.isp392.model.enums.WalletTransactionStatus;
import com.example.isp392.model.enums.WalletTransactionType;
import com.example.isp392.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify wallet system compilation and basic functionality
 */
public class WalletSystemTest {

    @Test
    public void testWalletTransactionCreation() {
        // Test creating a wallet transaction
        User user = new User();
        user.setUserId(1);
        user.setEmail("test@example.com");
        
        Wallet wallet = new Wallet();
        wallet.setWalletId(1);
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.valueOf(100000));
        
        // Test creating a credit transaction
        WalletTransaction creditTransaction = WalletTransaction.createCredit(
            wallet,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(100000),
            BigDecimal.valueOf(150000),
            "Test refund",
            WalletReferenceType.ORDER_REFUND,
            123,
            1
        );
        
        assertNotNull(creditTransaction);
        assertEquals(WalletTransactionType.CREDIT, creditTransaction.getTransactionType());
        assertEquals(BigDecimal.valueOf(50000), creditTransaction.getAmount());
        assertEquals(WalletTransactionStatus.COMPLETED, creditTransaction.getStatus());
        assertEquals("Test refund", creditTransaction.getDescription());
        assertEquals(WalletReferenceType.ORDER_REFUND, creditTransaction.getReferenceType());
        assertEquals(Integer.valueOf(123), creditTransaction.getReferenceId());
        
        // Test creating a debit transaction
        WalletTransaction debitTransaction = WalletTransaction.createDebit(
            wallet,
            BigDecimal.valueOf(30000),
            BigDecimal.valueOf(150000),
            BigDecimal.valueOf(120000),
            "Test purchase",
            WalletReferenceType.PURCHASE_DEDUCTION,
            456,
            1
        );
        
        assertNotNull(debitTransaction);
        assertEquals(WalletTransactionType.DEBIT, debitTransaction.getTransactionType());
        assertEquals(BigDecimal.valueOf(30000), debitTransaction.getAmount());
        assertEquals(WalletTransactionStatus.COMPLETED, debitTransaction.getStatus());
        assertEquals("Test purchase", debitTransaction.getDescription());
        assertEquals(WalletReferenceType.PURCHASE_DEDUCTION, debitTransaction.getReferenceType());
        assertEquals(Integer.valueOf(456), debitTransaction.getReferenceId());
    }
    
    @Test
    public void testWalletBalanceOperations() {
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(100000));
        
        // Test sufficient balance check
        assertTrue(wallet.hasSufficientBalance(BigDecimal.valueOf(50000)));
        assertFalse(wallet.hasSufficientBalance(BigDecimal.valueOf(150000)));
        
        // Test add balance
        wallet.addBalance(BigDecimal.valueOf(25000));
        assertEquals(BigDecimal.valueOf(125000), wallet.getBalance());
        
        // Test deduct balance
        wallet.deductBalance(BigDecimal.valueOf(25000));
        assertEquals(BigDecimal.valueOf(100000), wallet.getBalance());
        
        // Test deduct more than balance should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deductBalance(BigDecimal.valueOf(150000));
        });
    }
}
