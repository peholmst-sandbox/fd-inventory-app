package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.jooq.enums.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for setting up security context in tests.
 * Provides methods to authenticate as different user types with proper
 * FirestockUserDetails including station assignments.
 */
public final class TestSecurityUtils {

    private TestSecurityUtils() {
        // Utility class
    }

    /**
     * Authenticates the security context as a firefighter with the given station assignments.
     * At least one station must be provided.
     *
     * @param stations the stations the firefighter is assigned to
     */
    public static void authenticateAsFirefighter(StationId... stations) {
        if (stations == null || stations.length == 0) {
            throw new IllegalArgumentException("At least one station must be provided");
        }

        Set<StationId> stationIds = new HashSet<>(Arrays.asList(stations));
        StationId primaryStation = stations[0];

        FirestockUserDetails userDetails = new FirestockUserDetails(
            UserId.generate(),
            new EmailAddress("test-firefighter@firestock.local"),
            "password",
            "Test",
            "Firefighter",
            UserRole.FIREFIGHTER,
            true,
            stationIds,
            primaryStation
        );

        setAuthentication(userDetails);
    }

    /**
     * Authenticates the security context as a maintenance technician.
     * Maintenance technicians have cross-station access.
     */
    public static void authenticateAsMaintenance() {
        FirestockUserDetails userDetails = new FirestockUserDetails(
            UserId.generate(),
            new EmailAddress("test-technician@firestock.local"),
            "password",
            "Test",
            "Technician",
            UserRole.MAINTENANCE_TECHNICIAN,
            true
        );

        setAuthentication(userDetails);
    }

    /**
     * Authenticates the security context as a system administrator.
     * System administrators have cross-station access.
     */
    public static void authenticateAsAdmin() {
        FirestockUserDetails userDetails = new FirestockUserDetails(
            UserId.generate(),
            new EmailAddress("test-admin@firestock.local"),
            "password",
            "Test",
            "Administrator",
            UserRole.SYSTEM_ADMINISTRATOR,
            true
        );

        setAuthentication(userDetails);
    }

    /**
     * Authenticates the security context with a specific FirestockUserDetails.
     *
     * @param userDetails the user details to authenticate as
     */
    public static void authenticateAs(FirestockUserDetails userDetails) {
        setAuthentication(userDetails);
    }

    /**
     * Clears the security context, removing any authentication.
     * Should be called in @AfterEach methods.
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Returns the currently authenticated FirestockUserDetails, or null if not authenticated.
     */
    public static FirestockUserDetails getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FirestockUserDetails) {
            return (FirestockUserDetails) auth.getPrincipal();
        }
        return null;
    }

    private static void setAuthentication(FirestockUserDetails userDetails) {
        var authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            userDetails.getPassword(),
            userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
