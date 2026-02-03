package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.issues.IssueDao;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.AuditStatus;
import com.example.firestock.jooq.enums.EquipmentStatus;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.security.StationAccessEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for UC-03: Conduct Formal Audit.
 *
 * <p>This service provides operations for maintenance technicians to perform
 * comprehensive formal audits on fire apparatus, including functional tests,
 * condition assessments, and expiry tracking.
 *
 * <p><b>Security:</b> This service is restricted to users with the MAINTENANCE_TECHNICIAN role.
 *
 * <h3>Business Rules:</h3>
 * <ul>
 *   <li><b>BR-01</b>: Only maintenance technicians can conduct formal audits</li>
 *   <li><b>BR-02</b>: Only one active (IN_PROGRESS) audit per apparatus at a time</li>
 *   <li><b>BR-03</b>: An audit cannot be completed until all items are audited</li>
 *   <li><b>BR-04</b>: Audits more than 7 days old are flagged as stale</li>
 *   <li><b>BR-05</b>: Items with FAILED_INSPECTION, MISSING, DAMAGED, or EXPIRED status auto-create issues</li>
 *   <li><b>BR-07</b>: Completed audits are read-only</li>
 * </ul>
 */
@Service
@Transactional
@PreAuthorize("hasRole('MAINTENANCE_TECHNICIAN')")
public class FormalAuditService {

    private final FormalAuditQuery auditQuery;
    private final FormalAuditDao auditDao;
    private final FormalAuditItemDao auditItemDao;
    private final IssueDao issueDao;
    private final AuditEquipmentDao auditEquipmentDao;
    private final StationAccessEvaluator stationAccess;

    public FormalAuditService(
            FormalAuditQuery auditQuery,
            FormalAuditDao auditDao,
            FormalAuditItemDao auditItemDao,
            IssueDao issueDao,
            AuditEquipmentDao auditEquipmentDao,
            StationAccessEvaluator stationAccess) {
        this.auditQuery = auditQuery;
        this.auditDao = auditDao;
        this.auditItemDao = auditItemDao;
        this.issueDao = issueDao;
        this.auditEquipmentDao = auditEquipmentDao;
        this.stationAccess = stationAccess;
    }

