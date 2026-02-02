package com.example.firestock.security;

import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.jooq.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates test users on application startup when enabled via configuration.
 * Only runs when firestock.security.create-test-users=true.
 */
@Component
public class TestUserInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestUserInitializer.class);

    private final SecurityProperties securityProperties;
    private final UserQuery userQuery;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public TestUserInitializer(SecurityProperties securityProperties, UserQuery userQuery,
                                UserDao userDao, PasswordEncoder passwordEncoder) {
        this.securityProperties = securityProperties;
        this.userQuery = userQuery;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isCreateTestUsers()) {
            return;
        }

        LOG.warn("Creating test users - this should only be enabled in development!");

        createTestUserIfNotExists(
            "firefighter@firestock.local",
            "firefighter123",
            "Test",
            "Firefighter",
            UserRole.FIREFIGHTER
        );

        createTestUserIfNotExists(
            "technician@firestock.local",
            "technician123",
            "Test",
            "Technician",
            UserRole.MAINTENANCE_TECHNICIAN
        );

        createTestUserIfNotExists(
            "admin@firestock.local",
            "administrator123",
            "Test",
            "Administrator",
            UserRole.SYSTEM_ADMINISTRATOR
        );
    }

    private void createTestUserIfNotExists(String email, String password,
                                            String firstName, String lastName,
                                            UserRole role) {
        EmailAddress emailAddress = new EmailAddress(email);

        if (userQuery.existsByEmail(emailAddress)) {
            LOG.info("Test user {} already exists, skipping creation", email);
            return;
        }

        String passwordHash = passwordEncoder.encode(password);
        userDao.insert(emailAddress, passwordHash, firstName, lastName, null, role);
        LOG.info("Created test user: {} with role {}", email, role);
    }
}
