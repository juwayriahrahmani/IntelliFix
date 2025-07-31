package com.loganalyzer.config;

import com.loganalyzer.service.AuthService;
import com.loganalyzer.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private LogService logService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing application data...");
        
        // Initialize default users
        authService.initializeDefaultUsers();
        logger.info("Default users initialized");
        
        // Initialize sample log data
        logService.initializeSampleData();
        logger.info("Sample log data initialized");
        
        logger.info("Application data initialization completed");
    }
}