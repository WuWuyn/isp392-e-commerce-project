package com.example.isp392.config;

import com.example.isp392.model.User;
import com.example.isp392.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom success handler for OAuth2 login
 * Ensures proper redirection and user session setup after Google login
 * Redirects users based on their role (BUYER or SELLER)
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    
    private final UserService userService;
    
    public OAuth2LoginSuccessHandler(UserService userService) {
        this.userService = userService;
        // Set default target URL to account info page
        setDefaultTargetUrl("/buyer/account-info");
    }
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
                                       
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String picture = oauth2User.getAttribute("picture");
            
            // Get database attributes directly from enhanced attributes if available
            String dbFullName = oauth2User.getAttribute("databaseFullName");
            
            log.info("OAuth2 login success for user: {} ({})", email, name);
            log.debug("OAuth2 user picture URL: {}", picture);
            
            // Get user from database
            Optional<User> userOptional = userService.findByEmail(email);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // Print user information to console for debugging
                log.info("OAuth2 logged in user: id={}, name={}, email={}, pictureUrl={}", 
                         user.getUserId(), user.getFullName(), user.getEmail(), 
                         user.getProfilePicUrl());
                
                // Store user ID in session for quicker retrieval
                request.getSession().setAttribute("USER_ID", user.getUserId());
     
                // Determine the target URL based on user role
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                String targetUrl = "/"; // Default
                
                // Check roles and set appropriate redirect URL
                for (GrantedAuthority authority : authorities) {
                    if (authority.getAuthority().equals("ROLE_SELLER")) {
                        targetUrl = "/seller/account";
                        log.info("OAuth2 login: User has SELLER role, redirecting to {}", targetUrl);
                        break;
                    } else if (authority.getAuthority().equals("ROLE_BUYER")) {
                        targetUrl = "/buyer/account-info";
                        log.info("OAuth2 login: User has BUYER role, redirecting to {}", targetUrl);
                        break;
                    }
                }
                
                // Set the target URL for this request
                setDefaultTargetUrl(targetUrl);
            } else {
                log.warn("OAuth2 user not found in database after successful authentication: {}", email);
            }
            
            // Set session attributes for OAuth2 login
            request.getSession().setAttribute("OAUTH2_USER", true);
            request.getSession().setAttribute("USER_EMAIL", email);
            request.getSession().setAttribute("USER_NAME", dbFullName != null ? dbFullName : name);
            
            // Log additional OAuth2 attributes for debugging
            log.debug("OAuth2 attributes in session: OAUTH2_USER=true, USER_EMAIL={}, USER_NAME={}", 
                     email, dbFullName != null ? dbFullName : name);
        }
        
        // Use the parent class to handle the success redirect
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
