# Implementation Plan: Inventory Check UX (UX-Inventory-Check.md)

## Overview

Implement the Inventory Check feature for FireStock as specified in `Spec/UXDesigns/UX-Inventory-Check.md`. This is a mobile-first responsive Vaadin Flow application using Push for real-time updates.

## Technology Decisions

- **Vaadin version**: 25.0.3 (already configured)
- **UI Framework**: Vaadin Flow (Java server-side views)
- **Real-time updates**: Vaadin Push with Broadcaster pattern
- **Network errors**: Vaadin's built-in ReconnectDialogConfiguration
- **Styling**: Lumo theme with CSS utility classes

## Architecture Constraints

1. **All data access from UI must go through services.** Views are not allowed to access Query objects or Repositories directly.
2. **User display names must be fetched from the user table** based on `UserId`. Names should not be passed as parameters or stored redundantly.

---

## Backend Changes

### Step 0: Backend Enhancements

Before implementing the UI, the following backend changes are required to support the new UX.

#### 0.1 User Display Name Lookup

Add method to `UserQuery.java` for looking up user display names:

```java
/**
 * Gets display names for a collection of user IDs.
 * Returns a map of UserId to full name (first + last).
 */
Map<UserId, String> getDisplayNames(Collection<UserId> userIds);

/**
 * Gets the display name for a single user ID.
 * Returns empty if user not found.
 */
Optional<String> getDisplayName(UserId userId);
```

Add a new service `UserDisplayNameService.java` in `src/main/java/com/example/firestock/inventorycheck/`:

```java
@Service
public class UserDisplayNameService {
    private final UserQuery userQuery;

    /**
     * Gets display names for multiple users (batch lookup).
     */
    @Transactional(readOnly = true)
    public Map<UserId, String> getDisplayNames(Collection<UserId> userIds);

    /**
     * Gets display name for a single user.
     */
    @Transactional(readOnly = true)
    public Optional<String> getDisplayName(UserId userId);
}
```

#### 0.2 New DTOs

Create new DTOs in `src/main/java/com/example/firestock/inventorycheck/`:

**`ActiveCheckInfo.java`** - Information about an active check for display:
```java
public record ActiveCheckInfo(
    InventoryCheckId checkId,
    ApparatusId apparatusId,
    String apparatusUnitNumber,
    Instant startedAt,
    int totalItems,
    int verifiedCount
) {}
```

**`ApparatusWithCheckStatus.java`** - Apparatus with current check status:
```java
public record ApparatusWithCheckStatus(
    ApparatusId id,
    UnitNumber unitNumber,
    String stationName,
    LocalDateTime lastCheckDate,
    boolean hasActiveCheck,
    List<String> currentCheckerNames  // Display names looked up from user table
) {}
```

**`CompartmentCheckProgress.java`** - Progress for a single compartment:
```java
public record CompartmentCheckProgress(
    CompartmentId id,
    String code,
    String name,
    int displayOrder,
    int totalItems,
    int verifiedCount,
    boolean isFullyChecked,
    String currentCheckerName  // Display name looked up from user table, null if no one checking
) {}
```

**`CheckableItemWithStatus.java`** - Item with its verification status in current check:
```java
public record CheckableItemWithStatus(
    CheckableItem item,
    VerificationStatus verificationStatus,  // Null if not yet verified
    Instant verifiedAt,
    String verifiedByName  // Display name looked up from user table
) {}
```

**`CompartmentLock.java`** - Represents a user's lock on a compartment (internal, no display name):
```java
public record CompartmentLock(
    CompartmentId compartmentId,
    UserId userId,
    Instant lockedAt
) {}
```

#### 0.3 New Service: CompartmentLockService

Create `src/main/java/com/example/firestock/inventorycheck/CompartmentLockService.java`:

This service manages in-memory compartment locks to track who is currently checking each compartment. It only stores `UserId` - display names are resolved by the calling service when needed.

