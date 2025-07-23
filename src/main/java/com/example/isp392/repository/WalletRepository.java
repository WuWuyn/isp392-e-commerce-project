package com.example.isp392.repository;

import com.example.isp392.model.User;
import com.example.isp392.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Wallet entity
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    
    /**
     * Find wallet by user
     * @param user The user
     * @return Optional containing the wallet if found
     */
    Optional<Wallet> findByUser(User user);
    
    /**
     * Find wallet by user ID
     * @param userId The user ID
     * @return Optional containing the wallet if found
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserId(@Param("userId") Integer userId);
    
    /**
     * Check if wallet exists for user
     * @param user The user
     * @return true if wallet exists, false otherwise
     */
    boolean existsByUser(User user);
    
    /**
     * Find active wallet by user
     * @param user The user
     * @return Optional containing the active wallet if found
     */
    @Query("SELECT w FROM Wallet w WHERE w.user = :user AND w.isActive = true")
    Optional<Wallet> findActiveWalletByUser(@Param("user") User user);
    
    /**
     * Find wallet with transactions by user ID
     * @param userId The user ID
     * @return Optional containing the wallet with transactions if found
     */
    @Query("SELECT w FROM Wallet w LEFT JOIN FETCH w.transactions WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserIdWithTransactions(@Param("userId") Integer userId);
}
