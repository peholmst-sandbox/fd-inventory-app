package com.example.firestock.inventorycheck;

import com.example.firestock.domain.inventorycheck.ConsumableCheckTarget;
import com.example.firestock.domain.inventorycheck.EquipmentCheckTarget;
import com.example.firestock.domain.inventorycheck.InProgressCheck;
import com.example.firestock.domain.inventorycheck.InventoryCheckException;
import com.example.firestock.domain.inventorycheck.InventoryCheckItem;
import com.example.firestock.domain.issue.ConsumableIssueTarget;
import com.example.firestock.domain.issue.EquipmentIssueTarget;
import com.example.firestock.domain.issue.IssueTarget;
import com.example.firestock.domain.issue.OpenIssue;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.InventoryCheckItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.infrastructure.persistence.EquipmentItemRepository;
import com.example.firestock.infrastructure.persistence.InventoryCheckItemRepository;
import com.example.firestock.infrastructure.persistence.InventoryCheckRepository;
import com.example.firestock.infrastructure.persistence.IssueRepository;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.jooq.enums.EquipmentStatus;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.example.firestock.security.StationAccessEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import java.util.List;
import java.util.Optional;

/**
 * Service for UC-01: Shift Inventory Check.
 *
 * <p>This service provides operations for firefighters to perform shift inventory checks
 * on fire apparatus, verifying that all required equipment is present and in good condition.
 *
 * <p><b>Security:</b> Authentication is required to use this service. When Spring Security
 * is configured, add {@code @PreAuthorize("isAuthenticated()")} to enforce at service level.
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><b>BR-01</b>: Only one active (IN_PROGRESS) check per apparatus at a time</li>
 *   <li><b>BR-02</b>: A check cannot be completed until all items are verified</li>
 *   <li><b>BR-04</b>: Issues are automatically created for damaged or missing items</li>
 *   <li><b>BR-05</b>: Consumable quantity discrepancies >20% require notes</li>
 * </ul>
 */
@Service
@Transactional
@PreAuthorize("isAuthenticated()")
public class ShiftInventoryCheckService {

    private final ApparatusQuery apparatusQuery;
    private final InventoryCheckQuery inventoryCheckQuery;
    private final EquipmentQuery equipmentQuery;
    private final InventoryCheckRepository inventoryCheckRepository;
    private final InventoryCheckItemRepository inventoryCheckItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final IssueRepository issueRepository;
    private final StationAccessEvaluator stationAccess;
    private final Clock clock;

    public ShiftInventoryCheckService(
            ApparatusQuery apparatusQuery,
            InventoryCheckQuery inventoryCheckQuery,
            EquipmentQuery equipmentQuery,
            InventoryCheckRepository inventoryCheckRepository,
            InventoryCheckItemRepository inventoryCheckItemRepository,
            EquipmentItemRepository equipmentItemRepository,
            IssueRepository issueRepository,
            StationAccessEvaluator stationAccess,
            Clock clock) {
        this.apparatusQuery = apparatusQuery;
        this.inventoryCheckQuery = inventoryCheckQuery;
        this.equipmentQuery = equipmentQuery;
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.inventoryCheckItemRepository = inventoryCheckItemRepository;
        this.equipmentItemRepository = equipmentItemRepository;
        this.issueRepository = issueRepository;
        this.stationAccess = stationAccess;
        this.clock = clock;
    }

