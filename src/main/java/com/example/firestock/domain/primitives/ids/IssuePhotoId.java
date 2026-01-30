package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an issue photo.
 *
 * <p>An issue photo is an image attached to an issue for documentation purposes,
 * such as showing damage to equipment. Photos help maintenance technicians
 * understand the nature and severity of reported problems.
 *
 * @param value the UUID value, must not be null
 */
public record IssuePhotoId(UUID value) {
    public IssuePhotoId {
        Objects.requireNonNull(value, "Issue photo ID cannot be null");
    }

    /**
     * Generates a new issue photo ID with a random UUID.
     *
     * @return a new issue photo ID
     */
    public static IssuePhotoId generate() {
        return new IssuePhotoId(UUID.randomUUID());
    }

    /**
     * Creates an issue photo ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an issue photo ID wrapping the given UUID
     */
    public static IssuePhotoId of(UUID value) {
        return new IssuePhotoId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