```java
@Service
public class CompartmentLockService {

    // In-memory storage: checkId -> compartmentId -> lock
    private final Map<InventoryCheckId, Map<CompartmentId, CompartmentLock>> locks =
        new ConcurrentHashMap<>();

    /**
     * Attempts to acquire a lock on a compartment.
     * @return true if lock acquired, false if already locked by another user
     */
    public boolean acquireLock(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId);

    /**
     * Releases a lock on a compartment.
     */
    public void releaseLock(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId);

    /**
     * Gets the current lock holder for a compartment.
     * @return the lock info (with UserId), or empty if not locked
     */
    public Optional<CompartmentLock> getLock(InventoryCheckId checkId, CompartmentId compartmentId);

    /**
     * Forces a take-over of a compartment lock.
     * @return the previous lock holder's UserId, or empty if wasn't locked
     */
    public Optional<UserId> takeOver(InventoryCheckId checkId, CompartmentId compartmentId, UserId newUserId);

    /**
     * Gets all locks for a check (to show who is checking what).
     * Returns map of CompartmentId to UserId.
     */
    public Map<CompartmentId, UserId> getLocksForCheck(InventoryCheckId checkId);

    /**
     * Releases all locks held by a user (called on session end/timeout).
     */
    public void releaseAllLocksForUser(UserId userId);

    /**
     * Clears all locks for a check (called when check is completed/abandoned).
     */
    public void clearLocksForCheck(InventoryCheckId checkId);
}
```

#### 0.4 New Methods in ShiftInventoryCheckService

Add the following methods to `ShiftInventoryCheckService.java`. These methods use `UserDisplayNameService` to resolve user IDs to display names when building response DTOs.

```java
// Add dependency
private final CompartmentLockService compartmentLockService;
private final UserDisplayNameService userDisplayNameService;

/**
 * Gets all apparatus for a station with their current check status.
 * Includes display names of users currently checking each apparatus.
 * User names are looked up from the user table.
 */
@Transactional(readOnly = true)
@PreAuthorize("@stationAccess.canAccessStation(#stationId)")
public List<ApparatusWithCheckStatus> getApparatusWithCheckStatus(StationId stationId);

/**
 * Gets any active check for the station (for resume banner).
 * @return the active check info, or empty if no active checks
 */
@Transactional(readOnly = true)
@PreAuthorize("@stationAccess.canAccessStation(#stationId)")
public Optional<ActiveCheckInfo> getActiveCheckForStation(StationId stationId);

/**
 * Gets compartment progress for an active check.
 * Shows how many items are verified in each compartment.
 * Includes display names of current checkers (looked up from user table).
 */
@Transactional(readOnly = true)
public List<CompartmentCheckProgress> getCompartmentProgress(InventoryCheckId checkId);

/**
 * Gets items in a compartment with their verification status for the current check.
 * Items are sorted: unchecked first, then checked (by verification time).
 * Includes verifier display names (looked up from user table).
 */
@Transactional(readOnly = true)
public List<CheckableItemWithStatus> getItemsWithStatus(
    InventoryCheckId checkId, CompartmentId compartmentId);

/**
 * Starts checking a compartment (acquires lock).
 * @param userId the user acquiring the lock
 * @throws CompartmentLockedException if another user is checking (includes their display name)
 */
public void startCheckingCompartment(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId);

/**
 * Stops checking a compartment (releases lock).
 */
public void stopCheckingCompartment(InventoryCheckId checkId, CompartmentId compartmentId, UserId userId);

/**
 * Takes over a compartment from another user.
 * @return the previous checker's display name (for notification), or null if wasn't locked
 */
public String takeOverCompartment(InventoryCheckId checkId, CompartmentId compartmentId, UserId newUserId);

/**
 * Gets who is currently checking a compartment.
 * @return the checker's display name, or empty if not being checked
 */
@Transactional(readOnly = true)
public Optional<String> getCompartmentCheckerName(InventoryCheckId checkId, CompartmentId compartmentId);

/**
 * Exception thrown when trying to check a compartment that's locked by another user.
 */
public static class CompartmentLockedException extends RuntimeException {
    private final String lockedByName;  // Display name looked up from user table
    public CompartmentLockedException(String lockedByName) {
        super("Compartment is being checked by " + lockedByName);
        this.lockedByName = lockedByName;
    }
    public String getLockedByName() { return lockedByName; }
}
```

#### 0.5 Query Enhancements

Add methods to `InventoryCheckQuery.java`:

```java
/**
 * Finds any active check for a station.
 */
Optional<ActiveCheckInfo> findActiveCheckForStation(StationId stationId);

/**
 * Gets verification records for all items in a compartment for a specific check.
 * Returns item IDs with their verification status and verifier UserId.
 */
List<ItemVerificationRecord> findVerifiedItemsInCompartment(
    InventoryCheckId checkId, CompartmentId compartmentId);

/**
 * Counts verified items per compartment for a check.
 */
Map<CompartmentId, Integer> countVerifiedItemsByCompartment(InventoryCheckId checkId);
```

