package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.audit.AbandonedAudit;
import com.example.firestock.domain.audit.CompletedAudit;
import com.example.firestock.domain.audit.FormalAudit;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.AuditStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.APPARATUS;
import static com.example.firestock.jooq.Tables.FORMAL_AUDIT;

/**
 * Repository for {@link FormalAudit} aggregate persistence.
 *
 * <p>Handles storage and retrieval of formal audits across all states
 * (in-progress, completed, abandoned). The repository works with domain types and
 * handles the sealed interface hierarchy by mapping based on the status field.
 *
 * <h3>Aggregate Boundaries</h3>
 * <p>FormalAudit is an aggregate root. The repository manages:
 * <ul>
 *   <li>The audit entity itself (all states)</li>
 *   <li>Transactional consistency for state transitions</li>
 * </ul>
 *
 * <p>FormalAuditItem is handled by {@link FormalAuditItemRepository} as items may be
 * added incrementally during an audit session.
 *
 * <h3>State Handling</h3>
 * <p>Methods like {@link #save(FormalAudit)} accept any audit state and persist
 * appropriately. The sealed interface hierarchy ensures type safety:
 * <ul>
 *   <li>{@link InProgressAudit} - active, mutable audit</li>
 *   <li>{@link CompletedAudit} - terminal, read-only audit</li>
 *   <li>{@link AbandonedAudit} - terminal, cancelled audit</li>
 * </ul>
 */
@Repository
public class FormalAuditRepository {

    private final DSLContext create;
    private final FormalAuditMapper mapper;

    public FormalAuditRepository(DSLContext create, FormalAuditMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a formal audit (insert or update based on existence).
     *
     * <p>This method handles all audit states. The implementation:
     * <ul>
     *   <li>Inserts if the audit doesn't exist</li>
     *   <li>Updates if the audit exists</li>
     *   <li>Persists state-specific fields appropriately</li>
     * </ul>
     *
     * @param audit the audit to save (any state)
     * @return the saved audit
     */
    public FormalAudit save(FormalAudit audit) {
        var record = create.newRecord(FORMAL_AUDIT);
        mapper.updateRecord(record, audit);

        if (existsById(audit.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            // Look up station_id from apparatus (denormalized for query performance)
            var stationId = create.select(APPARATUS.STATION_ID)
                    .from(APPARATUS)
                    .where(APPARATUS.ID.eq(audit.apparatusId()))
                    .fetchOne(APPARATUS.STATION_ID);
            record.setStationId(stationId);
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return audit;
    }

    /**
     * Finds a formal audit by its ID.
     *
     * <p>Returns the audit in its current state (InProgressAudit, CompletedAudit,
     * or AbandonedAudit).
     *
     * @param id the audit ID
     * @return the audit, or empty if not found
     */
    public Optional<FormalAudit> findById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an audit with the given ID exists.
     *
     * @param id the audit ID
     * @return true if the audit exists
     */
    public boolean existsById(FormalAuditId id) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT)
                        .where(FORMAL_AUDIT.ID.eq(id))
        );
    }

    /**
     * Deletes a formal audit by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the audit and should only be used
     * for cleanup of test data or administrative purposes. Completed audits should
     * generally be retained for historical records.
     *
     * @param id the audit ID to delete
     */
    public void deleteById(FormalAuditId id) {
        create.deleteFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    // ========================================================================
    // State-Specific Queries
    // ========================================================================

    /**
     * Finds an in-progress audit by its ID.
     *
     * <p>Use this when you specifically need an in-progress audit for mutation.
     *
     * @param id the audit ID
     * @return the in-progress audit, or empty if not found or not in-progress
     */
    public Optional<InProgressAudit> findInProgressById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast);
    }

    /**
     * Finds a completed audit by its ID.
     *
     * @param id the audit ID
     * @return the completed audit, or empty if not found or not completed
     */
    public Optional<CompletedAudit> findCompletedById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.COMPLETED))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(CompletedAudit.class::isInstance)
                .map(CompletedAudit.class::cast);
    }

    /**
     * Finds an abandoned audit by its ID.
     *
     * @param id the audit ID
     * @return the abandoned audit, or empty if not found or not abandoned
     */
    public Optional<AbandonedAudit> findAbandonedById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.ABANDONED))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(AbandonedAudit.class::isInstance)
                .map(AbandonedAudit.class::cast);
    }

    // ========================================================================
    // Business Rule Queries
    // ========================================================================

    /**
     * Finds an active (in-progress) audit for an apparatus.
     *
     * <p>This is used to enforce BR-02: Only one active audit per apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the active audit, or empty if no active audit exists
     */
    public Optional<InProgressAudit> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast);
    }

    /**
     * Checks if an active audit exists for the given apparatus.
     *
     * <p>Convenience method for BR-02 enforcement.
     *
     * @param apparatusId the apparatus ID
     * @return true if an active audit exists
     */
    public boolean hasActiveAuditForApparatus(ApparatusId apparatusId) {
        return findActiveByApparatusId(apparatusId).isPresent();
    }

    // ========================================================================
    // List Queries
    // ========================================================================

    /**
     * Finds all audits for a specific apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return list of all audits (any state) for the apparatus, ordered by startedAt descending
     */
    public List<FormalAudit> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .orderBy(FORMAL_AUDIT.STARTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all in-progress audits.
     *
     * <p>Useful for administrative dashboards and stale audit detection (BR-04).
     *
     * @return list of all in-progress audits
     */
    public List<InProgressAudit> findAllInProgress() {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast)
                .toList();
    }

    /**
     * Finds all in-progress audits for a specific auditor.
     *
     * @param auditorId the user ID of the auditor
     * @return list of in-progress audits assigned to the auditor
     */
    public List<InProgressAudit> findInProgressByAuditorId(UserId auditorId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.PERFORMED_BY_ID.eq(auditorId))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast)
                .toList();
    }

    /**
     * Finds completed audits for an apparatus within a date range.
     *
     * @param apparatusId the apparatus ID
     * @param fromInclusive start of date range (inclusive)
     * @param toExclusive end of date range (exclusive)
     * @return list of completed audits in the date range
     */
    public List<CompletedAudit> findCompletedByApparatusIdAndDateRange(
            ApparatusId apparatusId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.COMPLETED))
                .and(FORMAL_AUDIT.COMPLETED_AT.ge(fromInclusive))
                .and(FORMAL_AUDIT.COMPLETED_AT.lt(toExclusive))
                .orderBy(FORMAL_AUDIT.COMPLETED_AT.desc())
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(CompletedAudit.class::isInstance)
                .map(CompletedAudit.class::cast)
                .toList();
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all audits for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of audits
     */
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts in-progress audits (useful for dashboard metrics).
     *
     * @return the count of in-progress audits
     */
    public long countInProgress() {
        return create.selectCount()
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetchOne(0, Long.class);
    }
}
