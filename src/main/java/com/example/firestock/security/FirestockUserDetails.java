package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.jooq.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

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

    public FirestockUserDetails(UserId userId, EmailAddress email, String passwordHash,
                                 String firstName, String lastName, UserRole userRole,
                                 boolean isActive) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userRole = userRole;
        this.isActive = isActive;
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
