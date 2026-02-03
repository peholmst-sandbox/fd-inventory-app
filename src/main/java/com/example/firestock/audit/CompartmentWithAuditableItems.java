package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.jooq.enums.CompartmentLocation;

import java.util.List;

/**
 * A compartment containing its auditable items for display in the audit UI.
 *
 * @param id the compartment ID
 * @param code the compartment code
 * @param name the compartment display name
 * @param location the compartment location on the apparatus
 * @param displayOrder the order for displaying compartments
 * @param items the auditable items in this compartment
 */
public record CompartmentWithAuditableItems(
        CompartmentId id,
        String code,
        String name,
        CompartmentLocation location,
        int displayOrder,
        List<AuditableItem> items
) {
    /**
     * Returns the total number of items in this compartment.
     */
    public int totalItems() {
        return items.size();
    }

    /**
     * Returns the number of items that have been audited.
     */
    public int auditedItems() {
        return (int) items.stream().filter(AuditableItem::isAudited).count();
    }

    /**
     * Returns true if all items in this compartment have been audited.
     */
    public boolean isComplete() {
        return auditedItems() >= totalItems();
    }

    /**
     * Returns the progress percentage (0-100) for this compartment.
     */
    public int progressPercentage() {
        if (totalItems() == 0) {
            return 100;
        }
        return (auditedItems() * 100) / totalItems();
    }

    /**
     * Returns the number of issues found in this compartment.
     */
    public int issuesCount() {
        return (int) items.stream()
                .filter(AuditableItem::isAudited)
                .filter(item -> {
                    var status = item.auditStatus();
                    return status == com.example.firestock.jooq.enums.AuditItemStatus.MISSING
                            || status == com.example.firestock.jooq.enums.AuditItemStatus.DAMAGED
                            || status == com.example.firestock.jooq.enums.AuditItemStatus.FAILED_INSPECTION
                            || status == com.example.firestock.jooq.enums.AuditItemStatus.EXPIRED;
                })
                .count();
    }
}
