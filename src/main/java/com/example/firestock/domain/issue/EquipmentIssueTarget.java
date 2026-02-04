package com.example.firestock.domain.issue;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;

import java.util.Objects;

/**
 * Issue target representing an individual equipment item.
 *
 * @param equipmentItemId the equipment item ID
 */
public record EquipmentIssueTarget(EquipmentItemId equipmentItemId) implements IssueTarget {

    public EquipmentIssueTarget {
        Objects.requireNonNull(equipmentItemId, "Equipment item ID cannot be null");
    }
}
