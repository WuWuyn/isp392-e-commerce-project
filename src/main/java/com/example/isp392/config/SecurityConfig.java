package com.example.isp392.config;

import com.example.isp392.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Spring Security
 * Implements separate authentication flows for admin, buyer, and seller roles
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

    /**
     * Authentication provider bean
     * Creating and configuring DaoAuthenticationProvider with our custom UserDetailsService
     *
     * @return DaoAuthenticationProvider properly configured with our user service and password encoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
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
     * Security filter chain for public resources
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicResourcesFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/css/**", "/js/**", "/images/**", "/webjars/**", "/error/**",
                        "/", "/about-contact", "/all-category", "/blog", "/blog-single",
                        "/terms-policy", "/product-detail", "/favicon.ico")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Security filter chain for admin authentication
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/process_login")
                        .defaultSuccessUrl("/admin/products")
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Security filter chain for buyer authentication
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(3)
    public SecurityFilterChain buyerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/buyer/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/buyer/signup",
                                "/buyer/login",
                                "/buyer/register"
                        ).permitAll()
                        .anyRequest().hasRole("BUYER")
                )
                .formLogin(form -> form
                        .loginPage("/buyer/login")
                        .loginProcessingUrl("/buyer/process_login")
                        .defaultSuccessUrl("/buyer/account-info")
                        .failureUrl("/buyer/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/buyer/logout")
                        .logoutSuccessUrl("/buyer/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Security filter chain for seller authentication
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(4)
    public SecurityFilterChain sellerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/seller/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/seller/login").permitAll()
                        .anyRequest().hasRole("SELLER")
                )
                .formLogin(form -> form
                        .loginPage("/seller/login")
                        .loginProcessingUrl("/seller/process_login")
                        .defaultSuccessUrl("/seller/dashboard")
                        .failureUrl("/seller/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/seller/logout")
                        .logoutSuccessUrl("/seller/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Default security filter chain to catch all other requests
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(5)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/buyer/login")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
