package com.example.isp392.config;

import com.example.isp392.model.Role;
import com.example.isp392.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data initializer to ensure essential data exists in the database on startup
 * This runs after Hibernate creates/updates the schema but before the application is fully available
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    /**
     * Constructor with explicit dependency injection
     * 
     * @param roleRepository Repository for role data access
     */
    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Initialize essential data when the application starts
     * 
     * @param args Command line arguments (not used)
     * @throws Exception if an error occurs
     */
    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        createRoleIfNotExists("BUYER", 1);
        createRoleIfNotExists("SELLER", 2);
        createRoleIfNotExists("ADMIN", 3);
        
        System.out.println("Data initialization completed!");
    }
    
    /**
     * Create a role if it doesn't exist yet
     * 
     * @param roleName Name of the role to create
     * @param roleId ID to assign to the role
     */
    private void createRoleIfNotExists(String roleName, int roleId) {
        if (roleRepository.findByRoleName(roleName).isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleName);
            roleRepository.save(role);
            System.out.println("Created role: " + roleName);
        } else {
            System.out.println("Role already exists: " + roleName);
        }
    }
}
