package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.AuditStatus;

import java.time.Instant;
import java.util.List;

/**
 * Full details of a formal audit including apparatus info and compartments with items.
 *
 * @param auditId the formal audit ID
 * @param apparatusId the apparatus being audited
 * @param stationId the station where the apparatus is located
 * @param unitNumber the apparatus unit number
 * @param status the current audit status
 * @param startedAt when the audit was started
 * @param completedAt when the audit was completed, or null
 * @param pausedAt when the audit was paused, or null
 * @param totalItems the total number of items to audit
 * @param auditedCount the number of items audited
 * @param issuesFoundCount the number of issues found
 * @param unexpectedItemsCount the number of unexpected items found
 * @param notes audit notes
 * @param compartments the compartments with their auditable items
 */
public record AuditDetails(
        FormalAuditId auditId,
        ApparatusId apparatusId,
        StationId stationId,
        UnitNumber unitNumber,
        AuditStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant pausedAt,
        int totalItems,
        int auditedCount,
        int issuesFoundCount,
        int unexpectedItemsCount,
        String notes,
        List<CompartmentWithAuditableItems> compartments
) {
    /**
     * Returns the progress percentage (0-100).
     */
    public int progressPercentage() {
        if (totalItems == 0) {
            return 0;
        }
        return Math.min(100, (auditedCount * 100) / totalItems);
    }

    /**
     * Returns true if all items have been audited.
     */
    public boolean isAllAudited() {
        return auditedCount >= totalItems;
    }

    /**
     * Returns the actual total item count from compartments.
     */
    public int actualTotalItemCount() {
        return compartments.stream()
                .mapToInt(CompartmentWithAuditableItems::totalItems)
                .sum();
    }
}
