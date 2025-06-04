package com.example.isp392.service;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.repository.UserRoleRepository;

// No imports needed for annotations since we use constructor injection
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
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
     * Find user by email and return the user object directly
     * @param email the email to search for
     * @return User if found, null otherwise
     */
    public User findByEmailDirectly(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Get user roles with the ROLE_ prefix for Spring Security
     * @param user the user
     * @return list of roles for the user with ROLE_ prefix
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(User user) {
        return userRoleRepository.findByUser(user).stream()
                .filter(UserRole::isRoleActiveForUser)
                .map(userRole -> "ROLE_" + userRole.getRole().getRoleName())
                .collect(Collectors.toList());
    }
    
    /**
     * Update user information
     *
     * @param email              the user's email
     * @param fullName           the user's full name
     * @param phone              the user's phone number
     * @param gender             the user's gender (0: Male, 1: Female, 2: Other)
     * @param dateOfBirth
     * @param profilePictureFile
     * @param request
     * @return the updated user
     * @throws RuntimeException if user not found
     */
    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, String dateOfBirth, MultipartFile profilePictureFile, HttpServletRequest request) {
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
     * Save a user to the database
     * @param user the user to save
     * @return the saved user
     */
    @Transactional
    public User saveUser(User user) {
        // Validate and set defaults for primitive fields to prevent null errors
        if (user.getGender() < 0 || user.getGender() > 2) {
            user.setGender(2); // Default to 'Other' if invalid
        }
        
        // Ensure all other required fields are not null
        if (user.getDateOfBirth() == null) {
            user.setDateOfBirth(new Date());
        }
        
        if (user.getFullName() == null) {
            user.setFullName("Google User");
        }
        
        return userRepository.save(user);
    }
    
    /**
     * Generate a random password for OAuth2 users
     * @return a random password string
     */
    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Encode a password using the password encoder
     * @param password the raw password
     * @return the encoded password
     */
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
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
    
    /**
     * Change user password directly (for password reset)
     * @param user the user to update
     * @param newPassword the new password (not encoded)
     * @return the updated user
     */
    @Transactional
    public User changeUserPassword(User user, String newPassword) {
        // Check if user is OAuth2 user
        if (user.isOAuth2User()) {
            throw new RuntimeException("Cannot change password for OAuth2 user");
        }
        
        // Encode and update the password
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }
    
    /**
     * Check if a given password matches the user's current password
     * @param user the user to check
     * @param password the password to check (not encoded)
     * @return true if the password matches the user's current password, false otherwise
     */
    public boolean checkIfValidOldPassword(User user, String password) {
        // Check if user is OAuth2 user (they don't have a password to check)
        if (user.isOAuth2User()) {
            return false;
        }
        
        // Use the password encoder to check if the given password matches the stored password
        return passwordEncoder.matches(password, user.getPassword());
    }

    public void registerNewUser(UserRegistrationDTO userRegistrationDTO, String seller) {
    }


    @Transactional
    public void deleteUserById(Integer userId) { // <--- THAM SỐ LÀ Integer
        log.info("UserService: Attempting to delete user with ID: {}", userId);
        if (userId == null) {
            log.error("UserService: userId is null, cannot delete.");
            // Có thể throw new IllegalArgumentException("User ID cannot be null");
            return;
        }

        try {
            // userRepository.findById(userId) sẽ mong đợi Integer và userId của bạn giờ là Integer
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                log.info("UserService: User found with ID: {}. Proceeding with deletion.", userId);

                // Xóa các bản ghi phụ thuộc nếu cần (và nếu chúng chưa được xử lý bằng Cascade)
                // Ví dụ: orderRepository.deleteByUserId(userId); // Giả sử có phương thức này

                userRepository.deleteById(userId); // Gọi với Integer userId

                log.info("UserService: Call to userRepository.deleteById({}) completed.", userId);

                if (userRepository.existsById(userId)) { // Gọi với Integer userId
                    log.error("UserService: CRITICAL - User with ID {} STILL EXISTS after deletion attempt!", userId);
                } else {
                    log.info("UserService: User with ID {} successfully confirmed as deleted from repository.", userId);
                }
            } else {
                log.warn("UserService: User with ID {} not found in repository. Cannot delete.", userId);
            }
        } catch (DataIntegrityViolationException dive) {
            log.error("UserService: DataIntegrityViolationException while deleting user {}: {}", userId, dive.getMessage(), dive);
            // throw dive; // Cân nhắc re-throw để controller có thể xử lý chi tiết hơn
        } catch (Exception e) {
            log.error("UserService: Unexpected error while deleting user {}: {}", userId, e.getMessage(), e);
            // throw new RuntimeException("Error deleting user", e); // Cân nhắc re-throw
        }
    }

    // ... các phương thức khác của UserService ...
    // Đảm bảo tất cả các phương thức khác trong UserService
    // nếu làm việc với userId cũng mong đợi hoặc trả về Integer nếu cần.
}

