package com.example.isp392.service;

import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRepository;
import com.example.isp392.repository.UserRoleRepository;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
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
import org.springframework.data.jpa.domain.Specification;

@Service
public class UserService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        Collection<SimpleGrantedAuthority> authorities = userRoles.stream()
                .filter(UserRole::isRoleActiveForUser)
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getRoleName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    @Transactional
    public User registerBuyer(UserRegistrationDTO registrationDTO) throws ParseException {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFullName(registrationDTO.getFullName());
        user.setPhone(registrationDTO.getPhoneNumber());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        user.setDateOfBirth(dateFormat.parse(registrationDTO.getDateOfBirth()));

        String genderStr = registrationDTO.getGender().toLowerCase();
        if ("male".equals(genderStr)) {
            user.setGender(0);
        } else if ("female".equals(genderStr)) {
            user.setGender(1);
        } else {
            user.setGender(2);
        }

        User savedUser = userRepository.save(user);

        Role buyerRole = roleRepository.findByRoleName("BUYER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("BUYER");
                    return roleRepository.save(newRole);
                });

        UserRole userRole = new UserRole(savedUser, buyerRole);
        userRoleRepository.save(userRole);

        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByEmailDirectly(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(User user) {
        return userRoleRepository.findByUser(user).stream()
                .filter(UserRole::isRoleActiveForUser)
                .map(userRole -> "ROLE_" + userRole.getRole().getRoleName())
                .collect(Collectors.toList());
    }

    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, String dateOfBirth, MultipartFile profilePictureFile, HttpServletRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, Date dateOfBirth) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);

        return userRepository.save(user);
    }

    @Transactional
    public User saveUser(User user) {
        if (user.getGender() < 0 || user.getGender() > 2) {
            user.setGender(2);
        }

        if (user.getDateOfBirth() == null) {
            user.setDateOfBirth(new Date());
        }

        if (user.getFullName() == null) {
            user.setFullName("Google User");
        }

        return userRepository.save(user);
    }

    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Transactional
    public User updateUserInfo(String email, String fullName, String phone, int gender, Date dateOfBirth, String profilePicUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);

        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            user.setProfilePicUrl(profilePicUrl);
        }

        return userRepository.save(user);
    }

    @Transactional
    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;
    }

    @Transactional
    public User changeUserPassword(User user, String newPassword) {
        if (user.isOAuth2User()) {
            throw new RuntimeException("Cannot change password for OAuth2 user");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public boolean checkIfValidOldPassword(User user, String password) {
        if (user.isOAuth2User()) {
            return false;
        }

        return passwordEncoder.matches(password, user.getPassword());
    }

    public void registerNewUser(UserRegistrationDTO userRegistrationDTO, String seller) {
    }

    @Transactional
    public void deleteUserById(Integer userId) {
        log.info("UserService: Attempting to delete user with ID: {}", userId);
        if (userId == null) {
            log.error("UserService: userId is null, cannot delete.");
            return;
        }

        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                log.info("UserService: User found with ID: {}. Proceeding with deletion.", userId);
                userRepository.deleteById(userId);
                log.info("UserService: Call to userRepository.deleteById({}) completed.", userId);
            } else {
                log.warn("UserService: User with ID {} not found in repository. Cannot delete.", userId);
            }
        } catch (DataIntegrityViolationException dive) {
            log.error("UserService: DataIntegrityViolationException while deleting user {}: {}", userId, dive.getMessage(), dive);
        } catch (Exception e) {
            log.error("UserService: Unexpected error while deleting user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // <<< SỬA LỖI & CẬP NHẬT: THAY THẾ TOÀN BỘ PHƯƠNG THỨC NÀY >>>
    @Transactional(readOnly = true)
    public java.util.List<User> searchUsers(String keyword, String role) {

        // Cú pháp mới cho Specification (phù hợp với Spring Boot 3)
        Specification<User> spec = (root, query, criteriaBuilder) -> {

            // Tạo một danh sách để chứa tất cả các điều kiện (Predicate)
            List<Predicate> predicates = new ArrayList<>();

            // 1. Thêm điều kiện tìm kiếm theo keyword (tên hoặc email) nếu keyword tồn tại
            if (keyword != null && !keyword.trim().isEmpty()) {
                String keywordPattern = "%" + keyword.toLowerCase().trim() + "%";

                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordPattern)
                );
                predicates.add(keywordPredicate);
            }

            // 2. Thêm điều kiện lọc theo role nếu role tồn tại
            if (role != null && !role.trim().isEmpty()) {
                // Join từ User -> UserRole -> Role để lấy roleName
                // Sử dụng Join từ 'jakarta.persistence.criteria.Join'
                Join<User, com.example.isp392.model.UserRole> userRoleJoin = root.join("userRoles");
                Join<com.example.isp392.model.UserRole, com.example.isp392.model.Role> roleJoin = userRoleJoin.join("role");

                Predicate rolePredicate = criteriaBuilder.equal(roleJoin.get("roleName"), role.trim());
                predicates.add(rolePredicate);
            }

            // Kết hợp tất cả các điều kiện lại với nhau bằng phép AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Thực thi truy vấn với Specification đã được xây dựng
        return userRepository.findAll(spec);
    }
    @Transactional(readOnly = true)
    public User findUserById(Integer userId) {
        // Gọi phương thức findByIdWithAddresses để lấy cả địa chỉ
        return userRepository.findByIdWithAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }
    
}