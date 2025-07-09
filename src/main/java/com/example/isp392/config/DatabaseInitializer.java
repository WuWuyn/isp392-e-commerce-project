package com.example.isp392.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    // Removed normalizedTitle initialization as we're now using fuzzy search
    // and no longer need the normalized title field
} 