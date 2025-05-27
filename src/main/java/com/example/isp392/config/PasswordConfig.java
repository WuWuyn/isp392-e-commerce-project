package com.example.isp392.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding
 * Separating this from SecurityConfig to avoid circular dependencies
 */
@Configuration
public class PasswordConfig {

    /**
     * Password encoder bean for encoding passwords
     * This is used by both UserService and SecurityConfig
     * 
     * @return BCryptPasswordEncoder for secure password storage
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