New record for verification data (internal use, uses existing domain model):
```java
record ItemVerificationRecord(
    CheckedItemTarget target,  // Uses existing sealed interface (EquipmentCheckTarget or ConsumableCheckTarget)
    VerificationStatus status,
    Instant verifiedAt,
    UserId verifiedBy  // UserId, not display name
) {}
```

Add methods to `ApparatusQuery.java`:

```java
/**
 * Finds apparatus with active check info for a station.
 * Returns apparatus data with check status (but not user names - those are resolved separately).
 */
List<ApparatusWithActiveCheckRecord> findByStationIdWithActiveChecks(StationId stationId);
```

#### 0.6 Modify Existing Methods

Update `completeCheck()` and `abandonCheck()` in `ShiftInventoryCheckService` to clear compartment locks:

```java
public InventoryCheckSummary completeCheck(InventoryCheckId checkId) {
    // ... existing code ...
    compartmentLockService.clearLocksForCheck(checkId);  // Add this
    return ...;
}

public void abandonCheck(InventoryCheckId checkId) {
    // ... existing code ...
    compartmentLockService.clearLocksForCheck(checkId);  // Add this
}
```

---

## Frontend Files

### New Files to Create

```
src/main/java/com/example/firestock/views/inventorycheck/
├── SelectApparatusView.java          # Step 2
├── SelectCompartmentView.java        # Step 3
├── CheckCompartmentView.java         # Step 4
├── CheckCompartmentReadOnlyView.java # Step 5
├── ViewSummaryView.java              # Step 6
├── components/
│   ├── ApparatusCard.java            # Step 2
│   ├── ResumeBanner.java             # Step 2
│   ├── CompartmentCard.java          # Step 3
│   ├── EquipmentItemCard.java        # Step 4
│   ├── ConsumableItemCard.java       # Step 4
│   └── CheckProgressBar.java         # Step 3, 4
├── dialogs/
│   ├── MarkAsMissingDialog.java      # Step 4
│   ├── MarkAsDamagedDialog.java      # Step 4
│   ├── ConfirmAbandonDialog.java     # Step 6
│   ├── ConfirmCompleteDialog.java    # Step 6
│   └── ConfirmTakeOverDialog.java    # Step 5
└── broadcast/
    └── InventoryCheckBroadcaster.java # Step 7

src/main/java/com/example/firestock/config/
└── PushConfiguration.java            # Step 1

src/main/frontend/styles/
└── inventory-check.css               # Step 8
```

---

## Implementation Steps

### Step 1: Infrastructure Setup

**Goal**: Configure Push and network error handling.

**Tasks**:
1. Add `@Push` annotation to `Application.java` (implements `AppShellConfigurator`)
2. Create `PushConfiguration.java` with `VaadinServiceInitListener` to configure `ReconnectDialogConfiguration`:
   - "Connection lost. Trying to reconnect..."
   - "Unable to connect. Please check your network connection."
3. Create base mobile-responsive CSS styles in `src/main/frontend/styles/inventory-check.css`

**Key Code Pattern** (ReconnectDialogConfiguration):
```java
@Component
public class PushConfiguration implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            ReconnectDialogConfiguration dialog = uiInitEvent.getUI()
                .getReconnectDialogConfiguration();
            dialog.setDialogText("Connection lost. Trying to reconnect...");
            dialog.setDialogTextGaveUp("Unable to connect. Please check your network connection.");
        });
    }
}
```

**Verification**: Start app, disconnect network, verify custom messages appear.

---

### Step 2: Select Apparatus View

**Goal**: First screen showing station's apparatus list with resume capability.

**Route**: `/inventory-check` or `/inventory-check/select-apparatus`

**Components to create**:
- `SelectApparatusView.java` - Main view
- `ApparatusCard.java` - Card showing apparatus info, last check, current checkers
- `ResumeBanner.java` - Banner for resuming in-progress check

**Layout**:
```
[Header: Back button + "Station Name Inventory Check"]
[ResumeBanner - if in-progress check exists]
[Title: "Select Apparatus"]
[Scrollable list of ApparatusCard components]
```

**Service calls used**:
- `shiftInventoryCheckService.getApparatusWithCheckStatus(stationId)` - for apparatus list with checker names
- `shiftInventoryCheckService.getActiveCheckForStation(stationId)` - for resume banner

