package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.jooq.enums.EquipmentStatus;

/**
 * Result of creating an issue, containing the issue ID and reference number.
 *
 * @param issueId the ID of the created issue
 * @param referenceNumber the human-readable reference number for the issue
 */
public record IssueCreatedResult(
        IssueId issueId,
        ReferenceNumber referenceNumber
) {
    /**
     * Extended result that includes the updated equipment status.
     *
     * @param issueId the ID of the created issue
     * @param referenceNumber the human-readable reference number for the issue
     * @param updatedEquipmentStatus the new status of the equipment after issue creation, or null if unchanged
     */
    public record WithEquipmentStatus(
            IssueId issueId,
            ReferenceNumber referenceNumber,
            EquipmentStatus updatedEquipmentStatus
    ) {}
}
