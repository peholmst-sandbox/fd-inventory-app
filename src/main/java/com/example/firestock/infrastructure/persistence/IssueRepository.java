package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.issue.ClosedIssue;
import com.example.firestock.domain.issue.Issue;
import com.example.firestock.domain.issue.IssueTarget;
import com.example.firestock.domain.issue.OpenIssue;
import com.example.firestock.domain.issue.ResolvedIssue;
import com.example.firestock.domain.issue.ApparatusIssueTarget;
import com.example.firestock.domain.issue.ConsumableIssueTarget;
import com.example.firestock.domain.issue.EquipmentIssueTarget;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.infrastructure.persistence.mapper.IssueMapper;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.IssueStatus;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.ISSUE;

/**
 * Repository for {@link Issue} aggregate persistence.
 *
 * <p>Handles storage and retrieval of issues across all states
 * (open, acknowledged, in-progress, resolved, closed). The repository works with domain types
 * and handles the sealed interface hierarchy by mapping based on the status field.
 *
 * <h3>Aggregate Boundaries</h3>
 * <p>Issue is an aggregate root. The repository manages:
 * <ul>
 *   <li>The issue entity itself (all states)</li>
 *   <li>Transactional consistency for state transitions</li>
 *   <li>Reference number generation</li>
 * </ul>
 *
 * <h3>State Handling</h3>
 * <p>Methods like {@link #save(Issue)} accept any issue state and persist
 * appropriately. The sealed interface hierarchy ensures type safety.
 */
@Repository
public class IssueRepository {

    private final DSLContext create;
    private final IssueMapper mapper;

