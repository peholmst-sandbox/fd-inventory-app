package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Summary information about an apparatus for the audit selection view.
 *
 * @param id the apparatus ID
 * @param unitNumber the unit number
 * @param type the apparatus type
 * @param stationName the station name
 * @param lastAuditDate the last completed audit date, or null
 * @param hasActiveAudit whether there is an in-progress audit for this apparatus
 */
public record ApparatusAuditInfo(
        ApparatusId id,
        UnitNumber unitNumber,
        ApparatusType type,
        String stationName,
        Instant lastAuditDate,
        boolean hasActiveAudit
) {
    /**
     * Returns true if the apparatus is due for an audit.
     * Apparatus should be audited at least every 90 days.
     */
    public boolean isAuditDue() {
        if (lastAuditDate == null) {
            return true;
        }
        return lastAuditDate.until(Instant.now(), ChronoUnit.DAYS) > 90;
    }

    /**
     * Returns the number of days since the last audit.
     */
    public long daysSinceLastAudit() {
        if (lastAuditDate == null) {
            return -1;
        }
        return lastAuditDate.until(Instant.now(), ChronoUnit.DAYS);
    }
}
