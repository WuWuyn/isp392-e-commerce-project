package com.example.isp392.service;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor with explicit dependency injection
     * This is preferred over field injection with @Autowired as it makes dependencies clear,
     * ensures they're required, and makes testing easier
     * 
     * @param userRepository Repository for user data access
     * @param roleRepository Repository for role data access
     * @param userRoleRepository Repository for user-role relationship data access
     * @param passwordEncoder Password encoder for securely storing passwords
     */
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
     * Load user by username (email) for Spring Security authentication
     * @param email the email as username
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Get user roles
        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        
        // Map roles to authorities
        Collection<SimpleGrantedAuthority> authorities = userRoles.stream()
                .filter(UserRole::isRoleActiveForUser)
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getRoleName()))
                .collect(Collectors.toList());

        // Return Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    /**
     * Register a new buyer user
     * @param registrationDTO the registration data
     * @return the created user
     */
    @Transactional
    public User registerBuyer(UserRegistrationDTO registrationDTO) throws ParseException {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword())); // Encode password
        user.setFullName(registrationDTO.getFullName());
        user.setPhone(registrationDTO.getPhoneNumber());
        
        // Parse and set date of birth
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        user.setDateOfBirth(dateFormat.parse(registrationDTO.getDateOfBirth()));
        
        // Set gender (0 - Male, 1 - Female, 2 - Other)
        String genderStr = registrationDTO.getGender().toLowerCase();
        if ("male".equals(genderStr)) {
            user.setGender(0);
        } else if ("female".equals(genderStr)) {
            user.setGender(1);
        } else {
            user.setGender(2); // Other
        }
        
        // Save user
        User savedUser = userRepository.save(user);

        // Find BUYER role (create if not exists)
        Role buyerRole = roleRepository.findByRoleName("BUYER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("BUYER");
                    return roleRepository.save(newRole);
                });

        // Assign BUYER role to user
        UserRole userRole = new UserRole(savedUser, buyerRole);
        userRoleRepository.save(userRole);

        return savedUser;
    }

    /**
     * Find user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get user roles
     * @param user the user
     * @return list of roles for the user
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(User user) {
        return userRoleRepository.findByUser(user).stream()
                .filter(UserRole::isRoleActiveForUser)
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.toList());
    }
    
    /**
     * Update user information
     * @param email the user's email
     * @param fullName the user's full name
     * @param phone the user's phone number
     * @param gender the user's gender (0: Male, 1: Female, 2: Other)
     * @return the updated user
     * @throws RuntimeException if user not found
     */
    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Update user information
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        
        // Save and return updated user
        return userRepository.save(user);
    }
    
    /**
     * Update user information including date of birth
     * @param email the user's email
     * @param fullName the user's full name
     * @param phone the user's phone number
     * @param gender the user's gender (0: Male, 1: Female, 2: Other)
     * @param dateOfBirth the user's date of birth
     * @return the updated user
     * @throws RuntimeException if user not found
     */
    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, Date dateOfBirth) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Update user information
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);
        
        // Save and return updated user
        return userRepository.save(user);
    }
    
    /**
     * Update user information including date of birth and profile picture
     * @param email the user's email
     * @param fullName the user's full name
     * @param phone the user's phone number
     * @param gender the user's gender (0: Male, 1: Female, 2: Other)
     * @param dateOfBirth the user's date of birth
     * @param profilePicUrl the URL of the user's profile picture (can be null)
     * @return the updated user
     * @throws RuntimeException if user not found
     */
    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, Date dateOfBirth, String profilePicUrl) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Update user information
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);
        
        // Update profile picture URL if provided
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            user.setProfilePicUrl(profilePicUrl);
        }
        
        // Save and return updated user
        return userRepository.save(user);
    }
    
    /**
     * Update user password
     * @param email the user's email
     * @param currentPassword the user's current password
     * @param newPassword the user's new password
     * @return true if password was updated successfully, false otherwise
     * @throws RuntimeException if user not found
     */
    @Transactional
    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false; // Current password is incorrect
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        return true;
    }
}
