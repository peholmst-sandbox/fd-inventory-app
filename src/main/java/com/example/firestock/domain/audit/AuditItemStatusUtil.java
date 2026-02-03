package com.example.firestock.domain.audit;

import com.example.firestock.jooq.enums.AuditItemStatus;

/**
 * Utility methods for working with {@link AuditItemStatus}.
 *
 * <p>Since jOOQ generates the enum and we can't add methods to it, this utility class
 * provides domain logic that would otherwise be on the enum itself.
 */
public final class AuditItemStatusUtil {
    private AuditItemStatusUtil() {
        // Utility class
    }

    /**
     * Determines if the given status requires automatic issue creation per BR-05.
     *
     * @param status the audit item status
     * @return true if an issue should be created for this status
     */
    public static boolean requiresIssue(AuditItemStatus status) {
        return status == AuditItemStatus.MISSING
                || status == AuditItemStatus.DAMAGED
                || status == AuditItemStatus.FAILED_INSPECTION
                || status == AuditItemStatus.EXPIRED;
    }
}
