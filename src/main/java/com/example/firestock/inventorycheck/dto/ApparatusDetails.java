package com.example.firestock.inventorycheck.dto;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.util.List;

/**
 * Full details of an apparatus including all compartments and items for inventory checking.
 *
 * @param id the apparatus ID
 * @param unitNumber the operational unit number
 * @param stationId the station ID where the apparatus is assigned
 * @param stationName the station name
 * @param compartments the list of compartments with their items, ordered by display order
 */
public record ApparatusDetails(
    ApparatusId id,
    UnitNumber unitNumber,
    StationId stationId,
    String stationName,
    List<CompartmentWithItems> compartments
) {
    /**
     * Returns the total number of items across all compartments.
     */
    public int totalItemCount() {
        return compartments.stream()
            .mapToInt(c -> c.items().size())
            .sum();
    }
}
