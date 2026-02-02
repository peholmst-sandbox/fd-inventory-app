package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;

import java.util.List;

/**
 * A compartment with its associated checkable items.
 *
 * @param id the compartment ID
 * @param code the short compartment code (e.g., "L1", "R2")
 * @param name the full compartment name
 * @param displayOrder the order in which compartments should be displayed
 * @param items the list of items in this compartment to be checked
 */
public record CompartmentWithItems(
    CompartmentId id,
    String code,
    String name,
    int displayOrder,
    List<CheckableItem> items
) {}
