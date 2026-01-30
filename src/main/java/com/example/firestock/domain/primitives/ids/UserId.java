package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a user.
 *
 * <p>A user is an authenticated individual who interacts with FireStock. Users are
 * assigned roles that determine their permissions (firefighter, maintenance technician,
 * or system administrator) and may be associated with one or more stations.
 *
 * @param value the UUID value, must not be null
 */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "User ID cannot be null");
    }

    /**
     * Generates a new user ID with a random UUID.
     *
     * @return a new user ID
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Creates a user ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a user ID wrapping the given UUID
     */
    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