    /**
     * Gets the list of apparatus at a station with their audit status.
     *
     * @param stationId the station to get apparatus for
     * @return list of apparatus with audit info
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@stationAccess.canAccessStation(#stationId)")
    public List<ApparatusAuditInfo> getApparatusForStation(StationId stationId) {
        return auditQuery.findApparatusWithAuditInfoByStation(stationId);
    }

    /**
     * Starts a new formal audit for an apparatus.
     *
     * <p>Enforces <b>BR-02</b>: Only one active audit per apparatus.
     *
     * @param apparatusId the apparatus to audit
     * @param performedBy the user starting the audit
     * @return the created audit summary
     * @throws ActiveAuditExistsException if an active audit already exists for this apparatus
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public AuditSummary startAudit(ApparatusId apparatusId, UserId performedBy) {
        stationAccess.requireApparatusAccess(apparatusId);

        // BR-02: Check for existing active audit
        var existingAudit = auditQuery.findActiveByApparatusId(apparatusId);
        if (existingAudit.isPresent()) {
            throw new ActiveAuditExistsException(
                    "An active formal audit already exists for this apparatus. " +
                            "Complete or abandon the existing audit before starting a new one.");
        }

        // Get total item count by querying the apparatus
        var details = auditQuery.findDetailsById(null); // We need a different approach

        // Create a dummy audit to get item count
        var stationId = stationAccess.getStationIdForApparatus(apparatusId);
        int totalItems = countItemsForApparatus(apparatusId);

        FormalAuditId auditId = auditDao.insert(apparatusId, stationId, performedBy, totalItems);

        return auditQuery.findById(auditId)
                .orElseThrow(() -> new IllegalStateException("Failed to create formal audit"));
    }

    private int countItemsForApparatus(ApparatusId apparatusId) {
        // Use a temporary query to count items
        return auditEquipmentDao.countItemsOnApparatus(apparatusId);
    }

    /**
     * Gets an existing in-progress audit for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the active audit summary, or empty if no active audit
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public Optional<AuditSummary> getActiveAudit(ApparatusId apparatusId) {
        stationAccess.requireApparatusAccess(apparatusId);
        return auditQuery.findActiveByApparatusId(apparatusId)
                .map(r -> new AuditSummary(
                        r.getId(),
                        r.getApparatusId(),
                        r.getStatus(),
                        r.getStartedAt(),
                        r.getCompletedAt(),
                        r.getPausedAt(),
                        r.getTotalItems(),
                        r.getAuditedCount(),
                        r.getIssuesFoundCount(),
                        r.getUnexpectedItemsCount()
                ));
    }

    /**
     * Gets a formal audit by ID.
     *
     * @param auditId the audit ID
     * @return the audit summary
     * @throws IllegalArgumentException if the audit is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public AuditSummary getAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);
        return auditQuery.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));
    }

    /**
     * Gets the full details of a formal audit including compartments and items.
     *
     * @param auditId the audit ID
     * @return the audit details
     * @throws IllegalArgumentException if the audit is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    @Transactional(readOnly = true)
    public AuditDetails getAuditDetails(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);
        return auditQuery.findDetailsById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));
    }

    /**
     * Records an audit of an individual item.
     *
     * <p>Enforces <b>BR-05</b>: Creates issues for MISSING, DAMAGED, FAILED_INSPECTION, and EXPIRED items.
     *
     * @param request the audit details
     * @param performedBy the user performing the audit
     * @throws IllegalArgumentException if the audit is not found or not in progress
     * @throws ItemAlreadyAuditedException if the item has already been audited
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void auditItem(AuditItemRequest request, UserId performedBy) {
        stationAccess.requireFormalAuditAccess(request.auditId());

        // Validate the audit exists and is in progress
        var audit = auditQuery.findRecordById(request.auditId())
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + request.auditId()));

        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot audit items on an audit that is not in progress");
        }

        // Check if already audited
        if (auditItemDao.existsForItem(request.auditId(),
                request.equipmentItemId(), request.consumableStockId())) {
            throw new ItemAlreadyAuditedException("This item has already been audited in this formal audit");
        }

        // BR-05: Create issue if needed
        IssueId issueId = null;
        if (request.requiresIssue()) {
            issueId = createIssueForAudit(request, audit, performedBy);
        }

        // Update equipment status if needed
        if (request.equipmentItemId() != null) {
            EquipmentStatus newStatus = mapAuditStatusToEquipmentStatus(request.status());
            if (newStatus != null) {
                auditEquipmentDao.updateStatus(request.equipmentItemId(), newStatus);
            }
        }

        // Record the audit item
        auditItemDao.insert(
                request.auditId(),
                request.compartmentId(),
                request.equipmentItemId(),
                request.consumableStockId(),
                request.manifestEntryId(),
                request.status(),
                request.condition(),
                request.testResult(),
                request.expiryStatus(),
                request.conditionNotes(),
                request.testNotes(),
                request.quantityFound(),
                request.quantityExpected(),
                request.isUnexpected(),
                issueId
        );

        // Update audit counts
        auditDao.incrementCounts(request.auditId(), issueId != null, request.isUnexpected());
    }

    private EquipmentStatus mapAuditStatusToEquipmentStatus(AuditItemStatus auditStatus) {
        return switch (auditStatus) {
            case MISSING -> EquipmentStatus.MISSING;
            case DAMAGED -> EquipmentStatus.DAMAGED;
            case FAILED_INSPECTION -> EquipmentStatus.FAILED_INSPECTION;
            case EXPIRED -> EquipmentStatus.EXPIRED;
            default -> null;
        };
    }

    private IssueId createIssueForAudit(AuditItemRequest request,
                                        com.example.firestock.jooq.tables.records.FormalAuditRecord audit,
                                        UserId reportedBy) {
        String title;
        IssueCategory category;
        IssueSeverity severity;

        switch (request.status()) {
            case MISSING -> {
                title = "Missing item found during formal audit";
                category = IssueCategory.MISSING;
                severity = IssueSeverity.HIGH;
            }
            case DAMAGED -> {
                title = "Damaged item found during formal audit";
                category = IssueCategory.DAMAGE;
                severity = IssueSeverity.MEDIUM;
            }
            case FAILED_INSPECTION -> {
                title = "Item failed inspection during formal audit";
                category = IssueCategory.MALFUNCTION;
                severity = IssueSeverity.HIGH;
            }
            case EXPIRED -> {
                title = "Expired item found during formal audit";
                category = IssueCategory.EXPIRED;
                severity = IssueSeverity.MEDIUM;
            }
            default -> {
                return null;
            }
        }

        StringBuilder description = new StringBuilder();
        description.append("Issue automatically created during formal audit.\n\n");

        if (request.conditionNotes() != null && !request.conditionNotes().isBlank()) {
            description.append("Condition notes: ").append(request.conditionNotes()).append("\n");
        }
        if (request.testNotes() != null && !request.testNotes().isBlank()) {
            description.append("Test notes: ").append(request.testNotes()).append("\n");
        }
        if (request.condition() != null) {
            description.append("Condition: ").append(request.condition().getLiteral()).append("\n");
        }
        if (request.testResult() != null) {
            description.append("Test result: ").append(request.testResult().getLiteral()).append("\n");
        }

        return issueDao.insert(
                request.equipmentItemId(),
                request.consumableStockId(),
                audit.getApparatusId(),
                audit.getStationId(),
                title,
                description.toString(),
                severity,
                category,
                reportedBy,
                false
        );
    }

    /**
     * Completes a formal audit.
     *
     * <p>Enforces <b>BR-03</b>: All items must be audited before completion.
     *
     * @param auditId the audit to complete
     * @return the updated audit summary
     * @throws IllegalArgumentException if the audit is not found or not in progress
     * @throws IncompleteAuditException if not all items have been audited
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public AuditSummary completeAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditQuery.findRecordById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot complete an audit that is not in progress");
        }

        // BR-03: Verify all items have been audited
        int auditedCount = auditItemDao.countByAuditId(auditId);
        if (auditedCount < audit.getTotalItems()) {
            throw new IncompleteAuditException(
                    String.format("Cannot complete audit: only %d of %d items audited",
                            auditedCount, audit.getTotalItems()));
        }

        auditDao.markCompleted(auditId);

        return auditQuery.findById(auditId)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve completed audit"));
    }

    /**
     * Abandons a formal audit without completing it.
     *
     * @param auditId the audit to abandon
     * @throws IllegalArgumentException if the audit is not found or not in progress
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void abandonAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditQuery.findRecordById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot abandon an audit that is not in progress");
        }

        auditDao.markAbandoned(auditId);
    }

    /**
     * Saves and exits an audit for later resumption.
     * The audit remains IN_PROGRESS but is marked as paused.
     *
     * @param auditId the audit to save
     * @throws IllegalArgumentException if the audit is not found or not in progress
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void saveAndExit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditQuery.findRecordById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot save an audit that is not in progress");
        }

        auditDao.markPaused(auditId);
    }

    /**
     * Resumes a paused audit.
     *
     * @param auditId the audit to resume
     * @throws IllegalArgumentException if the audit is not found or not in progress
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void resumeAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditQuery.findRecordById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));

        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Cannot resume an audit that is not in progress");
        }

        auditDao.resume(auditId);
    }

    /**
     * Updates the notes for an audit.
     *
     * @param auditId the audit to update
     * @param notes the notes to save
     * @throws IllegalArgumentException if the audit is not found
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void updateNotes(FormalAuditId auditId, String notes) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditQuery.findRecordById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Formal audit not found: " + auditId));

        // BR-07: Completed audits are read-only
        if (audit.getStatus() == AuditStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify a completed audit");
        }

        auditDao.updateNotes(auditId, notes);
    }

    /**
     * Exception thrown when attempting to start an audit when one already exists.
     */
    public static class ActiveAuditExistsException extends RuntimeException {
        public ActiveAuditExistsException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when attempting to complete an audit before all items are audited.
     */
    public static class IncompleteAuditException extends RuntimeException {
        public IncompleteAuditException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when an item has already been audited in this audit.
     */
    public static class ItemAlreadyAuditedException extends RuntimeException {
        public ItemAlreadyAuditedException(String message) {
            super(message);
        }
    }
}
