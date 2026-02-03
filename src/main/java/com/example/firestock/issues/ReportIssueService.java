package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.jooq.enums.EquipmentStatus;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.security.FirestockUserDetails;
import com.example.firestock.security.StationAccessEvaluator;
import org.jooq.DSLContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.EQUIPMENT_ITEM;

/**
 * Service for UC-02: Report Equipment Issue.
 *
 * <p>This service provides operations for users to report equipment issues
 * (damaged, missing, malfunctioning) outside of routine inventory checks.
 *
 * <p><b>Security:</b> Authentication is required. Access control enforces:
 * <ul>
 *   <li><b>BR-04</b>: Firefighters can only report for their station</li>
 *   <li><b>BR-05</b>: Maintenance can report for any equipment</li>
 * </ul>
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><b>BR-01</b>: Description minimum 10 characters</li>
 *   <li><b>BR-02</b>: Critical severity requires confirmation</li>
 *   <li><b>BR-03</b>: Equipment status auto-updates when issue is created</li>
 * </ul>
 */
@Service
@Transactional
@PreAuthorize("isAuthenticated()")
public class ReportIssueService {

    private final EquipmentLookupQuery equipmentLookupQuery;
    private final IssueQuery issueQuery;
    private final IssueDao issueDao;
    private final StationAccessEvaluator stationAccess;
    private final DSLContext create;

    public ReportIssueService(
            EquipmentLookupQuery equipmentLookupQuery,
            IssueQuery issueQuery,
            IssueDao issueDao,
            StationAccessEvaluator stationAccess,
            DSLContext create) {
        this.equipmentLookupQuery = equipmentLookupQuery;
        this.issueQuery = issueQuery;
        this.issueDao = issueDao;
        this.stationAccess = stationAccess;
        this.create = create;
    }

    /**
     * Gets equipment details for the report form with access control.
     *
     * @param id the equipment item ID
     * @return the equipment details
     * @throws EquipmentNotFoundException if equipment not found
     * @throws AccessDeniedException if user does not have access to the equipment's station
     */
    @Transactional(readOnly = true)
    public EquipmentForReport getEquipmentForReport(EquipmentItemId id) {
        EquipmentForReport equipment = equipmentLookupQuery.getEquipmentDetails(id)
                .orElseThrow(() -> new EquipmentNotFoundException("Equipment not found: " + id));

        // BR-04/BR-05: Check station access
        requireEquipmentAccess(equipment);

        return equipment;
    }

    /**
     * Finds equipment by barcode.
     *
     * @param barcode the barcode to search for
     * @return the equipment, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<EquipmentForReport> findByBarcode(Barcode barcode) {
        return equipmentLookupQuery.findByBarcode(barcode);
    }

    /**
     * Finds equipment by serial number.
     *
     * @param serialNumber the serial number to search for
     * @return the equipment, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<EquipmentForReport> findBySerialNumber(SerialNumber serialNumber) {
        return equipmentLookupQuery.findBySerialNumber(serialNumber);
    }

    /**
     * Gets any open issues for the equipment.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of open issue summaries
     */
    @Transactional(readOnly = true)
    public List<IssueSummary> getOpenIssues(EquipmentItemId equipmentItemId) {
        return issueQuery.findOpenIssuesByEquipmentItemId(equipmentItemId);
    }

