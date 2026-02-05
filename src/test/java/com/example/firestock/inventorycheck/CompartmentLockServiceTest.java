package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CompartmentLockServiceTest {

    private CompartmentLockService lockService;
    private InventoryCheckId checkId;
    private CompartmentId compartmentId;
    private UserId user1;
    private UserId user2;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        lockService = new CompartmentLockService(clock);
        checkId = InventoryCheckId.generate();
        compartmentId = CompartmentId.generate();
        user1 = UserId.generate();
        user2 = UserId.generate();
    }

    @Test
    void acquireLock_succeeds_whenCompartmentIsNotLocked() {
        boolean acquired = lockService.acquireLock(checkId, compartmentId, user1);

        assertTrue(acquired);
        assertTrue(lockService.getLock(checkId, compartmentId).isPresent());
        assertEquals(user1, lockService.getLock(checkId, compartmentId).get().userId());
    }

    @Test
    void acquireLock_succeeds_whenSameUserAlreadyHasLock() {
        lockService.acquireLock(checkId, compartmentId, user1);

        // Same user acquiring again should succeed
        boolean acquired = lockService.acquireLock(checkId, compartmentId, user1);

        assertTrue(acquired);
    }

    @Test
    void acquireLock_fails_whenDifferentUserHasLock() {
        lockService.acquireLock(checkId, compartmentId, user1);

        // Different user should fail
        boolean acquired = lockService.acquireLock(checkId, compartmentId, user2);

        assertFalse(acquired);
    }

    @Test
    void releaseLock_releasesLock_whenUserHasLock() {
        lockService.acquireLock(checkId, compartmentId, user1);

        lockService.releaseLock(checkId, compartmentId, user1);

        assertTrue(lockService.getLock(checkId, compartmentId).isEmpty());
    }

    @Test
    void releaseLock_doesNothing_whenUserDoesNotHaveLock() {
        lockService.acquireLock(checkId, compartmentId, user1);

        // User2 tries to release user1's lock - should not work
        lockService.releaseLock(checkId, compartmentId, user2);

        assertTrue(lockService.getLock(checkId, compartmentId).isPresent());
        assertEquals(user1, lockService.getLock(checkId, compartmentId).get().userId());
    }

    @Test
    void releaseLock_doesNothing_whenNoLockExists() {
        // Should not throw
        assertDoesNotThrow(() ->
            lockService.releaseLock(checkId, compartmentId, user1)
        );
    }

    @Test
    void getLock_returnsEmpty_whenNoLockExists() {
        var result = lockService.getLock(checkId, compartmentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getLock_returnsLock_whenLockExists() {
        lockService.acquireLock(checkId, compartmentId, user1);

        var result = lockService.getLock(checkId, compartmentId);

        assertTrue(result.isPresent());
        assertEquals(user1, result.get().userId());
        assertEquals(compartmentId, result.get().compartmentId());
        assertNotNull(result.get().lockedAt());
    }

    @Test
    void takeOver_acquiresLock_andReturnsPreviousHolder() {
        lockService.acquireLock(checkId, compartmentId, user1);

        var previousHolder = lockService.takeOver(checkId, compartmentId, user2);

        assertTrue(previousHolder.isPresent());
        assertEquals(user1, previousHolder.get());
        assertEquals(user2, lockService.getLock(checkId, compartmentId).get().userId());
    }

    @Test
    void takeOver_acquiresLock_andReturnsEmpty_whenNotPreviouslyLocked() {
        var previousHolder = lockService.takeOver(checkId, compartmentId, user1);

        assertTrue(previousHolder.isEmpty());
        assertEquals(user1, lockService.getLock(checkId, compartmentId).get().userId());
    }

    @Test
    void getLocksForCheck_returnsAllLocks() {
        var compartment1 = CompartmentId.generate();
        var compartment2 = CompartmentId.generate();

        lockService.acquireLock(checkId, compartment1, user1);
        lockService.acquireLock(checkId, compartment2, user2);

        var locks = lockService.getLocksForCheck(checkId);

        assertEquals(2, locks.size());
        assertEquals(user1, locks.get(compartment1));
        assertEquals(user2, locks.get(compartment2));
    }

    @Test
    void getLocksForCheck_returnsEmptyMap_whenNoLocks() {
        var locks = lockService.getLocksForCheck(checkId);

        assertTrue(locks.isEmpty());
    }

    @Test
    void releaseAllLocksForUser_releasesAllLocksForUser() {
        var check1 = InventoryCheckId.generate();
        var check2 = InventoryCheckId.generate();
        var compartment1 = CompartmentId.generate();
        var compartment2 = CompartmentId.generate();

        // User1 has locks on multiple checks/compartments
        lockService.acquireLock(check1, compartment1, user1);
        lockService.acquireLock(check2, compartment2, user1);
        // User2 also has a lock
        lockService.acquireLock(check1, compartment2, user2);

        lockService.releaseAllLocksForUser(user1);

        // User1's locks should be gone
        assertTrue(lockService.getLock(check1, compartment1).isEmpty());
        assertTrue(lockService.getLock(check2, compartment2).isEmpty());
        // User2's lock should remain
        assertTrue(lockService.getLock(check1, compartment2).isPresent());
    }

    @Test
    void clearLocksForCheck_removesAllLocksForCheck() {
        var compartment1 = CompartmentId.generate();
        var compartment2 = CompartmentId.generate();
        var otherCheckId = InventoryCheckId.generate();

        lockService.acquireLock(checkId, compartment1, user1);
        lockService.acquireLock(checkId, compartment2, user2);
        lockService.acquireLock(otherCheckId, compartment1, user1);

        lockService.clearLocksForCheck(checkId);

        // Locks for checkId should be gone
        assertTrue(lockService.getLock(checkId, compartment1).isEmpty());
        assertTrue(lockService.getLock(checkId, compartment2).isEmpty());
        // Lock for other check should remain
        assertTrue(lockService.getLock(otherCheckId, compartment1).isPresent());
    }

    @Test
    void locks_areScopedToCheck() {
        var check1 = InventoryCheckId.generate();
        var check2 = InventoryCheckId.generate();

        // Both users can lock the same compartment if on different checks
        boolean acquired1 = lockService.acquireLock(check1, compartmentId, user1);
        boolean acquired2 = lockService.acquireLock(check2, compartmentId, user2);

        assertTrue(acquired1);
        assertTrue(acquired2);
        assertEquals(user1, lockService.getLock(check1, compartmentId).get().userId());
        assertEquals(user2, lockService.getLock(check2, compartmentId).get().userId());
    }
}
