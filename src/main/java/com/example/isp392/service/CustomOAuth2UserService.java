package com.example.isp392.service;

import com.example.isp392.model.Role;
import com.example.isp392.model.User;
import com.example.isp392.model.UserRole;
import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to handle OAuth2 user authentication
 * Customized to use email as the principal name instead of the Google ID
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        try {
            // Get user info from OAuth2 provider
            Map<String, Object> attributes = oAuth2User.getAttributes();
            log.debug("OAuth2 user attributes: {}", attributes);
            
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String pictureUrl = oAuth2User.getAttribute("picture");
            String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        log.info("Processing OAuth2 user: email={}, name={}, provider={}", email, name, provider);
        
        if (email == null) {
            log.warn("Email is null for OAuth2 user. Cannot proceed with authentication.");
            throw new OAuth2AuthenticationException("Email not available from OAuth2 provider");
        }
        
        // Check if user already exists in our database
        User existingUser = null;
        try {
            existingUser = userService.findByEmailDirectly(email);
            log.debug("User lookup result for {}: {}", email, existingUser != null ? "found" : "not found");
        } catch (Exception e) {
            log.error("Error finding user by email {}: {}", email, e.getMessage(), e);
            throw new OAuth2AuthenticationException("Error finding user in database: " + e.getMessage());
        }
        
        if (existingUser != null) {
            log.info("User with email {} already exists in database", email);
            
            // Only update profile picture, not name (to preserve manual edits)
            boolean changed = false;
            
            // No longer updating name on every login to preserve user edits
            // Only set name for brand new users (handled in the else block below)
            
            // Update profile picture if provided and different
            if (pictureUrl != null && !pictureUrl.equals(existingUser.getProfilePicUrl())) {
                existingUser.setProfilePicUrl(pictureUrl);
                changed = true;
                log.info("Updated profile picture for user {}", email);
            }
            
            // Save user if any changes were made
            if (changed) {
                userService.saveUser(existingUser);
                log.info("Saved updated OAuth2 user information for {}", email);
            }
        } else {
            log.info("Creating new user with email {}", email);
            // Register new user
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name != null ? name : "Google User"); // Ensure name is never null
            
            // Set profile picture if available from Google
            if (pictureUrl != null) {
                newUser.setProfilePicUrl(pictureUrl);
                log.info("Set profile picture from Google for new user");
            }
            
            // Set default values required by the database
            newUser.setPhone("Not provided"); // Default phone
            newUser.setDateOfBirth(LocalDate.now()); // Default date of birth
            newUser.setGender(2); // Default gender as Other
            
            // For OAuth2 users, we don't have a password, so we'll generate a random one
            // They'll never use it for login as they'll use Google authentication
            String randomPassword = userService.generateRandomPassword();
            newUser.setPassword(userService.encodePassword(randomPassword));
            
            // Explicitly set all required fields to ensure no nulls for primitive fields
            
            User savedUser = null;
            try {
                savedUser = userService.saveUser(newUser);
                log.info("New user created with ID: {}", savedUser.getUserId());
                
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
                log.info("Assigned BUYER role to new user: {}", email);
            } catch (Exception e) {
                log.error("Error saving new OAuth2 user: {}", e.getMessage(), e);
                throw new OAuth2AuthenticationException("Error creating new user: " + e.getMessage());
            }
        }
        
        // Find user in database to get their roles for authorities
        User dbUser = userService.findByEmailDirectly(email);
        if (dbUser == null) {
            log.error("Could not find user in database after processing: {}", email);
            throw new OAuth2AuthenticationException("User not found in database after OAuth2 processing");
        }
        
        // Get user roles from database and convert to Spring Security authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // Add roles as authorities
        List<String> roleNames = userService.getUserRoles(dbUser);
        for (String roleName : roleNames) {
            log.info("Adding authority for OAuth2 user: {}", roleName);
            authorities.add(new SimpleGrantedAuthority(roleName));
        }
        
        // Create a new OAuth2User with email as the name attribute
        // Make a copy of the attributes to ensure we don't modify the original
        Map<String, Object> enhancedAttributes = new LinkedHashMap<>(oAuth2User.getAttributes());
        
        // Add database user ID to attributes for reference
        enhancedAttributes.put("userId", dbUser.getUserId());
        enhancedAttributes.put("databaseFullName", dbUser.getFullName());
        
        String nameAttributeKey = "email";
        
        log.info("Creating CustomOAuth2User with nameAttributeKey={} and {} authorities", nameAttributeKey, authorities.size());
        return new DefaultOAuth2User(authorities, enhancedAttributes, nameAttributeKey);
        } catch (Exception e) {
            log.error("Error in processOAuth2User: {}", e.getMessage(), e);
            throw e;
        }
    }
}
