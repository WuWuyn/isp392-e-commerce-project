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
     * Process refund to wallet for cancelled individual order
     * @param order The cancelled order
     * @return The wallet transaction if refund was processed, null if not applicable
     */
    @Transactional
    public WalletTransaction processOrderRefund(Order order) {
        // Validate refund conditions
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        CustomerOrder customerOrder = order.getCustomerOrder();
        if (customerOrder == null) {
            log.warn("No customer order found for order: {}", order.getOrderId());
            return null;
        }

        if (customerOrder.getPaymentMethod() != PaymentMethod.VNPAY) {
            log.info("No refund needed for order {} - payment method is not VNPAY",
                    order.getOrderId());
            return null;
        }

        if (customerOrder.getPaymentStatus() != PaymentStatus.PAID) {
            log.info("No refund needed for order {} - payment status is not PAID",
                    order.getOrderId());
            return null;
        }

        // Check if refund already processed for this specific order
        boolean refundExists = walletTransactionRepository.existsByReferenceTypeAndReferenceId(
                WalletReferenceType.ORDER_REFUND, order.getOrderId());

        if (refundExists) {
            log.warn("Refund already processed for order: {}", order.getOrderId());
            return null;
        }

        // Use the individual order's total amount for refund
        BigDecimal refundAmount = order.getTotalAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid refund amount for order: {}", order.getOrderId());
            return null;
        }

        String description = String.format("Refund for cancelled order #%d (Shop: %s)",
                order.getOrderId(),
                order.getShop() != null ? order.getShop().getShopName() : "Unknown");

        try {
            WalletTransaction refundTransaction = addFunds(
                    customerOrder.getUser(),
                    refundAmount,
                    description,
                    WalletReferenceType.ORDER_REFUND,
                    order.getOrderId(), // Use order ID instead of customer order ID
                    customerOrder.getUser().getUserId()
            );

            log.info("Processed refund of {} for order {} (Shop: {}) to user {}",
                    refundAmount, order.getOrderId(),
                    order.getShop() != null ? order.getShop().getShopName() : "Unknown",
                    customerOrder.getUser().getEmail());

            return refundTransaction;
        } catch (Exception e) {
            log.error("Failed to process refund for order {}: {}",
                    order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    /**
     * Process refund to wallet for cancelled customer order (legacy method)
     * @param customerOrder The cancelled customer order
     * @return The wallet transaction if refund was processed, null if not applicable
     * @deprecated Use processOrderRefund(Order) instead for individual order refunds
     */
    @Deprecated
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
     * Check if an order has already been refunded
     * @param orderId The order ID to check
     * @return true if refund exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isOrderRefunded(Integer orderId) {
        return walletTransactionRepository.existsByReferenceTypeAndReferenceId(
                WalletReferenceType.ORDER_REFUND, orderId);
    }

    /**
     * Get refund transaction for a specific order
     * @param orderId The order ID
     * @return The refund transaction if exists, null otherwise
     */
    @Transactional(readOnly = true)
    public WalletTransaction getOrderRefundTransaction(Integer orderId) {
        List<WalletTransaction> transactions = walletTransactionRepository.findByReferenceTypeAndReferenceId(
                WalletReferenceType.ORDER_REFUND, orderId);
        return transactions.isEmpty() ? null : transactions.get(0);
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

    /**
     * Process a withdrawal request
     * @param user The user requesting withdrawal
     * @param withdrawalRequest The withdrawal request details
     * @return The wallet transaction
     * @throws RuntimeException if validation fails or insufficient balance
     */
    @Transactional
    public WalletTransaction processWithdrawal(User user, com.example.isp392.dto.WithdrawalRequestDTO withdrawalRequest) {
        if (withdrawalRequest == null) {
            throw new IllegalArgumentException("Withdrawal request cannot be null");
        }

        BigDecimal amount = withdrawalRequest.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        // Validate amount limits
        BigDecimal minAmount = new BigDecimal("10000"); // 10,000 VND
        BigDecimal maxAmount = new BigDecimal("50000000"); // 50,000,000 VND

        if (amount.compareTo(minAmount) < 0) {
            throw new RuntimeException("Minimum withdrawal amount is 10,000 VND");
        }

        if (amount.compareTo(maxAmount) > 0) {
            throw new RuntimeException("Maximum withdrawal amount is 50,000,000 VND per transaction");
        }

        // Validate bank account details
        if (withdrawalRequest.getBankName() == null || withdrawalRequest.getBankName().trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name is required");
        }

        if (withdrawalRequest.getAccountNumber() == null || withdrawalRequest.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        if (withdrawalRequest.getAccountHolderName() == null || withdrawalRequest.getAccountHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name is required");
        }

        // Get wallet and check balance
        Wallet wallet = getWalletByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + user.getEmail()));

        if (!wallet.hasSufficientBalance(amount)) {
            throw new RuntimeException("Insufficient balance. Available: " +
                String.format("%,d VND", wallet.getBalance().longValue()) +
                ", Requested: " + String.format("%,d VND", amount.longValue()));
        }

        // Create withdrawal description with bank details
        String description = String.format("Withdrawal to %s - Account: %s (%s)",
                withdrawalRequest.getBankName().trim(),
                withdrawalRequest.getAccountNumber().trim(),
                withdrawalRequest.getAccountHolderName().trim());

        try {
            // Process the withdrawal using existing deductFunds method
            WalletTransaction transaction = deductFunds(
                    user,
                    amount,
                    description,
                    WalletReferenceType.WITHDRAWAL,
                    null, // No specific reference ID for withdrawals
                    user.getUserId()
            );

            log.info("Processed withdrawal of {} for user: {} to bank account: {} - {}",
                    amount, user.getEmail(), withdrawalRequest.getBankName(), withdrawalRequest.getAccountNumber());

            return transaction;
        } catch (Exception e) {
            log.error("Failed to process withdrawal for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to process withdrawal: " + e.getMessage(), e);
        }
    }
}
