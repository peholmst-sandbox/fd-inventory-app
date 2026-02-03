package com.example.firestock.audit;

import com.example.firestock.domain.audit.AuditException;
import com.example.firestock.domain.audit.AuditItemStatus;
import com.example.firestock.domain.audit.FormalAuditItem;
import com.example.firestock.domain.audit.FormalAuditItemRepository;
import com.example.firestock.domain.audit.FormalAuditRepository;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.issues.IssueDao;
import com.example.firestock.jooq.enums.EquipmentStatus;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.security.StationAccessEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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
    private final FormalAuditRepository auditRepository;
    private final FormalAuditItemRepository auditItemRepository;
    private final IssueDao issueDao;
    private final AuditEquipmentDao auditEquipmentDao;
    private final StationAccessEvaluator stationAccess;
    private final Clock clock;

    public FormalAuditService(
            FormalAuditQuery auditQuery,
            FormalAuditRepository auditRepository,
            FormalAuditItemRepository auditItemRepository,
            IssueDao issueDao,
            AuditEquipmentDao auditEquipmentDao,
            StationAccessEvaluator stationAccess,
            Clock clock) {
        this.auditQuery = auditQuery;
        this.auditRepository = auditRepository;
        this.auditItemRepository = auditItemRepository;
        this.issueDao = issueDao;
        this.auditEquipmentDao = auditEquipmentDao;
        this.stationAccess = stationAccess;
        this.clock = clock;
    }

    private Instant now() {
        return clock.instant();
    }

    /**
     * Gets the list of all apparatus across all stations with their audit status.
     * For maintenance technicians who have cross-station access.
     *
     * @return list of all apparatus with audit info
     */
    @Transactional(readOnly = true)
    public List<ApparatusAuditInfo> getAllApparatus() {
        return auditQuery.findAllApparatusWithAuditInfo();
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
     * @throws AuditException.ActiveAuditExistsException if an active audit already exists for this apparatus
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public AuditSummary startAudit(ApparatusId apparatusId, UserId performedBy) {
        stationAccess.requireApparatusAccess(apparatusId);

        // BR-02: Check for existing active audit
        var existingAudit = auditRepository.findActiveByApparatusId(apparatusId);
        if (existingAudit.isPresent()) {
            throw new AuditException.ActiveAuditExistsException(apparatusId, existingAudit.get().id());
        }

        // Count items on apparatus for progress tracking
        int totalItems = countItemsForApparatus(apparatusId);

        // Create new audit using the domain model
        var auditId = FormalAuditId.generate();
        var now = now();
        var audit = InProgressAudit.start(auditId, apparatusId, performedBy, now, totalItems);

        auditRepository.save(audit);

        return auditQuery.findById(auditId)
                .orElseThrow(() -> new IllegalStateException("Failed to create formal audit"));
    }

    private int countItemsForApparatus(ApparatusId apparatusId) {
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
        return auditRepository.findActiveByApparatusId(apparatusId)
                .map(this::toAuditSummary);
    }

    private AuditSummary toAuditSummary(InProgressAudit audit) {
        return new AuditSummary(
                audit.id(),
                audit.apparatusId(),
                com.example.firestock.jooq.enums.AuditStatus.IN_PROGRESS,
                java.time.LocalDateTime.ofInstant(audit.startedAt(), java.time.ZoneId.systemDefault()),
                null,
                audit.pausedAt() != null ? java.time.LocalDateTime.ofInstant(audit.pausedAt(), java.time.ZoneId.systemDefault()) : null,
                audit.progress().totalItems(),
                audit.progress().auditedCount(),
                audit.progress().issuesFoundCount(),
                audit.progress().unexpectedItemsCount()
        );
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
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is not in progress
     * @throws AuditException.ItemAlreadyAuditedException if the item has already been audited
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void auditItem(AuditItemRequest request, UserId performedBy) {
        stationAccess.requireFormalAuditAccess(request.auditId());

        // Validate the audit exists and is in progress
        var audit = auditRepository.findInProgressById(request.auditId())
                .orElseThrow(() -> {
                    // Check if it exists but is not in progress
                    if (!auditRepository.existsById(request.auditId())) {
                        return new AuditException.AuditNotFoundException(request.auditId());
                    }
                    return new AuditException.AuditAlreadyCompletedException(request.auditId());
                });

        // Check if already audited
        boolean alreadyAudited = request.equipmentItemId() != null
                ? auditItemRepository.existsByAuditIdAndEquipmentItemId(request.auditId(), request.equipmentItemId())
                : auditItemRepository.existsByAuditIdAndConsumableStockId(request.auditId(), request.consumableStockId());

        if (alreadyAudited) {
            throw new AuditException.ItemAlreadyAuditedException(request.auditId(), request.toTarget());
        }

        // Map request status to domain status
        var domainStatus = mapToDomainStatus(request.status());

        // BR-05: Create issue if needed
        IssueId issueId = null;
        if (domainStatus.requiresIssue()) {
            issueId = createIssueForAudit(request, audit, performedBy);
        }

        // Update equipment status if needed
        if (request.equipmentItemId() != null) {
            EquipmentStatus newStatus = mapAuditStatusToEquipmentStatus(domainStatus);
            if (newStatus != null) {
                auditEquipmentDao.updateStatus(request.equipmentItemId(), newStatus);
            }
        }

        // Create and save the audit item
        var auditItemId = FormalAuditItemId.generate();

        var auditItem = new FormalAuditItem(
                auditItemId,
                request.auditId(),
                request.toTarget(),
                request.compartmentId(),
                request.manifestEntryId(),
                request.isUnexpected(),
                domainStatus,
                mapToDomainCondition(request.condition()),
                mapToDomainTestResult(request.testResult()),
                mapToDomainExpiryStatus(request.expiryStatus()),
                request.quantityFound() != null && request.quantityExpected() != null
                        ? new com.example.firestock.domain.audit.QuantityComparison(request.quantityExpected(), request.quantityFound())
                        : null,
                request.conditionNotes(),
                now()
        );

        auditItemRepository.save(auditItem);

        // Update audit progress
        var updatedAudit = request.isUnexpected()
                ? audit.withUnexpectedItem(issueId != null, now())
                : audit.withItemAudited(issueId != null, now());

        auditRepository.save(updatedAudit);
    }

    private AuditItemStatus mapToDomainStatus(com.example.firestock.jooq.enums.AuditItemStatus status) {
        return AuditItemStatus.valueOf(status.name());
    }

    private com.example.firestock.domain.audit.ItemCondition mapToDomainCondition(com.example.firestock.jooq.enums.ItemCondition condition) {
        return condition == null ? null : com.example.firestock.domain.audit.ItemCondition.valueOf(condition.name());
    }

    private com.example.firestock.domain.audit.TestResult mapToDomainTestResult(com.example.firestock.jooq.enums.TestResult testResult) {
        return testResult == null ? null : com.example.firestock.domain.audit.TestResult.valueOf(testResult.name());
    }

    private com.example.firestock.domain.audit.ExpiryStatus mapToDomainExpiryStatus(com.example.firestock.jooq.enums.ExpiryStatus expiryStatus) {
        return expiryStatus == null ? null : com.example.firestock.domain.audit.ExpiryStatus.valueOf(expiryStatus.name());
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
                                        InProgressAudit audit,
                                        UserId reportedBy) {
        String title;
        IssueCategory category;
        IssueSeverity severity;

        var status = mapToDomainStatus(request.status());
        switch (status) {
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

        var stationId = auditQuery.getStationIdForAudit(request.auditId());

        return issueDao.insert(
                request.equipmentItemId(),
                request.consumableStockId(),
                audit.apparatusId(),
                stationId,
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
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is not in progress
     * @throws AuditException.IncompleteAuditException if not all items have been audited
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public AuditSummary completeAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditRepository.findInProgressById(auditId)
                .orElseThrow(() -> {
                    if (!auditRepository.existsById(auditId)) {
                        return new AuditException.AuditNotFoundException(auditId);
                    }
                    return new AuditException.AuditAlreadyCompletedException(auditId);
                });

        // BR-03: complete() enforces all items must be audited
        var completedAudit = audit.complete(now());
        auditRepository.save(completedAudit);

        return auditQuery.findById(auditId)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve completed audit"));
    }

    /**
     * Abandons a formal audit without completing it.
     *
     * @param auditId the audit to abandon
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is not in progress
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void abandonAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditRepository.findInProgressById(auditId)
                .orElseThrow(() -> {
                    if (!auditRepository.existsById(auditId)) {
                        return new AuditException.AuditNotFoundException(auditId);
                    }
                    return new AuditException.AuditAlreadyCompletedException(auditId);
                });

        var abandonedAudit = audit.abandon(null, now());
        auditRepository.save(abandonedAudit);
    }

    /**
     * Saves and exits an audit for later resumption.
     * The audit remains IN_PROGRESS but is marked as paused.
     *
     * @param auditId the audit to save
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is not in progress
     * @throws AuditException.AuditAlreadyPausedException if the audit is already paused
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void saveAndExit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditRepository.findInProgressById(auditId)
                .orElseThrow(() -> {
                    if (!auditRepository.existsById(auditId)) {
                        return new AuditException.AuditNotFoundException(auditId);
                    }
                    return new AuditException.AuditAlreadyCompletedException(auditId);
                });

        var pausedAudit = audit.pause(now());
        auditRepository.save(pausedAudit);
    }

    /**
     * Resumes a paused audit.
     *
     * @param auditId the audit to resume
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is not in progress
     * @throws AuditException.AuditNotPausedException if the audit is not paused
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void resumeAudit(FormalAuditId auditId) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditRepository.findInProgressById(auditId)
                .orElseThrow(() -> {
                    if (!auditRepository.existsById(auditId)) {
                        return new AuditException.AuditNotFoundException(auditId);
                    }
                    return new AuditException.AuditAlreadyCompletedException(auditId);
                });

        var resumedAudit = audit.resume(now());
        auditRepository.save(resumedAudit);
    }

    /**
     * Updates the notes for an audit.
     *
     * <p>Notes can only be updated on in-progress audits per BR-07.
     *
     * @param auditId the audit to update
     * @param notes the notes to save
     * @throws AuditException.AuditNotFoundException if the audit is not found
     * @throws AuditException.AuditAlreadyCompletedException if the audit is completed
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access
     */
    public void updateNotes(FormalAuditId auditId, String notes) {
        stationAccess.requireFormalAuditAccess(auditId);

        var audit = auditRepository.findInProgressById(auditId)
                .orElseThrow(() -> {
                    // BR-07: Completed audits are read-only
                    if (!auditRepository.existsById(auditId)) {
                        return new AuditException.AuditNotFoundException(auditId);
                    }
                    return new AuditException.AuditAlreadyCompletedException(auditId);
                });

        var updatedAudit = audit.withNotes(notes);
        auditRepository.save(updatedAudit);
    }
}
