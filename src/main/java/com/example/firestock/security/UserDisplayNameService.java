package com.example.firestock.security;

import com.example.firestock.domain.primitives.ids.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Service for looking up user display names.
 * This service provides a public API for other packages to resolve user IDs to display names.
 */
@Service
public class UserDisplayNameService {

    private final UserQuery userQuery;

    public UserDisplayNameService(UserQuery userQuery) {
        this.userQuery = userQuery;
    }

    /**
     * Gets the display name for a single user.
     * Display name is the full name (first + last name).
     *
     * @param userId the user ID to look up
     * @return the display name, or empty if user not found
     */
    @Transactional(readOnly = true)
    public Optional<String> getDisplayName(UserId userId) {
        return userQuery.getDisplayName(userId);
    }

    /**
     * Gets display names for multiple users (batch lookup).
     * Display name is the full name (first + last name).
     * This is more efficient than calling getDisplayName() in a loop.
     *
     * @param userIds the user IDs to look up
     * @return a map of UserId to display name (users not found are omitted)
     */
    @Transactional(readOnly = true)
    public Map<UserId, String> getDisplayNames(Collection<UserId> userIds) {
        return userQuery.getDisplayNames(userIds);
    }
}
