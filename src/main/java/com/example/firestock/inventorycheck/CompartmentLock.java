package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.UserId;

import java.time.Instant;

/**
 * Represents a user's lock on a compartment during an inventory check.
 * This is an internal DTO for the compartment lock service - display names
 * are resolved separately when needed.
 *
 * @param compartmentId the compartment that is locked
 * @param userId the user who holds the lock
 * @param lockedAt when the lock was acquired
 */
public record CompartmentLock(
    CompartmentId compartmentId,
    UserId userId,
    Instant lockedAt
) {}
