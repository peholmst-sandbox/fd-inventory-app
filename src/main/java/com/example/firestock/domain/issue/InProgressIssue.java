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
import java.util.Objects;

/**
 * An issue that is being actively worked on.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@link #resolve(UserId, Instant, String)} - Transitions to {@link ResolvedIssue} (requires notes per BR-07)</li>
 *   <li>{@link #close(Instant)} - Transitions to {@link ClosedIssue}</li>
 * </ul>
 *
 * @param id the unique identifier of this issue
 * @param referenceNumber the human-readable reference number
 * @param target the target of the issue (equipment, consumable, or apparatus-level)
 * @param apparatusId the apparatus where the issue was found
 * @param stationId the station where the issue was found
 * @param title the issue title
 * @param description the detailed description
 * @param severity the severity level
 * @param category the issue category
 * @param reportedById the user who reported the issue
 * @param reportedAt when the issue was reported
 * @param isCrewResponsibility whether this issue is a crew responsibility
 * @param acknowledgedById the user who acknowledged the issue
 * @param acknowledgedAt when the issue was acknowledged
 * @param startedAt when work was started on the issue
 */
public record InProgressIssue(
        IssueId id,
        ReferenceNumber referenceNumber,
        IssueTarget target,
        ApparatusId apparatusId,
        StationId stationId,
        String title,
        String description,
        IssueSeverity severity,
        IssueCategory category,
        UserId reportedById,
        Instant reportedAt,
        boolean isCrewResponsibility,
        UserId acknowledgedById,
        Instant acknowledgedAt,
        Instant startedAt
) implements Issue {

    public InProgressIssue {
        Objects.requireNonNull(id, "Issue ID cannot be null");
        Objects.requireNonNull(referenceNumber, "Reference number cannot be null");
        Objects.requireNonNull(target, "Issue target cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(reportedById, "Reported by ID cannot be null");
        Objects.requireNonNull(reportedAt, "Reported at cannot be null");
        Objects.requireNonNull(acknowledgedById, "Acknowledged by ID cannot be null");
        Objects.requireNonNull(acknowledgedAt, "Acknowledged at cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
    }

    @Override
    public IssueStatus status() {
        return IssueStatus.IN_PROGRESS;
    }

    /**
     * Resolves this issue, transitioning to {@link ResolvedIssue}.
     *
     * <p>Per BR-07, resolution notes are required.
     *
     * @param resolvedById the user resolving the issue
     * @param resolvedAt when the issue was resolved
     * @param resolutionNotes notes describing how the issue was resolved
     * @return a resolved issue
     * @throws IssueException.ResolutionNotesRequiredException if resolution notes are empty
     */
    public ResolvedIssue resolve(UserId resolvedById, Instant resolvedAt, String resolutionNotes) {
        Objects.requireNonNull(resolvedById, "Resolved by ID cannot be null");
        Objects.requireNonNull(resolvedAt, "Resolved at cannot be null");

        // BR-07: Resolution requires notes
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            throw new IssueException.ResolutionNotesRequiredException(id);
        }

        return new ResolvedIssue(
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
                isCrewResponsibility,
                acknowledgedById,
                acknowledgedAt,
                resolvedById,
                resolvedAt,
                resolutionNotes
        );
    }
}
