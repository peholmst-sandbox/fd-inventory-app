package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.jooq.enums.UserRole;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.STATION;
import static com.example.firestock.jooq.Tables.USER_STATION_ASSIGNMENT;

/**
 * Creates test users on application startup when enabled via configuration.
 * Only runs when firestock.security.create-test-users=true.
 */
@Component
@Order(1)
class TestUserInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestUserInitializer.class);

    private final SecurityProperties securityProperties;
    private final UserQuery userQuery;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final DSLContext create;

    TestUserInitializer(SecurityProperties securityProperties, UserQuery userQuery,
                                UserDao userDao, PasswordEncoder passwordEncoder, DSLContext create) {
        this.securityProperties = securityProperties;
        this.userQuery = userQuery;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.create = create;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isCreateTestUsers()) {
            return;
        }

        LOG.warn("Creating test users - this should only be enabled in development!");

        // Create or find test station for firefighter assignment
        StationId testStationId = getOrCreateTestStation();

        UserId firefighterId = createTestUserIfNotExists(
            "firefighter@firestock.local",
            "firefighter123",
            "Test",
            "Firefighter",
            UserRole.FIREFIGHTER
        );

        // Assign firefighter to test station (maintenance and admin have cross-station access)
        if (firefighterId != null) {
            assignUserToStation(firefighterId, testStationId);
        }

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

    /**
     * Creates a test user if not exists and returns the user ID.
     * Returns null if the user already exists.
     */
    private UserId createTestUserIfNotExists(String email, String password,
                                              String firstName, String lastName,
                                              UserRole role) {
        EmailAddress emailAddress = new EmailAddress(email);

        if (userQuery.existsByEmail(emailAddress)) {
            LOG.info("Test user {} already exists, skipping creation", email);
            return null;
        }

        String passwordHash = passwordEncoder.encode(password);
        UserId userId = userDao.insert(emailAddress, passwordHash, firstName, lastName, null, role);
        LOG.info("Created test user: {} with role {}", email, role);
        return userId;
    }

    /**
     * Gets or creates a test station for development.
     */
    private StationId getOrCreateTestStation() {
        StationCode testCode = new StationCode("TEST01");

        // Check if test station exists
        var existing = create.select(STATION.ID)
            .from(STATION)
            .where(STATION.CODE.eq(testCode))
            .fetchOptional(STATION.ID);

        if (existing.isPresent()) {
            LOG.info("Test station {} already exists", testCode);
            return existing.get();
        }

        // Create test station
        StationId stationId = StationId.generate();
        create.insertInto(STATION)
            .set(STATION.ID, stationId)
            .set(STATION.CODE, testCode)
            .set(STATION.NAME, "Test Station 01")
            .set(STATION.IS_ACTIVE, true)
            .execute();

        LOG.info("Created test station: {} ({})", "Test Station 01", testCode);
        return stationId;
    }

    /**
     * Assigns a user to a station if not already assigned.
     */
    private void assignUserToStation(UserId userId, StationId stationId) {
        // Check if assignment already exists
        boolean exists = create.fetchExists(
            create.selectOne()
                .from(USER_STATION_ASSIGNMENT)
                .where(USER_STATION_ASSIGNMENT.USER_ID.eq(userId))
                .and(USER_STATION_ASSIGNMENT.STATION_ID.eq(stationId))
        );

        if (exists) {
            LOG.info("User {} already assigned to station {}", userId, stationId);
            return;
        }

        create.insertInto(USER_STATION_ASSIGNMENT)
            .set(USER_STATION_ASSIGNMENT.USER_ID, userId)
            .set(USER_STATION_ASSIGNMENT.STATION_ID, stationId)
            .set(USER_STATION_ASSIGNMENT.IS_PRIMARY, true)
            .execute();

        LOG.info("Assigned user {} to station {} as primary", userId, stationId);
    }
}