    public IssueRepository(DSLContext create, IssueMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Factory Methods for Creating New Issues
    // ========================================================================

    /**
     * Creates a new open issue with an auto-generated reference number.
     *
     * @param target the issue target (equipment, consumable, or apparatus-level)
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station where the issue was found
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedById the user reporting the issue
     * @param reportedAt when the issue was reported
     * @param isCrewResponsibility whether this is a crew responsibility
     * @return the created open issue
     */
    public OpenIssue createIssue(
            IssueTarget target,
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedById,
            Instant reportedAt,
            boolean isCrewResponsibility
    ) {
        var id = IssueId.generate();
        var referenceNumber = generateReferenceNumber();

        var issue = OpenIssue.create(
                id,
                referenceNumber,
                target,
                apparatusId,
                stationId,
                title,
                description,
                severity,
                category,
                reportedById,
                reportedAt,
                isCrewResponsibility
        );

        save(issue);
        return issue;
    }

    /**
     * Creates a new open issue for an equipment item with an auto-generated reference number.
     *
     * @param equipmentItemId the equipment item ID
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station where the issue was found
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedById the user reporting the issue
     * @param reportedAt when the issue was reported
     * @param isCrewResponsibility whether this is a crew responsibility
     * @return the created open issue
     */
    public OpenIssue createEquipmentIssue(
            EquipmentItemId equipmentItemId,
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedById,
            Instant reportedAt,
            boolean isCrewResponsibility
    ) {
        return createIssue(
                new EquipmentIssueTarget(equipmentItemId),
                apparatusId,
                stationId,
                title,
                description,
                severity,
                category,
                reportedById,
                reportedAt,
                isCrewResponsibility
        );
    }

    /**
     * Creates a new open issue for a consumable stock entry with an auto-generated reference number.
     *
     * @param consumableStockId the consumable stock ID
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station where the issue was found
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedById the user reporting the issue
     * @param reportedAt when the issue was reported
     * @param isCrewResponsibility whether this is a crew responsibility
     * @return the created open issue
     */
    public OpenIssue createConsumableIssue(
            ConsumableStockId consumableStockId,
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedById,
            Instant reportedAt,
            boolean isCrewResponsibility
    ) {
        return createIssue(
                new ConsumableIssueTarget(consumableStockId),
                apparatusId,
                stationId,
                title,
                description,
                severity,
                category,
                reportedById,
                reportedAt,
                isCrewResponsibility
        );
    }

    /**
     * Creates a new open issue at the apparatus level with an auto-generated reference number.
     *
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station where the issue was found
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedById the user reporting the issue
     * @param reportedAt when the issue was reported
     * @param isCrewResponsibility whether this is a crew responsibility
     * @return the created open issue
     */
    public OpenIssue createApparatusIssue(
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedById,
            Instant reportedAt,
            boolean isCrewResponsibility
    ) {
        return createIssue(
                new ApparatusIssueTarget(),
                apparatusId,
                stationId,
                title,
                description,
                severity,
                category,
                reportedById,
                reportedAt,
                isCrewResponsibility
        );
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves an issue (insert or update based on existence).
     *
     * <p>This method handles all issue states. The implementation:
     * <ul>
     *   <li>Inserts if the issue doesn't exist</li>
     *   <li>Updates if the issue exists</li>
     *   <li>Persists state-specific fields appropriately</li>
     * </ul>
     *
     * @param issue the issue to save (any state)
     * @return the saved issue
     */
    public Issue save(Issue issue) {
        var record = create.newRecord(ISSUE);
        mapper.updateRecord(record, issue);

        if (existsById(issue.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return issue;
    }

    /**
     * Finds an issue by its ID.
     *
     * <p>Returns the issue in its current state.
     *
     * @param id the issue ID
     * @return the issue, or empty if not found
     */
    public Optional<Issue> findById(IssueId id) {
        return create.selectFrom(ISSUE)
                .where(ISSUE.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an issue with the given ID exists.
     *
     * @param id the issue ID
     * @return true if the issue exists
     */
    public boolean existsById(IssueId id) {
        return create.fetchExists(
                create.selectFrom(ISSUE)
                        .where(ISSUE.ID.eq(id))
        );
    }

    /**
     * Deletes an issue by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the issue and should only be used
     * for cleanup of test data or administrative purposes.
     *
     * @param id the issue ID to delete
     */
    public void deleteById(IssueId id) {
        create.deleteFrom(ISSUE)
                .where(ISSUE.ID.eq(id))
                .execute();
    }

    // ========================================================================
    // Query Operations
    // ========================================================================

    /**
     * Finds all open issues for an equipment item.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of open issues
     */
    public List<OpenIssue> findOpenByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectFrom(ISSUE)
                .where(ISSUE.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .and(ISSUE.STATUS.eq(IssueStatus.OPEN))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(OpenIssue.class::isInstance)
                .map(OpenIssue.class::cast)
                .toList();
    }

    /**
     * Finds all issues for an equipment item.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of all issues
     */
    public List<Issue> findByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectFrom(ISSUE)
                .where(ISSUE.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .orderBy(ISSUE.REPORTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all issues for a station.
     *
     * @param stationId the station ID
     * @return list of all issues
     */
    public List<Issue> findByStationId(StationId stationId) {
        return create.selectFrom(ISSUE)
                .where(ISSUE.STATION_ID.eq(stationId))
                .orderBy(ISSUE.REPORTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all active (non-terminal) issues.
     *
     * @return list of active issues
     */
    public List<Issue> findAllActive() {
        return create.selectFrom(ISSUE)
                .where(ISSUE.STATUS.notIn(IssueStatus.RESOLVED, IssueStatus.CLOSED))
                .orderBy(ISSUE.SEVERITY.desc(), ISSUE.REPORTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all issues by status.
     *
     * @param status the issue status
     * @return list of issues with the given status
     */
    public List<Issue> findByStatus(IssueStatus status) {
        return create.selectFrom(ISSUE)
                .where(ISSUE.STATUS.eq(status))
                .orderBy(ISSUE.REPORTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts open issues for an equipment item.
     *
     * @param equipmentItemId the equipment item ID
     * @return the count of open issues
     */
    public long countOpenByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectCount()
                .from(ISSUE)
                .where(ISSUE.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .and(ISSUE.STATUS.eq(IssueStatus.OPEN))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts active (non-terminal) issues for a station.
     *
     * @param stationId the station ID
     * @return the count of active issues
     */
    public long countActiveByStationId(StationId stationId) {
        return create.selectCount()
                .from(ISSUE)
                .where(ISSUE.STATION_ID.eq(stationId))
                .and(ISSUE.STATUS.notIn(IssueStatus.RESOLVED, IssueStatus.CLOSED))
                .fetchOne(0, Long.class);
    }

    // ========================================================================
    // Reference Number Generation
    // ========================================================================

    /**
     * Generates a unique reference number for an issue.
     * Format: ISS-YYYY-NNNNN (e.g., ISS-2026-00001)
     *
     * @return the generated reference number
     */
    ReferenceNumber generateReferenceNumber() {
        int currentYear = Year.now().getValue();
        String prefix = "ISS-" + currentYear + "-";

        // Find the highest sequence number for this year
        var maxRef = create.select(DSL.max(ISSUE.REFERENCE_NUMBER))
                .from(ISSUE)
                .where(ISSUE.REFERENCE_NUMBER.cast(String.class).like(prefix + "%"))
                .fetchOne(0, ReferenceNumber.class);

        int nextSeq = 1;
        if (maxRef != null) {
            String maxRefStr = maxRef.value();
            String seqPart = maxRefStr.substring(prefix.length());
            nextSeq = Integer.parseInt(seqPart) + 1;
        }

        return new ReferenceNumber(prefix + String.format("%05d", nextSeq));
    }
}
