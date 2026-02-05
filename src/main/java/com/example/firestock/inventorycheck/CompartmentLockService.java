package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.UserId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing in-memory compartment locks during inventory checks.
 *
 * <p>This service tracks who is currently checking each compartment to enable
 * real-time collaboration. Locks are held in-memory only - they will be cleared
 * on server restart, which is acceptable for this use case.
 *
 * <p>Note: This service only stores UserId, not display names. Display names
 * are resolved by the calling service when needed using UserDisplayNameService.
 */
@Service
public class CompartmentLockService {

    private final Map<InventoryCheckId, Map<CompartmentId, CompartmentLock>> locks =
        new ConcurrentHashMap<>();
    private final Clock clock;

    public CompartmentLockService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Attempts to acquire a lock on a compartment.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to lock
     * @param userId the user attempting to acquire the lock
     * @return true if lock acquired, false if already locked by another user
     */
    public synchronized boolean acquireLock(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId) {
        var checkLocks = locks.computeIfAbsent(checkId, k -> new ConcurrentHashMap<>());

        var existingLock = checkLocks.get(compartmentId);
        if (existingLock != null && !existingLock.userId().equals(userId)) {
            return false; // Locked by someone else
        }

        // Already have lock or compartment is free
        if (existingLock == null || !existingLock.userId().equals(userId)) {
            checkLocks.put(compartmentId, new CompartmentLock(compartmentId, userId, clock.instant()));
        }
        return true;
    }

    /**
     * Releases a lock on a compartment.
     * Only the lock holder can release the lock.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to unlock
     * @param userId the user releasing the lock
     */
    public synchronized void releaseLock(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId) {
        var checkLocks = locks.get(checkId);
        if (checkLocks == null) {
            return;
        }

        var existingLock = checkLocks.get(compartmentId);
        if (existingLock != null && existingLock.userId().equals(userId)) {
            checkLocks.remove(compartmentId);
        }
    }

    /**
     * Gets the current lock holder for a compartment.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to check
     * @return the lock info (with UserId), or empty if not locked
     */
    public Optional<CompartmentLock> getLock(InventoryCheckId checkId, CompartmentId compartmentId) {
        var checkLocks = locks.get(checkId);
        if (checkLocks == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(checkLocks.get(compartmentId));
    }

    /**
     * Forces a take-over of a compartment lock.
     * This removes any existing lock and grants the lock to the new user.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to take over
     * @param newUserId the user taking over the lock
     * @return the previous lock holder's UserId, or empty if wasn't locked
     */
    public synchronized Optional<UserId> takeOver(InventoryCheckId checkId, CompartmentId compartmentId, UserId newUserId) {
        var checkLocks = locks.computeIfAbsent(checkId, k -> new ConcurrentHashMap<>());

        var existingLock = checkLocks.get(compartmentId);
        UserId previousUserId = existingLock != null ? existingLock.userId() : null;

        checkLocks.put(compartmentId, new CompartmentLock(compartmentId, newUserId, clock.instant()));

        return Optional.ofNullable(previousUserId);
    }

    /**
     * Gets all locks for a check (to show who is checking what).
     *
     * @param checkId the inventory check ID
     * @return map of CompartmentId to UserId for all locked compartments
     */
    public Map<CompartmentId, UserId> getLocksForCheck(InventoryCheckId checkId) {
        var checkLocks = locks.get(checkId);
        if (checkLocks == null) {
            return Map.of();
        }

        Map<CompartmentId, UserId> result = new HashMap<>();
        checkLocks.forEach((compartmentId, lock) -> result.put(compartmentId, lock.userId()));
        return result;
    }

    /**
     * Releases all locks held by a user across all checks.
     * Called when a user's session ends or times out.
     *
     * @param userId the user whose locks should be released
     */
    public synchronized void releaseAllLocksForUser(UserId userId) {
        for (var checkLocks : locks.values()) {
            checkLocks.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
        }
    }

    /**
     * Clears all locks for a check.
     * Called when a check is completed or abandoned.
     *
     * @param checkId the inventory check ID
     */
    public synchronized void clearLocksForCheck(InventoryCheckId checkId) {
        locks.remove(checkId);
    }
}
