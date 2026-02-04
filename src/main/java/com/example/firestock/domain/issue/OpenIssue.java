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
 * An open issue that has been reported but not yet acknowledged.
 *
 * <p>This is the initial state of an issue. State transitions:
 * <ul>
 *   <li>{@link #acknowledge(UserId, Instant)} - Transitions to {@link AcknowledgedIssue}</li>
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
 */
public record OpenIssue(
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
        boolean isCrewResponsibility
) implements Issue {

    public OpenIssue {
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
    }

    /**
     * Creates a new open issue.
     *
     * @param id the issue ID
     * @param referenceNumber the reference number
     * @param target the issue target
     * @param apparatusId the apparatus ID
     * @param stationId the station ID
     * @param title the title
     * @param description the description
     * @param severity the severity
     * @param category the category
     * @param reportedById the reporter's user ID
     * @param reportedAt when the issue was reported
     * @param isCrewResponsibility whether this is a crew responsibility
     * @return a new open issue
     */
    public static OpenIssue create(
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
            boolean isCrewResponsibility
    ) {
        return new OpenIssue(
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
    }

    @Override
    public IssueStatus status() {
        return IssueStatus.OPEN;
    }

    /**
     * Acknowledges this issue, transitioning to {@link AcknowledgedIssue}.
     *
     * @param acknowledgedById the user acknowledging the issue
     * @param acknowledgedAt when the issue was acknowledged
     * @return an acknowledged issue
     */
    public AcknowledgedIssue acknowledge(UserId acknowledgedById, Instant acknowledgedAt) {
        Objects.requireNonNull(acknowledgedById, "Acknowledged by ID cannot be null");
        Objects.requireNonNull(acknowledgedAt, "Acknowledged at cannot be null");

        return new AcknowledgedIssue(
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
                acknowledgedAt
        );
    }
}
