package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.jooq.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * UserDetails implementation that wraps FireStock user information.
 * Maps the domain UserRole to Spring Security ROLE_* format.
 */
public class FirestockUserDetails implements UserDetails {

    private final UserId userId;
    private final EmailAddress email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final UserRole userRole;
    private final boolean isActive;
    private final Set<StationId> assignedStationIds;
    private final StationId primaryStationId;

    public FirestockUserDetails(UserId userId, EmailAddress email, String passwordHash,
                                 String firstName, String lastName, UserRole userRole,
                                 boolean isActive) {
        this(userId, email, passwordHash, firstName, lastName, userRole, isActive,
             Collections.emptySet(), null);
    }

    public FirestockUserDetails(UserId userId, EmailAddress email, String passwordHash,
                                 String firstName, String lastName, UserRole userRole,
                                 boolean isActive, Set<StationId> assignedStationIds,
                                 StationId primaryStationId) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userRole = userRole;
        this.isActive = isActive;
        this.assignedStationIds = assignedStationIds != null
            ? Collections.unmodifiableSet(assignedStationIds)
            : Collections.emptySet();
        this.primaryStationId = primaryStationId;
    }

    /**
     * Returns the user's domain ID.
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * Returns the user's domain role.
     */
    public UserRole getUserRole() {
        return userRole;
    }

    /**
     * Returns the user's full name for display purposes.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns the user's first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the user's last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the station IDs this user is assigned to.
     * For firefighters, this determines which stations they can access.
     * Maintenance technicians and administrators have cross-station access
     * regardless of assignments.
     */
    public Set<StationId> getAssignedStationIds() {
        return assignedStationIds;
    }

    /**
     * Returns the user's primary station, if any.
     * May be null if no primary station is designated.
     */
    public StationId getPrimaryStationId() {
        return primaryStationId;
    }

    /**
     * Checks if this user has access to the specified station.
     * Maintenance technicians and system administrators have cross-station access.
     * Firefighters can only access their assigned stations.
     *
     * @param stationId the station to check access for
     * @return true if the user can access the station
     */
    public boolean hasAccessToStation(StationId stationId) {
        if (stationId == null) {
            return false;
        }
        if (userRole == UserRole.MAINTENANCE_TECHNICIAN ||
            userRole == UserRole.SYSTEM_ADMINISTRATOR) {
            return true;
        }
        return assignedStationIds.contains(stationId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map UserRole to Spring Security ROLE_* format
        String roleName = "ROLE_" + userRole.getLiteral();
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email.value();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
