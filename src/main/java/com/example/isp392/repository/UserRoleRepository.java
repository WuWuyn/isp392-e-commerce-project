package com.example.isp392.repository;

import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRole entity
 * Provides methods for CRUD operations on user roles
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    /**
     * Find user roles by user
     * @param user the user entity
     * @return list of user roles for the specified user
     */
    List<UserRole> findByUser(User user);
    
    /**
     * Find user roles by role
     * @param role the role entity
     * @return list of user roles for the specified role
     */
    List<UserRole> findByRole(Role role);
    
    /**
     * Find user role by user and role
     * @param user the user entity
     * @param role the role entity
     * @return Optional containing the user role if found, empty otherwise
     */
    Optional<UserRole> findByUserAndRole(User user, Role role);
    
    /**
     * Find all roles for a specific user
     * @param userId the user ID
     * @return list of user roles assigned to the user
     */
    List<UserRole> findByUserUserId(Integer userId);
    
    /**
     * Find if a user has a specific role
     * @param userId the user ID
     * @param roleName the role name
     * @return true if the user has the role, false otherwise
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur JOIN ur.role r WHERE ur.user.userId = :userId AND r.roleName = :roleName AND ur.isRoleActiveForUser = true")
    boolean hasRole(@Param("userId") Integer userId, @Param("roleName") String roleName);
    
    /**
     * Count users with a specific role
     * @param roleName the role name
     * @param isActive whether to count only active roles
     * @return count of users with the specified role
     */
    @Query("SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur JOIN ur.role r WHERE r.roleName = :roleName AND ur.isRoleActiveForUser = :isActive")
    Long countByRoleNameAndActive(@Param("roleName") String roleName, @Param("isActive") boolean isActive);
}
