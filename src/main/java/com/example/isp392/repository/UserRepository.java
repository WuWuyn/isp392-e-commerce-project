package com.example.isp392.repository;

import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.userId = :userId")
    Optional<User> findByIdWithAddresses(@Param("userId") Integer userId);
    
    /**
     * Find users registered after a specific date
     * @param date The cutoff date
     * @return Count of users registered after the specified date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    long countByRegistrationDateAfter(@Param("date") LocalDateTime date);

    /**
     * Count users registered within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return Count of users registered in the date range
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countByRegistrationDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get user registration trend data by period
     * @param startDate Start date
     * @param endDate End date
     * @return List of registration data grouped by date
     */
    @Query(value = "SELECT CAST(created_at AS DATE) as registration_date, COUNT(*) as count " +
            "FROM users " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(created_at AS DATE) " +
            "ORDER BY CAST(created_at AS DATE)",
            nativeQuery = true)
    List<Map<String, Object>> getUserRegistrationTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find most recently registered users
     * @param limit Maximum number of users to return
     * @return List of most recently registered users
     */
    @Query(value = "SELECT TOP(:limit) * FROM users ORDER BY user_id DESC", nativeQuery = true)
    List<User> findRecentUsers(@Param("limit") int limit);
}
