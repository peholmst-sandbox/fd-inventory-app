package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an issue.
 *
 * <p>An issue is a reported problem with equipment or consumables that requires
 * attention. Issues track the lifecycle of problems from initial report through
 * resolution, enabling visibility into equipment health and maintenance needs.
 *
 * @param value the UUID value, must not be null
 */
public record IssueId(UUID value) {
    public IssueId {
        Objects.requireNonNull(value, "Issue ID cannot be null");
    }

    /**
     * Generates a new issue ID with a random UUID.
     *
     * @return a new issue ID
     */
    public static IssueId generate() {
        return new IssueId(UUID.randomUUID());
    }

    /**
     * Creates an issue ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an issue ID wrapping the given UUID
     */
    public static IssueId of(UUID value) {
        return new IssueId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
