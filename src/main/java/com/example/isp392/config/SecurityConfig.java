package com.example.isp392.config;

import com.example.isp392.service.UserService;
// Constructor injection is used, so @Autowired annotation is not needed
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// BCryptPasswordEncoder is now in PasswordConfig
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// AntPathRequestMatcher is deprecated, using logoutUrl() instead

/**
 * Security configuration for Spring Security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor with explicit dependency injection
     * 
     * @param userService User service for authentication
     * @param passwordEncoder Password encoder from PasswordConfig
     */
    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // PasswordEncoder bean is now in PasswordConfig to avoid circular dependencies

    /**
     * Authentication provider bean
     * Creating and configuring DaoAuthenticationProvider with our custom UserDetailsService
     * 
     * @return DaoAuthenticationProvider properly configured with our user service and password encoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Create DaoAuthenticationProvider manually without using deprecated methods
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        
        // Configure the provider with our password encoder and user details service
        // Note: Using the available API for this version of Spring Security
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userService);
        
        return provider;
    }

    /**
     * Authentication manager bean
     * @param authConfig Authentication configuration
     * @return AuthenticationManager
     * @throws Exception if an error occurs
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security filter chain configuration
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public resources accessible to all
                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/about-contact",
                    "/all-category",
                    "/blog",
                    "/blog-single",
                    "/terms-policy",
                    "/product-detail",
                    "/buyer/signup",
                    "/buyer/login",
                    "/buyer/register",
                    "/error"
                ).permitAll()
                
                // Buyer specific pages
                .requestMatchers("/buyer/**").hasRole("BUYER")
                
                // Admin specific pages
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Seller specific pages
                .requestMatchers("/seller/**").hasRole("SELLER")
                
                // Any other request needs authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Buyer login configuration
                .loginPage("/buyer/login")
                .loginProcessingUrl("/buyer/login")
                .defaultSuccessUrl("/buyer/account-info", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout") // Use logoutUrl instead of logoutRequestMatcher
                .logoutSuccessUrl("/buyer/login?logout")
                .permitAll()
            )
            // Disable CSRF for simplicity in this demo project
            // In a production environment, CSRF protection should be enabled
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }
}
