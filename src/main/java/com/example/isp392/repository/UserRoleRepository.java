package com.example.isp392.repository;

import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
