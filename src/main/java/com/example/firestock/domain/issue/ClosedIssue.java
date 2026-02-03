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
 * A closed issue that was closed without being resolved.
 *
 * <p>This is a terminal state. No further state transitions are allowed.
 * Issues may be closed for various reasons such as being a duplicate,
 * no longer relevant, or unable to reproduce.
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
 * @param closedAt when the issue was closed
 */
public record ClosedIssue(
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
        Instant closedAt
) implements Issue {

    public ClosedIssue {
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
        Objects.requireNonNull(closedAt, "Closed at cannot be null");
    }

    @Override
    public IssueStatus status() {
        return IssueStatus.CLOSED;
    }
}
