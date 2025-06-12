package com.example.isp392.repository;

import com.example.isp392.model.User;
import com.example.isp392.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserAddress entity
 * Provides methods for CRUD operations on user addresses
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {
    
    /**
     * Find all addresses for a specific user
     * @param userId the user ID
     * @return list of addresses belonging to the user
     */
    List<UserAddress> findByUserUserId(int userId);
    
    /**
     * Find a specific address by ID and user ID
     * @param addressId the address ID
     * @param userId the user ID
     * @return the address if found and belongs to the user
     */
    Optional<UserAddress> findByAddressIdAndUserUserId(int addressId, int userId);
    
    /**
     * Count the number of addresses for a user
     * @param userId the user ID
     * @return count of addresses for the user
     */
    long countByUserUserId(int userId);
    
    /**
     * Reset all default addresses for a user except the specified one
     * @param userId the user ID
     * @param exceptAddressId the address ID to exclude from reset
     */
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.userId = :userId AND a.addressId != :exceptAddressId")
    void resetDefaultAddresses(@Param("userId") int userId, @Param("exceptAddressId") int exceptAddressId);
    
    /**
     * Find the default address for a user
     * @param userId the user ID
     * @return the default address if exists
     */
    Optional<UserAddress> findByUserUserIdAndIsDefaultTrue(int userId);

    List<UserAddress> findByUser(User user);
    UserAddress findByUserAndIsDefaultTrue(User user);
}
