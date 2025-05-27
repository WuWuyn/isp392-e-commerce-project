package com.example.isp392.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

/**
 * Configuration for Thymeleaf with Spring Security integration
 * This enables the use of sec:authorize and other security tags in Thymeleaf templates
 */
@Configuration
public class ThymeleafConfig {
    
    /**
     * Configure Spring Security dialect for Thymeleaf
     * This allows using sec:authorize and other security-related tags in templates
     * 
     * @return SpringSecurityDialect bean
     */
    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
