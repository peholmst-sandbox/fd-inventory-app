package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record IssuePhotoId(UUID value) {
    public IssuePhotoId {
        Objects.requireNonNull(value, "Issue photo ID cannot be null");
    }

    public static IssuePhotoId generate() {
        return new IssuePhotoId(UUID.randomUUID());
    }

    public static IssuePhotoId of(UUID value) {
        return new IssuePhotoId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
