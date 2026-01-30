package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

public record IssueId(UUID value) {
    public IssueId {
        Objects.requireNonNull(value, "Issue ID cannot be null");
    }

    public static IssueId generate() {
        return new IssueId(UUID.randomUUID());
    }

    public static IssueId of(UUID value) {
        return new IssueId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
