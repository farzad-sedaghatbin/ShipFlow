package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.event.annotation.BeforeTestMethod;

/**
 * Shared test configuration for common test data setup across all integration tests.
 * Provides standardized test users and mock bean configurations.
 */
@TestConfiguration
@Profile("test")
public class TestDataConfiguration {

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates or retrieves the admin test user used across integration tests.
     * This ensures consistent user data for @WithMockUser tests.
     */
    @BeforeTestMethod
    public void setupTestUsers() {
        userRepository.findByUsername("admin").orElseGet(() -> 
            userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .password("testpassword")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build()));
    }
}