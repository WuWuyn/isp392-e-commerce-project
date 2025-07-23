package com.example.isp392.repository;

import com.example.isp392.model.Wallet;
import com.example.isp392.model.WalletTransaction;
import com.example.isp392.model.enums.WalletReferenceType;
import com.example.isp392.model.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for WalletTransaction entity
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {
    
    /**
     * Find transactions by wallet
     * @param wallet The wallet
     * @param pageable Pagination information
     * @return Page of wallet transactions
     */
    Page<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);
    
    /**
     * Find transactions by wallet ID
     * @param walletId The wallet ID
     * @param pageable Pagination information
     * @return Page of wallet transactions
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet.walletId = :walletId ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") Integer walletId, Pageable pageable);
    
    /**
     * Find transactions by wallet and transaction type
     * @param wallet The wallet
     * @param transactionType The transaction type
     * @param pageable Pagination information
     * @return Page of wallet transactions
     */
    Page<WalletTransaction> findByWalletAndTransactionTypeOrderByCreatedAtDesc(
            Wallet wallet, WalletTransactionType transactionType, Pageable pageable);
    
    /**
     * Find transactions by reference type and reference ID
     * @param referenceType The reference type
     * @param referenceId The reference ID
     * @return List of wallet transactions
     */
    List<WalletTransaction> findByReferenceTypeAndReferenceId(WalletReferenceType referenceType, Integer referenceId);
    
    /**
     * Check if transaction exists for reference
     * @param referenceType The reference type
     * @param referenceId The reference ID
     * @return true if transaction exists, false otherwise
     */
    boolean existsByReferenceTypeAndReferenceId(WalletReferenceType referenceType, Integer referenceId);
    
    /**
     * Find latest transaction by wallet
     * @param wallet The wallet
     * @return Optional containing the latest transaction if found
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet = :wallet ORDER BY wt.createdAt DESC")
    Optional<WalletTransaction> findLatestByWallet(@Param("wallet") Wallet wallet);
}
