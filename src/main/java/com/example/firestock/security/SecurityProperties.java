package com.example.firestock.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FireStock security settings.
 */
@ConfigurationProperties(prefix = "firestock.security")
public class SecurityProperties {

    /**
     * Whether to create test users on application startup.
     * Should only be enabled in development environments.
     */
    private boolean createTestUsers = false;

    public boolean isCreateTestUsers() {
        return createTestUsers;
    }

    public void setCreateTestUsers(boolean createTestUsers) {
        this.createTestUsers = createTestUsers;
    }
}
