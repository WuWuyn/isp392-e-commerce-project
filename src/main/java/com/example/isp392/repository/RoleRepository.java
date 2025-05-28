package com.example.isp392.repository;

import com.example.isp392.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    /**
     * Find a role by its name
     * @param roleName the name of the role
     * @return Optional containing the role if found, empty otherwise
     */
    Optional<Role> findByRoleName(String roleName);
}