    /**
     * Reports a new issue for equipment.
     *
     * <p>Enforces business rules:
     * <ul>
     *   <li>BR-01: Description min 10 characters</li>
     *   <li>BR-02: Critical severity requires confirmation</li>
     *   <li>BR-03: Equipment status auto-updates</li>
     *   <li>BR-04: Firefighters can only report for their station</li>
     *   <li>BR-05: Maintenance can report for any equipment</li>
     * </ul>
     *
     * @param request the issue report request
     * @param reportedBy the user reporting the issue
     * @return the result containing issue ID, reference number, and updated equipment status
     * @throws EquipmentNotFoundException if equipment not found
     * @throws AccessDeniedException if user does not have access
     * @throws IllegalArgumentException if validation fails
     */
    public IssueCreatedResult.WithEquipmentStatus reportIssue(ReportIssueRequest request, UserId reportedBy) {
        // Validate request (BR-01, BR-02)
        request.validate();

        // Get equipment details
        EquipmentForReport equipment = equipmentLookupQuery.getEquipmentDetails(request.equipmentItemId())
                .orElseThrow(() -> new EquipmentNotFoundException(
                        "Equipment not found: " + request.equipmentItemId()));

        // BR-04/BR-05: Check station access
        requireEquipmentAccess(equipment);

        // Create the issue
        IssueCreatedResult result = issueDao.insertAndReturn(
                request.equipmentItemId(),
                null, // consumableStockId
                equipment.apparatusId(),
                equipment.stationId(),
                request.generateTitle(),
                request.description(),
                request.severity(),
                request.category(),
                reportedBy,
                false // not a crew responsibility
        );

        // BR-03: Update equipment status based on issue category
        EquipmentStatus newStatus = mapCategoryToStatus(request.category());
        if (newStatus != null && newStatus != equipment.status()) {
            updateEquipmentStatus(request.equipmentItemId(), newStatus);
            return new IssueCreatedResult.WithEquipmentStatus(
                    result.issueId(),
                    result.referenceNumber(),
                    newStatus
            );
        }

        return new IssueCreatedResult.WithEquipmentStatus(
                result.issueId(),
                result.referenceNumber(),
                null
        );
    }

    /**
     * Adds a note to an existing issue.
     * This could be used when user chooses to add to an existing issue
     * instead of creating a new one.
     *
     * @param issueId the existing issue ID
     * @param notes additional notes to add
     * @param addedBy the user adding the notes
     * @throws IssueNotFoundException if issue not found
     */
    public void addToExistingIssue(IssueId issueId, String notes, UserId addedBy) {
        // For v1, we'll append notes to the description
        // A more complete implementation would use a separate comments table
        IssueSummary issue = issueQuery.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue not found: " + issueId));

        // Append the notes to the existing description
        String timestamp = java.time.Instant.now().toString();
        String additionalNotes = String.format("\n\n--- Additional notes (%s) ---\n%s", timestamp, notes);

        create.update(com.example.firestock.jooq.Tables.ISSUE)
                .set(com.example.firestock.jooq.Tables.ISSUE.DESCRIPTION,
                        com.example.firestock.jooq.Tables.ISSUE.DESCRIPTION.concat(additionalNotes))
                .set(com.example.firestock.jooq.Tables.ISSUE.UPDATED_AT, java.time.Instant.now())
                .where(com.example.firestock.jooq.Tables.ISSUE.ID.eq(issueId))
                .execute();
    }

    /**
     * Maps issue category to the appropriate equipment status.
     */
    private EquipmentStatus mapCategoryToStatus(IssueCategory category) {
        return switch (category) {
            case DAMAGE -> EquipmentStatus.DAMAGED;
            case MISSING -> EquipmentStatus.MISSING;
            case MALFUNCTION -> EquipmentStatus.DAMAGED;
            case EXPIRED -> EquipmentStatus.EXPIRED;
            default -> null; // Other categories don't change status
        };
    }

    /**
     * Updates the equipment status.
     */
    private void updateEquipmentStatus(EquipmentItemId id, EquipmentStatus status) {
        create.update(EQUIPMENT_ITEM)
                .set(EQUIPMENT_ITEM.STATUS, status)
                .where(EQUIPMENT_ITEM.ID.eq(id))
                .execute();
    }

    /**
     * Checks if the current user has access to the equipment's station.
     * BR-04: Firefighters can only report for their station
     * BR-05: Maintenance can report for any equipment
     */
    private void requireEquipmentAccess(EquipmentForReport equipment) {
        FirestockUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new AccessDeniedException("Authentication required");
        }

        StationId stationId = equipment.stationId();
        if (stationId != null && !userDetails.hasAccessToStation(stationId)) {
            throw new AccessDeniedException(
                    "You do not have access to report issues for equipment at " + equipment.stationName());
        }
    }

    private FirestockUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FirestockUserDetails) {
            return (FirestockUserDetails) principal;
        }
        return null;
    }

    /**
     * Exception thrown when equipment is not found.
     */
    public static class EquipmentNotFoundException extends RuntimeException {
        public EquipmentNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when an issue is not found.
     */
    public static class IssueNotFoundException extends RuntimeException {
        public IssueNotFoundException(String message) {
            super(message);
        }
    }
}
