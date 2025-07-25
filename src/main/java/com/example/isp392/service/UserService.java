package com.example.isp392.service;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.repository.UserRoleRepository;
import com.example.isp392.repository.OrderRepository;

// No imports needed for annotations since we use constructor injection
import java.security.SecureRandom;
import java.util.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final WalletService walletService;

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
                       PasswordEncoder passwordEncoder,
                       OrderRepository orderRepository,
                       WalletService walletService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
        this.walletService = walletService;
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
                user.isActive(), // <-- THÊM THAM SỐ NÀY
                true,           // accountNonExpired
                true,           // credentialsNonExpired
                true,           // accountNonLocked
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
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        user.setDateOfBirth(LocalDate.parse(registrationDTO.getDateOfBirth(), dateFormatter));

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

        // Create wallet for the new user
        try {
            walletService.createWalletForUser(savedUser);
            log.info("Created wallet for new user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to create wallet for user {}: {}", savedUser.getEmail(), e.getMessage(), e);
            // Don't fail registration if wallet creation fails, but log the error
        }

        return savedUser;
    }

    /**
     * Ensure user has a wallet (create if doesn't exist)
     * @param user the user
     */
    @Transactional
    public void ensureUserHasWallet(User user) {
        try {
            walletService.ensureWalletExists(user);
        } catch (Exception e) {
            log.error("Failed to ensure wallet exists for user {}: {}", user.getEmail(), e.getMessage(), e);
        }
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
     * Retrieve a User object by username (email).
     * @param username the email of the user.
     * @return User object if found, null otherwise.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByEmail(username).orElse(null);
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
    public User updateUserInfo(String email, String fullName, String phone, int gender, LocalDate dateOfBirth) {
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

    @Transactional
    public void demoteUserFromSeller(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Role sellerRole = roleRepository.findByRoleName("SELLER")
                .orElseThrow(() -> new IllegalStateException("SELLER role not found in the database."));

        // Tìm và xóa bản ghi UserRole tương ứng
        user.getUserRoles().removeIf(userRole -> userRole.getRole().equals(sellerRole));

        userRepository.save(user);
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
            user.setDateOfBirth(LocalDate.now());
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
    public User updateUserInfo(String email, String fullName, String phone, int gender, LocalDate dateOfBirth, String profilePicUrl) {
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
    private Specification<User> createSpecification(String keyword, String role, String status) { // <-- Thêm 'String status'
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Predicate for keyword search
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern)
                );
                predicates.add(keywordPredicate);
            }

            // Predicate for role filtering
            if (role != null && !role.trim().isEmpty()) {
                Predicate rolePredicate = criteriaBuilder.equal(
                        root.join("userRoles").join("role").get("roleName"),
                        role.toUpperCase()
                );
                predicates.add(rolePredicate);
            }
            if (status != null && !status.trim().isEmpty()) {
                // Chuyển chuỗi "active" thành boolean true, và các chuỗi khác thành false
                boolean isActive = "active".equalsIgnoreCase(status);
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }
            // To prevent duplicates when joining
            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Page<User> searchUsers(String keyword, String role, String status, Pageable pageable) { // <-- Thêm 'String status'
        Specification<User> spec = createSpecification(keyword, role, status); // <-- Truyền status vào đây
        return userRepository.findAll(spec, pageable);
    }

    public User findUserById(Integer userId) {
        return userRepository.findByIdWithAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public Optional<User> findById(Integer userId) {
        return userRepository.findById(userId);
    }
    public List<Role> getAllRoles() {
        // Lấy tất cả trừ role "ADMIN" để tránh việc gán nhầm
        return roleRepository.findAll().stream()
                .filter(role -> !role.getRoleName().equals("ADMIN"))
                .collect(Collectors.toList());
    }
    @Transactional
    public void updateUserRoles(Integer userId, List<String> newRoleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Nếu newRoleNames là null (do không checkbox nào được chọn), coi như là danh sách rỗng
        if (newRoleNames == null) {
            newRoleNames = new ArrayList<>();
        }

        // Lấy các vai trò hiện tại của người dùng
        Set<UserRole> currentUserRoles = user.getUserRoles();

        // Tạo một map để dễ dàng xóa
        Map<String, UserRole> currentRoleMap = currentUserRoles.stream()
                .collect(Collectors.toMap(ur -> ur.getRole().getRoleName(), ur -> ur));

        // Xóa các vai trò không còn được chọn
        List<UserRole> rolesToRemove = new ArrayList<>();
        for (UserRole userRole : currentUserRoles) {
            if (!newRoleNames.contains(userRole.getRole().getRoleName())) {
                rolesToRemove.add(userRole);
            }
        }
        currentUserRoles.removeAll(rolesToRemove);
        userRoleRepository.deleteAll(rolesToRemove); // Xóa khỏi bảng join

        // Thêm các vai trò mới
        for (String roleName : newRoleNames) {
            if (!currentRoleMap.containsKey(roleName)) {
                Role role = roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                UserRole newUserRole = new UserRole(user, role);
                currentUserRoles.add(newUserRole);
            }
        }

        userRepository.save(user);
    }
    @Transactional
    public void updateUserActivationStatus(Integer userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setActive(isActive);
        userRepository.save(user);
        log.info("Activation status for user ID {} has been updated to {}.", userId, isActive);
    }

    @Transactional
    public void upgradeUserToSeller(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Role sellerRole = roleRepository.findByRoleName("SELLER")
                .orElseThrow(() -> new RuntimeException("Error: SELLER role not found."));

        boolean hasSellerRole = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().equals(sellerRole));

        if (!hasSellerRole) {
            UserRole newUserRole = new UserRole(user, sellerRole);
            user.getUserRoles().add(newUserRole);
            userRepository.save(user);
            log.info("Upgraded user with ID {} to SELLER.", userId);
        } else {
            // Ghi log nếu người dùng đã là SELLER để tránh xử lý thừa
            log.warn("User with ID {} is already a SELLER.", userId);
        }

    }

    public long countAllUsers() {
        return userRepository.count();
    }

    @Transactional
    public void deactivateUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Dòng này sẽ set is_active = false
        user.setActive(false);

        userRepository.save(user);
        log.info("User account for {} has been deactivated.", email);
    }

    @Transactional
    public void deleteUserById(Integer userId) {
        // Kiểm tra xem user có tồn tại không trước khi xóa
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Successfully deleted user with ID: {}", userId);
    }

    /**
     * Check if a user has any active orders.
     * For now, 'active' means PENDING, PROCESSING, or SHIPPED.
     * @param userId the ID of the user to check
     * @return true if the user has active orders, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveOrders(Integer userId) {
        long activeOrderCount = orderRepository.countActiveOrdersByUserId(userId);
        return activeOrderCount > 0;
    }

    /**
     * Performs a soft delete on a user account.
     * Sets the user's is_active status to false.
     * @param userId the ID of the user to deactivate
     * @throws RuntimeException if the user is not found
     */
    @Transactional
    public void softDeleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User with ID {} has been soft-deleted (deactivated).", userId);
    }

}
