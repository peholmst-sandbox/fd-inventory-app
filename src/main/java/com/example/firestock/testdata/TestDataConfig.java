package com.example.firestock.testdata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable test data properties.
 */
@Configuration
@EnableConfigurationProperties(TestDataProperties.class)
class TestDataConfig {
}
