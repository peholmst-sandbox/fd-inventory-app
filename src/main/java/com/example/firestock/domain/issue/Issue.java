package com.example.firestock.domain.issue;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.IssueStatus;

import java.time.Instant;

/**
 * Sealed interface representing an issue (aggregate root).
 *
 * <p>An issue tracks problems with equipment, consumables, or apparatus that need
 * to be addressed by maintenance or crew. The sealed interface enforces state-based
 * behavior through its permitted implementations:
 *
 * <ul>
 *   <li>{@link OpenIssue} - Newly reported, awaiting acknowledgment</li>
 *   <li>{@link AcknowledgedIssue} - Seen by maintenance, awaiting work</li>
 *   <li>{@link InProgressIssue} - Being actively worked on</li>
 *   <li>{@link ResolvedIssue} - Fixed, terminal state (BR-07)</li>
 *   <li>{@link ClosedIssue} - Closed without resolution, terminal state</li>
 * </ul>
 *
 * <p>State transitions:
 * <pre>
 * OpenIssue ------acknowledge()----> AcknowledgedIssue
 * AcknowledgedIssue ---startWork()--> InProgressIssue
 * InProgressIssue -----resolve()----> ResolvedIssue (requires notes per BR-07)
 * Any non-terminal -----close()-----> ClosedIssue
 * </pre>
 */
public sealed interface Issue permits OpenIssue, AcknowledgedIssue, InProgressIssue, ResolvedIssue, ClosedIssue {

    /**
     * Returns the unique identifier of this issue.
     *
     * @return the issue ID
     */
    IssueId id();

    /**
     * Returns the human-readable reference number for this issue.
     *
     * @return the reference number (e.g., ISS-2026-00001)
     */
    ReferenceNumber referenceNumber();

    /**
     * Returns the target of this issue (equipment, consumable, or apparatus-level).
     *
     * @return the issue target
     */
    IssueTarget target();

    /**
     * Returns the apparatus where the issue was found.
     *
     * @return the apparatus ID
     */
    ApparatusId apparatusId();

    /**
     * Returns the station where the issue was found.
     *
     * @return the station ID
     */
    StationId stationId();

    /**
     * Returns the title of the issue.
     *
     * @return the title
     */
    String title();

    /**
     * Returns the detailed description of the issue.
     *
     * @return the description
     */
    String description();

    /**
     * Returns the severity level of the issue.
     *
     * @return the severity
     */
    IssueSeverity severity();

    /**
     * Returns the category of the issue.
     *
     * @return the category
     */
    IssueCategory category();

    /**
     * Returns the current status of the issue.
     *
     * @return the issue status
     */
    IssueStatus status();

    /**
     * Returns the user who reported the issue.
     *
     * @return the reporter's user ID
     */
    UserId reportedById();

    /**
     * Returns when the issue was reported.
     *
     * @return the report timestamp
     */
    Instant reportedAt();

    /**
     * Returns whether this issue is a crew responsibility.
     *
     * @return true if the crew should handle this issue
     */
    boolean isCrewResponsibility();

    /**
     * Checks if this issue is in a terminal state (resolved or closed).
     *
     * @return true if the issue cannot be modified
     */
    default boolean isTerminal() {
        return status() == IssueStatus.RESOLVED || status() == IssueStatus.CLOSED;
    }

    /**
     * Checks if this issue is active (not in a terminal state).
     *
     * @return true if the issue can be modified
     */
    default boolean isActive() {
        return !isTerminal();
    }

    /**
     * Closes this issue without resolving it.
     *
     * <p>Any non-terminal issue can be closed. This transitions to {@link ClosedIssue}.
     *
     * @param closedAt when the issue was closed
     * @return a closed issue
     * @throws IssueException.IssueAlreadyClosedException if already in a terminal state
     */
    default ClosedIssue close(Instant closedAt) {
        if (isTerminal()) {
            throw new IssueException.IssueAlreadyClosedException(id(), status());
        }
        return new ClosedIssue(
                id(),
                referenceNumber(),
                target(),
                apparatusId(),
                stationId(),
                title(),
                description(),
                severity(),
                category(),
                reportedById(),
                reportedAt(),
                isCrewResponsibility(),
                closedAt
        );
    }
}
