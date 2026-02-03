package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;

import java.util.Objects;

/**
 * Request to report a new equipment issue.
 *
 * @param equipmentItemId the equipment item this issue relates to
 * @param category the issue category (DAMAGE, MISSING, MALFUNCTION)
 * @param description detailed description of the issue (min 10 characters per BR-01)
 * @param severity the severity level
 * @param criticalConfirmed true if user has confirmed critical severity (required per BR-02)
 */
public record ReportIssueRequest(
        EquipmentItemId equipmentItemId,
        IssueCategory category,
        String description,
        IssueSeverity severity,
        boolean criticalConfirmed
) {
    private static final int MIN_DESCRIPTION_LENGTH = 10;

    public ReportIssueRequest {
        Objects.requireNonNull(equipmentItemId, "Equipment item ID is required");
        Objects.requireNonNull(category, "Issue category is required");
        Objects.requireNonNull(description, "Description is required");
        Objects.requireNonNull(severity, "Severity is required");

        description = description.strip();
    }

    /**
     * Validates the request according to business rules.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        // BR-01: Description must be at least 10 characters
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Description must be at least %d characters", MIN_DESCRIPTION_LENGTH));
        }

        // BR-02: Critical severity requires confirmation
        if (severity == IssueSeverity.CRITICAL && !criticalConfirmed) {
            throw new IllegalArgumentException(
                "Critical severity issues require explicit confirmation");
        }
    }

    /**
     * Generates an appropriate title based on the category.
     */
    public String generateTitle() {
        return switch (category) {
            case DAMAGE -> "Damaged equipment reported";
            case MALFUNCTION -> "Equipment malfunction reported";
            case MISSING -> "Missing equipment reported";
            case EXPIRED -> "Expired equipment reported";
            case LOW_STOCK -> "Low stock reported";
            case CONTAMINATION -> "Equipment contamination reported";
            case CALIBRATION -> "Equipment calibration required";
            case OTHER -> "Equipment issue reported";
        };
    }
}
