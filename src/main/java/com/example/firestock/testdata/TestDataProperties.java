package com.example.firestock.testdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FireStock test data.
 */
@ConfigurationProperties(prefix = "firestock.testdata")
public class TestDataProperties {

    /**
     * Whether to create test data on application startup.
     * Should only be enabled in development environments.
     */
    private boolean createTestData = false;

    public boolean isCreateTestData() {
        return createTestData;
    }

    public void setCreateTestData(boolean createTestData) {
        this.createTestData = createTestData;
    }
}
