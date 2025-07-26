package com.example.isp392.config;

import com.example.isp392.repository.RoleRepository;
import com.example.isp392.repository.UserRoleRepository;
import com.example.isp392.service.CustomOAuth2UserService;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security configuration for Spring Security
 * Implements separate authentication flows for admin, buyer, and seller roles
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    /**
     * Constructor with explicit dependency injection
     *
     * @param userService User service for authentication
     * @param passwordEncoder Password encoder from PasswordConfig
     */
    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;

        log.info("SecurityConfig initialized with OAuth2LoginSuccessHandler");
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
     * Bean for custom OAuth2 user service
     * @return CustomOAuth2UserService
     */
    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(userService, roleRepository, userRoleRepository);
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
                        "/", "/about-contact", "/all-category", "/blog/**", "/blog-single",
                        "/terms-policy", "/product-detail", "/favicon.ico",
                        "/password-reset/**", "/uploads/**")

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
                        .defaultSuccessUrl("/admin/dashboard")
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
                .securityMatcher("/buyer/**", "/login/oauth2/**", "/oauth2/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/buyer/signup",
                                "/buyer/login",
                                "/buyer/register",
                                "/login/oauth2/**",
                                "/oauth2/**"
                        ).permitAll()
                        .anyRequest().hasRole("BUYER")
                )
                .formLogin(form -> form
                        .loginPage("/buyer/login")
                        .loginProcessingUrl("/buyer/process_login")
                        .defaultSuccessUrl("/home", true) // Redirect to account-info page with true to always redirect
                        .failureUrl("/buyer/login?error")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("readhubSecretKey") // Secret key for token signature
                        .tokenValiditySeconds(2592000) // 30 days in seconds
                        .rememberMeParameter("remember-me") // Match the checkbox name in the form
                        .userDetailsService(userService) // Use our custom UserDetailsService
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/buyer/login")
                        .successHandler(oAuth2LoginSuccessHandler) // Use custom success handler
                        .failureUrl("/buyer/login?error=oauth2")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/buyer/logout")
                        .logoutSuccessUrl("/buyer/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
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
                        .requestMatchers(
                                "/seller/signup",
                                "/seller/login",
                                "/seller/register"
                        ).permitAll()
                        .anyRequest().hasRole("SELLER")
                )
                .formLogin(form -> form
                        .loginPage("/seller/login")
                        .loginProcessingUrl("/seller/process_login")
                        .defaultSuccessUrl("/seller/dashboard", true)  // Changed from /seller/account to /seller/dashboard
                        .failureUrl("/seller/login?error")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("readhubSellerSecretKey")
                        .tokenValiditySeconds(2592000)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userService)
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/seller/login")
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureUrl("/seller/login?error=oauth2")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/seller/logout")
                        .logoutSuccessUrl("/seller/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * API security filter chain for REST endpoints
     * Allows public access to chat API
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(4)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/chat/**").authenticated() // Require login for chat API
                        .requestMatchers("/api/location/**").permitAll() // Keep existing location API public
                        .requestMatchers("/api/debug/**").permitAll() // Debug endpoints for development
                        .requestMatchers("/api/test/**").permitAll() // Test endpoints for development
                        .anyRequest().authenticated() // All other API endpoints require authentication
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/chat/**") // Allow CSRF for chat API
                ); // Keep session-based auth for chat API

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