package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.model.enums.WalletReferenceType;
import com.example.isp392.model.enums.WalletTransactionType;
import com.example.isp392.repository.WalletRepository;
import com.example.isp392.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for managing wallet operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ReentrantLock walletLock = new ReentrantLock();
    
    /**
     * Create a new wallet for a user
     * @param user The user
     * @return The created wallet
     */
    @Transactional
    public Wallet createWalletForUser(User user) {
        try {
            walletLock.lock();
            
            // Check if wallet already exists
            Optional<Wallet> existingWallet = walletRepository.findByUser(user);
            if (existingWallet.isPresent()) {
                log.warn("Wallet already exists for user: {}", user.getEmail());
                return existingWallet.get();
            }
            
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setActive(true);
            
            Wallet savedWallet = walletRepository.save(wallet);
            log.info("Created wallet for user: {} with ID: {}", user.getEmail(), savedWallet.getWalletId());
            
            return savedWallet;
        } finally {
            walletLock.unlock();
        }
    }
    
    /**
     * Get wallet by user
     * @param user The user
     * @return Optional containing the wallet if found
     */
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByUser(User user) {
        return walletRepository.findActiveWalletByUser(user);
    }
    
    /**
     * Get wallet by user ID
     * @param userId The user ID
     * @return Optional containing the wallet if found
     */
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByUserId(Integer userId) {
        return walletRepository.findByUserId(userId);
    }
    
    /**
     * Get wallet balance for a user
     * @param user The user
     * @return The wallet balance, or zero if no wallet exists
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(User user) {
        return getWalletByUser(user)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Add funds to wallet
     * @param user The user
     * @param amount The amount to add
     * @param description Transaction description
     * @param referenceType Reference type
     * @param referenceId Reference ID
     * @param createdBy User who initiated the transaction
     * @return The wallet transaction
     */
    @Transactional
    public WalletTransaction addFunds(User user, BigDecimal amount, String description,
                                    WalletReferenceType referenceType, Integer referenceId, Integer createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        try {
            walletLock.lock();
            
            Wallet wallet = getWalletByUser(user)
                    .orElseGet(() -> createWalletForUser(user));
            
            BigDecimal balanceBefore = wallet.getBalance();
            wallet.addBalance(amount);
            BigDecimal balanceAfter = wallet.getBalance();
            
            walletRepository.save(wallet);
            
            WalletTransaction transaction = WalletTransaction.createCredit(
                    wallet, amount, balanceBefore, balanceAfter, description,
                    referenceType, referenceId, createdBy);
            
            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
            
            log.info("Added {} to wallet for user: {}. New balance: {}", 
                    amount, user.getEmail(), balanceAfter);
            
            return savedTransaction;
        } finally {
            walletLock.unlock();
        }
    }

    /**
     * Deduct funds from wallet
     * @param user The user
     * @param amount The amount to deduct
     * @param description Transaction description
     * @param referenceType Reference type
     * @param referenceId Reference ID
     * @param createdBy User who initiated the transaction
     * @return The wallet transaction
     */
    @Transactional
    public WalletTransaction deductFunds(User user, BigDecimal amount, String description,
                                       WalletReferenceType referenceType, Integer referenceId, Integer createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        try {
            walletLock.lock();

            Wallet wallet = getWalletByUser(user)
                    .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + user.getEmail()));

            if (!wallet.hasSufficientBalance(amount)) {
                throw new RuntimeException("Insufficient wallet balance. Current balance: " + wallet.getBalance());
            }

            BigDecimal balanceBefore = wallet.getBalance();
            wallet.deductBalance(amount);
            BigDecimal balanceAfter = wallet.getBalance();

            walletRepository.save(wallet);

            WalletTransaction transaction = WalletTransaction.createDebit(
                    wallet, amount, balanceBefore, balanceAfter, description,
                    referenceType, referenceId, createdBy);

            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

            log.info("Deducted {} from wallet for user: {}. New balance: {}",
                    amount, user.getEmail(), balanceAfter);

            return savedTransaction;
        } finally {
            walletLock.unlock();
        }
    }

    /**
     * Process refund to wallet for cancelled order
     * @param customerOrder The cancelled customer order
     * @return The wallet transaction if refund was processed, null if not applicable
     */
    @Transactional
    public WalletTransaction processRefund(CustomerOrder customerOrder) {
        // Validate refund conditions
        if (customerOrder == null) {
            throw new IllegalArgumentException("CustomerOrder cannot be null");
        }

        if (customerOrder.getPaymentMethod() != PaymentMethod.VNPAY) {
            log.info("No refund needed for order {} - payment method is not VNPAY",
                    customerOrder.getCustomerOrderId());
            return null;
        }

        if (customerOrder.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("No refund needed for order {} - payment status is not PAID",
                    customerOrder.getCustomerOrderId());
            return null;
        }

        // Check if refund already processed
        boolean refundExists = walletTransactionRepository.existsByReferenceTypeAndReferenceId(
                WalletReferenceType.ORDER_REFUND, customerOrder.getCustomerOrderId());

        if (refundExists) {
            log.warn("Refund already processed for order: {}", customerOrder.getCustomerOrderId());
            return null;
        }

        BigDecimal refundAmount = customerOrder.getFinalTotalAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid refund amount for order: {}", customerOrder.getCustomerOrderId());
            return null;
        }

        String description = String.format("Refund for cancelled order #%d", customerOrder.getCustomerOrderId());

        try {
            WalletTransaction refundTransaction = addFunds(
                    customerOrder.getUser(),
                    refundAmount,
                    description,
                    WalletReferenceType.ORDER_REFUND,
                    customerOrder.getCustomerOrderId(),
                    customerOrder.getUser().getUserId()
            );

            log.info("Processed refund of {} for order {} to user {}",
                    refundAmount, customerOrder.getCustomerOrderId(), customerOrder.getUser().getEmail());

            return refundTransaction;
        } catch (Exception e) {
            log.error("Failed to process refund for order {}: {}",
                    customerOrder.getCustomerOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    /**
     * Get transaction history for a user's wallet
     * @param user The user
     * @param pageable Pagination information
     * @return Page of wallet transactions
     */
    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(User user, Pageable pageable) {
        Optional<Wallet> walletOpt = getWalletByUser(user);
        if (walletOpt.isEmpty()) {
            return Page.empty(pageable);
        }

        return walletTransactionRepository.findByWalletOrderByCreatedAtDesc(walletOpt.get(), pageable);
    }

    /**
     * Get transaction history by transaction type
     * @param user The user
     * @param transactionType The transaction type
     * @param pageable Pagination information
     * @return Page of wallet transactions
     */
    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistoryByType(User user, WalletTransactionType transactionType, Pageable pageable) {
        Optional<Wallet> walletOpt = getWalletByUser(user);
        if (walletOpt.isEmpty()) {
            return Page.empty(pageable);
        }

        return walletTransactionRepository.findByWalletAndTransactionTypeOrderByCreatedAtDesc(
                walletOpt.get(), transactionType, pageable);
    }

    /**
     * Check if user has sufficient balance for a transaction
     * @param user The user
     * @param amount The amount to check
     * @return true if sufficient balance, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(User user, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return getWalletByUser(user)
                .map(wallet -> wallet.hasSufficientBalance(amount))
                .orElse(false);
    }

    /**
     * Get transactions by reference
     * @param referenceType The reference type
     * @param referenceId The reference ID
     * @return List of wallet transactions
     */
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactionsByReference(WalletReferenceType referenceType, Integer referenceId) {
        return walletTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    /**
     * Create wallet for user if it doesn't exist
     * @param user The user
     * @return The wallet (existing or newly created)
     */
    @Transactional
    public Wallet ensureWalletExists(User user) {
        return getWalletByUser(user).orElseGet(() -> createWalletForUser(user));
    }
}
