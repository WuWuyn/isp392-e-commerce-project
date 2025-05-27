package com.example.isp392.service;

import com.example.isp392.model.Role;
import com.example.isp392.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Initialize default roles when the application starts
     */
    @PostConstruct
    public void initRoles() {
        // Define default roles
        List<String> defaultRoles = Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_SELLER");
        
        for (String roleName : defaultRoles) {
            createRoleIfNotExists(roleName);
        }
    }
    
    /**
     * Create a role if it doesn't already exist in the database
     * @param roleName the name of the role to create
     * @return the created or existing role
     */
    public Role createRoleIfNotExists(String roleName) {
        Optional<Role> existingRole = roleRepository.findByRoleName(roleName);
        
        if (existingRole.isPresent()) {
            return existingRole.get();
        }
        
        Role newRole = new Role();
        newRole.setRoleName(roleName);
        return roleRepository.save(newRole);
    }
    
    /**
     * Get all roles from the database
     * @return a list of all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
    /**
     * Find a role by name
     * @param roleName the name of the role to find
     * @return an Optional containing the role if found
     */
    public Optional<Role> findRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }
}
