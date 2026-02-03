package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link FormalAudit} aggregate persistence.
 *
 * <p>This repository handles storage and retrieval of formal audits across all states
 * (in-progress, completed, abandoned). The repository works with domain types and
 * abstracts away the persistence mechanism.
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
public interface FormalAuditRepository {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a formal audit (insert or update based on existence).
     *
     * <p>This method handles all audit states. The implementation should:
     * <ul>
     *   <li>Insert if the audit doesn't exist</li>
     *   <li>Update if the audit exists</li>
     *   <li>Persist state-specific fields appropriately</li>
     * </ul>
     *
     * @param audit the audit to save (any state)
     * @return the saved audit
     */
    FormalAudit save(FormalAudit audit);

    /**
     * Finds a formal audit by its ID.
     *
     * <p>Returns the audit in its current state (InProgressAudit, CompletedAudit,
     * or AbandonedAudit).
     *
     * @param id the audit ID
     * @return the audit, or empty if not found
     */
    Optional<FormalAudit> findById(FormalAuditId id);

    /**
     * Checks if an audit with the given ID exists.
     *
     * @param id the audit ID
     * @return true if the audit exists
     */
    boolean existsById(FormalAuditId id);

    /**
     * Deletes a formal audit by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the audit and should only be used
     * for cleanup of test data or administrative purposes. Completed audits should
     * generally be retained for historical records.
     *
     * @param id the audit ID to delete
     */
    void deleteById(FormalAuditId id);

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
    Optional<InProgressAudit> findInProgressById(FormalAuditId id);

    /**
     * Finds a completed audit by its ID.
     *
     * @param id the audit ID
     * @return the completed audit, or empty if not found or not completed
     */
    Optional<CompletedAudit> findCompletedById(FormalAuditId id);

    /**
     * Finds an abandoned audit by its ID.
     *
     * @param id the audit ID
     * @return the abandoned audit, or empty if not found or not abandoned
     */
    Optional<AbandonedAudit> findAbandonedById(FormalAuditId id);

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
    Optional<InProgressAudit> findActiveByApparatusId(ApparatusId apparatusId);

    /**
     * Checks if an active audit exists for the given apparatus.
     *
     * <p>Convenience method for BR-02 enforcement.
     *
     * @param apparatusId the apparatus ID
     * @return true if an active audit exists
     */
    default boolean hasActiveAuditForApparatus(ApparatusId apparatusId) {
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
    List<FormalAudit> findByApparatusId(ApparatusId apparatusId);

    /**
     * Finds all in-progress audits.
     *
     * <p>Useful for administrative dashboards and stale audit detection (BR-04).
     *
     * @return list of all in-progress audits
     */
    List<InProgressAudit> findAllInProgress();

    /**
     * Finds all in-progress audits for a specific auditor.
     *
     * @param auditorId the user ID of the auditor
     * @return list of in-progress audits assigned to the auditor
     */
    List<InProgressAudit> findInProgressByAuditorId(UserId auditorId);

    /**
     * Finds completed audits for an apparatus within a date range.
     *
     * @param apparatusId the apparatus ID
     * @param fromInclusive start of date range (inclusive)
     * @param toExclusive end of date range (exclusive)
     * @return list of completed audits in the date range
     */
    List<CompletedAudit> findCompletedByApparatusIdAndDateRange(
            ApparatusId apparatusId,
            java.time.Instant fromInclusive,
            java.time.Instant toExclusive
    );

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all audits for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of audits
     */
    long countByApparatusId(ApparatusId apparatusId);

    /**
     * Counts in-progress audits (useful for dashboard metrics).
     *
     * @return the count of in-progress audits
     */
    long countInProgress();
}
