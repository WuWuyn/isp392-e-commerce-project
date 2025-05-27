package com.example.isp392.service;

import com.example.isp392.dto.UserRegistrationDto;
import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, 
                      RoleRepository roleRepository, 
                      UserRoleRepository userRoleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user with a specified role
     * @param registrationDto the user registration data
     * @return the created User entity
     * @throws RuntimeException if the role doesn't exist or email is already taken
     */
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email is already registered: " + registrationDto.getEmail());
        }

        // Find the requested role
        Role role = roleRepository.findByRoleName(registrationDto.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + registrationDto.getRoleName()));

        // Create a new user
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFullName(registrationDto.getFullName());
        user.setPhone(registrationDto.getPhone());
        user.setDateOfBirth(registrationDto.getDateOfBirth());
        user.setGender(registrationDto.isGender());
        user.setProfilePicUrl(registrationDto.getProfilePicUrl());

        // Save the user to get the generated ID
        User savedUser = userRepository.save(user);

        // Create and save the user-role relationship
        UserRole userRole = new UserRole(savedUser, role);
        userRole.setRoleAssignmentDate(new Date());
        userRole.setRoleActive(true);
        userRoleRepository.save(userRole);

        // Add the role to the user
        savedUser.addUserRole(userRole);

        return savedUser;
    }

    /**
     * Find a user by email
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find a user by ID
     * @param id the user ID to search for
     * @return an Optional containing the user if found
     */
    public Optional<User> findUserById(Integer id) {
        return userRepository.findById(id);
    }
}
