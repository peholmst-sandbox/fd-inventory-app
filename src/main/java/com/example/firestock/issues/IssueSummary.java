package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.IssueStatus;

import java.time.Instant;

/**
 * Summary of an issue for display in lists.
 *
 * @param id the issue ID
 * @param referenceNumber the human-readable reference number
 * @param title the issue title
 * @param category the issue category (DAMAGE, MISSING, MALFUNCTION, etc.)
 * @param severity the severity level
 * @param status the current status
 * @param reportedAt when the issue was reported
 */
public record IssueSummary(
        IssueId id,
        ReferenceNumber referenceNumber,
        String title,
        IssueCategory category,
        IssueSeverity severity,
        IssueStatus status,
        Instant reportedAt
) {
    /**
     * Returns a display-friendly status label.
     */
    public String statusLabel() {
        return switch (status) {
            case OPEN -> "Open";
            case ACKNOWLEDGED -> "Acknowledged";
            case IN_PROGRESS -> "In Progress";
            case RESOLVED -> "Resolved";
            case CLOSED -> "Closed";
        };
    }

    /**
     * Returns a display-friendly category label.
     */
    public String categoryLabel() {
        return switch (category) {
            case DAMAGE -> "Damaged";
            case MALFUNCTION -> "Malfunctioning";
            case MISSING -> "Missing";
            case EXPIRED -> "Expired";
            case LOW_STOCK -> "Low Stock";
            case CONTAMINATION -> "Contaminated";
            case CALIBRATION -> "Calibration Required";
            case OTHER -> "Other";
        };
    }

    /**
     * Returns a display-friendly severity label.
     */
    public String severityLabel() {
        return switch (severity) {
            case CRITICAL -> "Critical";
            case HIGH -> "High";
            case MEDIUM -> "Medium";
            case LOW -> "Low";
        };
    }
}