    /**
     * Gets the list of apparatus at a station for selection.
     *
     * @param stationId the station to get apparatus for
     * @return list of apparatus summaries with last check dates
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@stationAccess.canAccessStation(#stationId)")
    public List<ApparatusSummary> getApparatusForStation(StationId stationId) {
        return apparatusQuery.findByStationId(stationId);
    }

    /**
     * Gets the full details of an apparatus including compartments and items.
     *
     * @param apparatusId the apparatus to retrieve
     * @return the apparatus details
     * @throws IllegalArgumentException if the apparatus is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public ApparatusDetails getApparatusDetails(ApparatusId apparatusId) {
        stationAccess.requireApparatusAccess(apparatusId);
        return apparatusQuery.findByIdWithCompartmentsAndItems(apparatusId)
            .orElseThrow(() -> new IllegalArgumentException("Apparatus not found: " + apparatusId));
    }

    /**
     * Starts a new inventory check for an apparatus.
     *
     * <p>Enforces <b>BR-01</b>: Only one active check per apparatus.
     *
     * @param apparatusId the apparatus to check
     * @param performedBy the user starting the check
     * @return the created inventory check summary
     * @throws ActiveCheckExistsException if an active check already exists for this apparatus
     * @throws IllegalArgumentException if the apparatus is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public InventoryCheckSummary startCheck(ApparatusId apparatusId, UserId performedBy) {
        stationAccess.requireApparatusAccess(apparatusId);

        // BR-01: Check for existing active check
        if (inventoryCheckRepository.hasActiveCheckForApparatus(apparatusId)) {
            throw new ActiveCheckExistsException(
                "An active inventory check already exists for this apparatus. " +
                "Complete or abandon the existing check before starting a new one.");
        }

        // Get apparatus details to determine total items and station
        var apparatus = getApparatusDetails(apparatusId);
        int totalItems = apparatus.totalItemCount();

        // Create the check using domain model
        var checkId = InventoryCheckId.generate();
        var check = InProgressCheck.start(
            checkId,
            apparatusId,
            apparatus.stationId(),
            performedBy,
            clock.instant(),
            totalItems
        );
        inventoryCheckRepository.save(check);

        return inventoryCheckQuery.findById(checkId)
            .orElseThrow(() -> new IllegalStateException("Failed to create inventory check"));
    }

    /**
     * Gets an existing in-progress check for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the active check summary, or empty if no active check
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public Optional<InventoryCheckSummary> getActiveCheck(ApparatusId apparatusId) {
        stationAccess.requireApparatusAccess(apparatusId);
        return inventoryCheckRepository.findActiveByApparatusId(apparatusId)
            .map(check -> new InventoryCheckSummary(
                check.id(),
                check.apparatusId(),
                check.status(),
                check.startedAt(),
                null, // In-progress checks don't have completedAt
                check.progress().totalItems(),
                check.progress().verifiedCount(),
                check.progress().issuesFoundCount()
            ));
    }

    /**
     * Gets an inventory check by ID.
     *
     * @param checkId the check ID
     * @return the check summary
     * @throws IllegalArgumentException if the check is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public InventoryCheckSummary getCheck(InventoryCheckId checkId) {
        stationAccess.requireInventoryCheckAccess(checkId);
        return inventoryCheckQuery.findById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory check not found: " + checkId));
    }

    /**
     * Records a verification of an item during an inventory check.
     *
     * <p>Enforces:
     * <ul>
     *   <li><b>BR-04</b>: Creates issues for MISSING or PRESENT_DAMAGED items</li>
     *   <li><b>BR-05</b>: Validates notes for significant quantity discrepancies</li>
     * </ul>
     *
     * @param request the verification details
     * @param performedBy the user performing the verification
     * @throws IllegalArgumentException if the check is not found or not in progress
     * @throws ItemAlreadyVerifiedException if the item has already been verified
     * @throws QuantityDiscrepancyRequiresNotesException if >20% discrepancy without notes
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void verifyItem(ItemVerificationRequest request, UserId performedBy) {
        stationAccess.requireInventoryCheckAccess(request.checkId());

        // Load in-progress check from repository
        var inProgressCheck = inventoryCheckRepository.findInProgressById(request.checkId())
            .orElseThrow(() -> new IllegalArgumentException("Inventory check not found: " + request.checkId()));

        // Check if already verified using repository
        if (request.equipmentItemId() != null) {
            if (inventoryCheckItemRepository.existsByCheckIdAndEquipmentItemId(
                    request.checkId(), request.equipmentItemId())) {
                throw new ItemAlreadyVerifiedException("This item has already been verified in this check");
            }
        } else if (request.consumableStockId() != null) {
            if (inventoryCheckItemRepository.existsByCheckIdAndConsumableStockId(
                    request.checkId(), request.consumableStockId())) {
                throw new ItemAlreadyVerifiedException("This item has already been verified in this check");
            }
        }

        // BR-05: Check for quantity discrepancy requiring notes (consumables)
        if (request.isConsumable() && request.quantityFound() != null && request.quantityExpected() != null) {
            var found = request.quantityFound().value();
            var expected = request.quantityExpected().value();
            if (expected.signum() > 0) {
                var discrepancy = expected.subtract(found).abs()
                    .divide(expected, 2, java.math.RoundingMode.HALF_UP);
                if (discrepancy.doubleValue() > 0.20 &&
                    (request.conditionNotes() == null || request.conditionNotes().isBlank())) {
                    throw new QuantityDiscrepancyRequiresNotesException(
                        "A quantity discrepancy greater than 20% requires condition notes");
                }
            }
        }

        // BR-04: Create issue if needed
        IssueId issueId = null;
        if (request.requiresIssue()) {
            issueId = createIssueForVerification(request, inProgressCheck, performedBy);
        }

        // Update equipment status if damaged or missing (only for equipment, not consumables)
        if (request.equipmentItemId() != null) {
            if (request.status() == VerificationStatus.MISSING ||
                request.status() == VerificationStatus.PRESENT_DAMAGED) {
                var equipmentItem = equipmentItemRepository.findById(request.equipmentItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment item not found: " + request.equipmentItemId()));
                var newStatus = request.status() == VerificationStatus.MISSING
                    ? EquipmentStatus.MISSING
                    : EquipmentStatus.DAMAGED;
                var updatedEquipment = equipmentItem.withStatus(newStatus);
                equipmentItemRepository.save(updatedEquipment);
            }
        }

        // Create and save check item using domain model
        var target = request.equipmentItemId() != null
            ? new EquipmentCheckTarget(request.equipmentItemId())
            : new ConsumableCheckTarget(request.consumableStockId());
        var checkItem = new InventoryCheckItem(
            InventoryCheckItemId.generate(),
            request.checkId(),
            target,
            request.compartmentId(),
            request.manifestEntryId(),
            request.status(),
            request.quantityFound(),
            request.quantityExpected(),
            request.conditionNotes(),
            clock.instant(),
            issueId
        );
        inventoryCheckItemRepository.save(checkItem);

        // Update check progress using domain model
        boolean hasIssue = issueId != null;
        var updatedCheck = inProgressCheck.withItemVerified(hasIssue, clock.instant());
        inventoryCheckRepository.save(updatedCheck);
    }

    private IssueId createIssueForVerification(ItemVerificationRequest request,
            InProgressCheck check, UserId reportedBy) {

        String title;
        IssueCategory category;
        IssueSeverity severity;

        switch (request.status()) {
            case MISSING -> {
                title = "Missing item found during inventory check";
                category = IssueCategory.MISSING;
                severity = IssueSeverity.HIGH;
            }
            case PRESENT_DAMAGED -> {
                title = "Damaged item found during inventory check";
                category = IssueCategory.DAMAGE;
                severity = IssueSeverity.MEDIUM;
            }
            case EXPIRED -> {
                title = "Expired item found during inventory check";
                category = IssueCategory.EXPIRED;
                severity = IssueSeverity.MEDIUM;
            }
            default -> {
                return null;
            }
        }

        String description = request.conditionNotes() != null ?
            request.conditionNotes() :
            "Issue automatically created during shift inventory check.";

        // Create the issue target based on what type of item this is
        IssueTarget target;
        if (request.equipmentItemId() != null) {
            target = new EquipmentIssueTarget(request.equipmentItemId());
        } else if (request.consumableStockId() != null) {
            target = new ConsumableIssueTarget(request.consumableStockId());
        } else {
            // Apparatus-level issue
            target = new com.example.firestock.domain.issue.ApparatusIssueTarget();
        }

        OpenIssue issue = issueRepository.createIssue(
            target,
            check.apparatusId(),
            check.stationId(),
            title,
            description,
            severity,
            category,
            reportedBy,
            clock.instant(),
            false
        );

        return issue.id();
    }

    /**
     * Completes an inventory check.
     *
     * <p>Enforces <b>BR-02</b>: All items must be verified before completion.
     *
     * @param checkId the check to complete
     * @return the updated check summary
     * @throws IllegalArgumentException if the check is not found or not in progress
     * @throws IncompleteCheckException if not all items have been verified
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public InventoryCheckSummary completeCheck(InventoryCheckId checkId) {
        stationAccess.requireInventoryCheckAccess(checkId);

        var inProgressCheck = inventoryCheckRepository.findInProgressById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory check not found: " + checkId));

        // BR-02 validation is done inside the domain model's complete() method
        // which validates progress.isAllVerified()
        try {
            var completedCheck = inProgressCheck.complete(clock.instant());
            inventoryCheckRepository.save(completedCheck);
        } catch (InventoryCheckException.IncompleteCheckException e) {
            throw new IncompleteCheckException(
                String.format("Cannot complete check: only %d of %d items verified",
                    inProgressCheck.progress().verifiedCount(),
                    inProgressCheck.progress().totalItems()));
        }

        return inventoryCheckQuery.findById(checkId)
            .orElseThrow(() -> new IllegalStateException("Failed to retrieve completed check"));
    }

    /**
     * Abandons an inventory check without completing it.
     *
     * @param checkId the check to abandon
     * @throws IllegalArgumentException if the check is not found or not in progress
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void abandonCheck(InventoryCheckId checkId) {
        stationAccess.requireInventoryCheckAccess(checkId);

        var inProgressCheck = inventoryCheckRepository.findInProgressById(checkId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory check not found: " + checkId));

        var abandonedCheck = inProgressCheck.abandon(null, clock.instant());
        inventoryCheckRepository.save(abandonedCheck);
    }

    /**
     * Finds an item by barcode scan within an apparatus.
     *
     * @param barcode the scanned barcode
     * @param apparatusId the apparatus to search within
     * @return the matching item, or empty if not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public Optional<CheckableItem> findByBarcode(Barcode barcode, ApparatusId apparatusId) {
        stationAccess.requireApparatusAccess(apparatusId);
        return equipmentQuery.findByBarcode(barcode, apparatusId);
    }

    /**
     * Exception thrown when attempting to start a check when one already exists.
     */
    public static class ActiveCheckExistsException extends RuntimeException {
        public ActiveCheckExistsException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when attempting to complete a check before all items are verified.
     */
    public static class IncompleteCheckException extends RuntimeException {
        public IncompleteCheckException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when an item has already been verified in this check.
     */
    public static class ItemAlreadyVerifiedException extends RuntimeException {
        public ItemAlreadyVerifiedException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when a quantity discrepancy requires notes but none provided.
     */
    public static class QuantityDiscrepancyRequiresNotesException extends RuntimeException {
        public QuantityDiscrepancyRequiresNotesException(String message) {
            super(message);
        }
    }
}