**Behavior**:
- Back button → navigate to main dashboard (home route)
- Click apparatus → navigate to Select Compartment
- Resume button → navigate to Select Compartment for that apparatus

**Mobile considerations**:
- Full-width cards
- Large touch targets (min 48px)
- High contrast status indicators

---

### Step 3: Select Compartment View

**Goal**: Show compartments for selected apparatus with progress tracking.

**Route**: `/inventory-check/apparatus/{apparatusId}/compartments`

**Components to create**:
- `SelectCompartmentView.java` - Main view
- `CompartmentCard.java` - Card showing compartment name, item count, checker
- `CheckProgressBar.java` - Reusable progress bar component

**Layout**:
```
[Header: Back button + "Apparatus Name Inventory Check"]
[Title: "Select Compartment"]
[Progress: "Checked: X of Y compartments"]
[CheckProgressBar]
[Scrollable list of CompartmentCard components]
[Footer: "VIEW SUMMARY" button]
```

**Service calls used**:
- `shiftInventoryCheckService.getApparatusDetails(apparatusId)` - for apparatus info
- `shiftInventoryCheckService.getActiveCheck(apparatusId)` - to get or start check
- `shiftInventoryCheckService.getCompartmentProgress(checkId)` - for compartment progress with checker names

**Behavior**:
- Back button → Select Apparatus
- Click compartment:
  - Calls `shiftInventoryCheckService.startCheckingCompartment()` to acquire lock
  - If lock acquired → Check Compartment
  - If `CompartmentLockedException` → Check Compartment (Read-Only)
- View Summary button → View Summary

---

### Step 4: Check Compartment View

**Goal**: Main item verification screen with Present/Missing/Damaged actions.

**Route**: `/inventory-check/apparatus/{apparatusId}/compartment/{compartmentId}`

**Components to create**:
- `CheckCompartmentView.java` - Main view
- `EquipmentItemCard.java` - Card for serialized equipment with action buttons
- `ConsumableItemCard.java` - Card for consumables with quantity stepper
- `MarkAsMissingDialog.java` - Dialog for missing items
- `MarkAsDamagedDialog.java` - Dialog for damaged items

**Layout**:
```
[Header: Back button + "Apparatus - Compartment"]
[Progress: "Checked: X of Y items"]
[CheckProgressBar]
[Scrollable list - unchecked items first, then checked items]
```

**EquipmentItemCard layout**:
```
[Display Name] [CREW badge if crew-owned]
[Type Name]
[S/N: Serial Number]
[Status Badge: NOT CHECKED / PRESENT / MISSING / DAMAGED]
[PRESENT] [MISSING] [DAMAGED] buttons
```

**ConsumableItemCard layout**:
```
[Display Name]
[Consumable]
[Expected: X | Actual: [-] Y [+]] stepper
[Status Badge: OK / DISCREPANCY]
[Notes field - shown when discrepancy > 20%]
```

**Service calls used**:
- `shiftInventoryCheckService.getItemsWithStatus(checkId, compartmentId)` - for items with verification status
- `shiftInventoryCheckService.verifyItem(request, userId)` - to verify items
- `shiftInventoryCheckService.stopCheckingCompartment()` - on detach/back navigation

**Behavior**:
- Present button → call `verifyItem()` with PRESENT status, move to bottom
- Missing button → open MarkAsMissingDialog
- Damaged button → open MarkAsDamagedDialog
- Stepper change → call `verifyItem()` with quantity
- Back button → release lock, navigate to Select Compartment

**Real-time**: Register with broadcaster to receive updates from other users.

**Lifecycle**:
- `onAttach`: Register with broadcaster, verify lock is still held
- `onDetach`: Release compartment lock, unregister from broadcaster

---

### Step 5: Check Compartment Read-Only View

**Goal**: View compartment being checked by another user with take-over option.

**Route**: `/inventory-check/apparatus/{apparatusId}/compartment/{compartmentId}/readonly`

**Components to create**:
- `CheckCompartmentReadOnlyView.java` - Read-only variant
- `ConfirmTakeOverDialog.java` - Confirmation for taking over

**Layout**:
```
[Header: Back button + "Apparatus - Compartment"]
[Warning Banner: "This compartment is being checked by [Name]"]
[Progress: "Checked: X of Y items"]
[CheckProgressBar]
[Scrollable list - read-only, no action buttons]
[Footer: BACK | TAKE OVER buttons]
```

**Service calls used**:
- `shiftInventoryCheckService.getItemsWithStatus(checkId, compartmentId)` - for items
- `shiftInventoryCheckService.getCompartmentCheckerName(checkId, compartmentId)` - for checker's display name
- `shiftInventoryCheckService.takeOverCompartment()` - to take over (returns previous checker's name)

**Behavior**:
- Real-time updates via Push as other user checks items
- Take Over button → ConfirmTakeOverDialog
- Confirm take over →
  - Call `takeOverCompartment()`
  - Broadcast `CheckTakeOverEvent` to notify previous checker
  - Navigate to editable Check Compartment view

---

### Step 6: View Summary

**Goal**: Summary dashboard with complete/abandon options.

**Route**: `/inventory-check/apparatus/{apparatusId}/summary`

**Components to create**:
- `ViewSummaryView.java` - Main view
- `ConfirmAbandonDialog.java` - Confirm abandon
- `ConfirmCompleteDialog.java` - Confirm complete

**Layout**:
```
[Header: Back button + "Apparatus Check Summary"]
[Stats Grid 2x2:]
  [Total Items] [Present]
  [Issues]      [Remaining]
[Footer: ABANDON CHECK | COMPLETE CHECK buttons]
```

**Service calls used**:
- `shiftInventoryCheckService.getCheck(checkId)` - for summary stats
- `shiftInventoryCheckService.abandonCheck(checkId)` - to abandon
- `shiftInventoryCheckService.completeCheck(checkId)` - to complete

**Behavior**:
- Abandon Check → ConfirmAbandonDialog → call `abandonCheck()` → Select Apparatus
- Complete Check → ConfirmCompleteDialog → call `completeCheck()` → Select Apparatus
- Back button → Select Compartment

---

### Step 7: Real-Time Collaboration (Broadcaster)

**Goal**: Enable real-time updates between users checking same apparatus.

**Components to create**:
- `InventoryCheckBroadcaster.java` - Singleton broadcaster

**Events to broadcast**:
```java
public sealed interface InventoryCheckEvent {
    ApparatusId apparatusId();

    record ItemVerifiedEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String itemId,  // equipmentItemId or consumableStockId as string
        VerificationStatus status,
        String verifiedByName  // Display name (resolved before broadcasting)
    ) implements InventoryCheckEvent {}

    record CompartmentLockChangedEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String lockedByName,  // Display name (resolved before broadcasting), null if unlocked
        boolean isLocked
    ) implements InventoryCheckEvent {}

    record CheckTakeOverEvent(
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String previousCheckerName,  // Display name
        String newCheckerName        // Display name
    ) implements InventoryCheckEvent {}

    record CheckCompletedEvent(
        ApparatusId apparatusId,
        InventoryCheckId checkId
    ) implements InventoryCheckEvent {}
}
```

**Broadcaster pattern**:
```java
@Component
public class InventoryCheckBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Map<ApparatusId, Set<Consumer<InventoryCheckEvent>>> listeners =
        new ConcurrentHashMap<>();

    public synchronized Registration register(
            ApparatusId apparatusId, Consumer<InventoryCheckEvent> listener) {
        listeners.computeIfAbsent(apparatusId, k -> ConcurrentHashMap.newKeySet())
            .add(listener);
        return () -> {
            Set<Consumer<InventoryCheckEvent>> set = listeners.get(apparatusId);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    listeners.remove(apparatusId);
                }
            }
        };
    }

    public void broadcast(InventoryCheckEvent event) {
        Set<Consumer<InventoryCheckEvent>> apparatusListeners = listeners.get(event.apparatusId());
        if (apparatusListeners != null) {
            for (Consumer<InventoryCheckEvent> listener : apparatusListeners) {
                executor.execute(() -> listener.accept(event));
            }
        }
    }
}
```

**View integration pattern**:
```java
@Override
protected void onAttach(AttachEvent event) {
    UI ui = event.getUI();
    registration = broadcaster.register(apparatusId, e -> {
        ui.access(() -> handleBroadcastEvent(e));
    });
}

@Override
protected void onDetach(DetachEvent event) {
    if (registration != null) {
        registration.remove();
    }
    // Release compartment lock if held
    shiftInventoryCheckService.stopCheckingCompartment(checkId, compartmentId, currentUserId);
}
```

**Integration with service**:
The service methods resolve user IDs to display names before broadcasting events:
- `verifyItem()` → look up verifier's name → broadcast `ItemVerifiedEvent`
- `startCheckingCompartment()` → look up user's name → broadcast `CompartmentLockChangedEvent`
- `stopCheckingCompartment()` → broadcast `CompartmentLockChangedEvent` (with null name)
- `takeOverCompartment()` → look up both names → broadcast `CheckTakeOverEvent`
- `completeCheck()` → broadcast `CheckCompletedEvent`

---

### Step 8: Mobile-First Responsive Styling

**Goal**: Ensure all views work well on mobile and adapt to desktop.

**CSS file**: `src/main/frontend/styles/inventory-check.css`

**Key styles**:
- Touch targets minimum 48x48px
- High contrast colors for status badges (green/red/orange/gray)
- Single column layout on mobile, optional multi-column on desktop
- Sticky headers and footers
- Card-based layout with clear visual hierarchy

**Lumo utility classes to use**:
- `gap-m`, `p-m`, `m-0` for spacing
- `text-l`, `font-semibold` for typography
- `bg-contrast-5` for card backgrounds
- `border-radius-m` for rounded corners

**Responsive breakpoints**:
```css
/* Mobile first - default styles */
.inventory-check-view { max-width: 100%; }

/* Tablet and up */
@media (min-width: 768px) {
    .inventory-check-view { max-width: 600px; margin: 0 auto; }
}
```

---

## Summary of Backend Changes

| File | Change Type | Description |
|------|-------------|-------------|
| `UserQuery.java` | Modify | Add `getDisplayName()` and `getDisplayNames()` methods |
| `UserDisplayNameService.java` | New | Service for looking up user display names |
| `ActiveCheckInfo.java` | New | DTO for active check display |
| `ApparatusWithCheckStatus.java` | New | DTO for apparatus with checker names |
| `CompartmentCheckProgress.java` | New | DTO for compartment progress with checker name |
| `CheckableItemWithStatus.java` | New | DTO for item with verification status and verifier name |
| `CompartmentLock.java` | New | Internal DTO for compartment lock (UserId only, no name) |
| `ItemVerificationRecord.java` | New | Internal record for verification data (uses `CheckedItemTarget` from domain model) |
| `CompartmentLockService.java` | New | Service for in-memory compartment locks |
| `ShiftInventoryCheckService.java` | Modify | Add 8 new methods, modify 2 existing, add dependencies |
| `InventoryCheckQuery.java` | Modify | Add 3 new query methods |
| `ApparatusQuery.java` | Modify | Add 1 new query method |

---

## Verification Plan

1. **Unit Tests**:
   - Test `CompartmentLockService` lock/unlock/takeover logic
   - Test `UserDisplayNameService` name resolution
   - Test new service methods
   - Existing service tests should still pass

2. **Manual Testing Flow**:
   - Log in as firefighter assigned to a station
   - Navigate to inventory check from dashboard
   - Select apparatus, verify last check dates shown
   - Start check on apparatus
   - Verify compartment list with progress
   - Check items: test Present, Missing, Damaged flows
   - Test consumable quantity stepper and notes requirement
   - Complete check, verify summary accurate
   - Test abandon flow

3. **Multi-User Testing**:
   - Open app in two browsers with different users
   - Verify real-time updates when one user checks items
   - **Verify user display names appear correctly** (not user IDs)
   - Test compartment lock/read-only mode
   - Test take-over functionality
   - Verify previous checker sees notification with correct names

4. **Network Testing**:
   - Disconnect network, verify reconnect dialog appears
   - Reconnect, verify state restored

5. **Mobile Testing**:
   - Test on mobile device or browser dev tools mobile mode
   - Verify touch targets are adequate
   - Verify scrolling works correctly

---

## Dependencies

- Existing `ShiftInventoryCheckService` (will be modified)
- Existing `UserQuery` (will be modified)
- Existing domain model and repositories
- Spring Security for user context
- New `UserDisplayNameService` for name lookups
- New `CompartmentLockService` for lock management
- New `InventoryCheckBroadcaster` for real-time updates

## Notes

- Barcode scanning intentionally excluded per spec
- Photo upload for damage not included in this version
- Auto-abandon (4 hour timeout) handled by existing backend scheduled task
- Compartment locks are in-memory; they will be cleared on server restart (acceptable for this use case)
- User display names are always fetched from the database, never stored redundantly in locks or passed as parameters
- Existing views in `src/main/java/com/example/firestock/views/inventorycheck/` can be used as reference patterns but will be replaced by the new implementation
