package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.time.LocalDateTime;

/**
 * Summary information about an apparatus for the selection view.
 *
 * @param id the unique apparatus identifier
 * @param unitNumber the operational unit number (e.g., "Engine 5")
 * @param stationName the name of the station where the apparatus is assigned
 * @param lastCheckDate the date of the last completed inventory check, or null if never checked
 */
public record ApparatusSummary(
    ApparatusId id,
    UnitNumber unitNumber,
    String stationName,
    LocalDateTime lastCheckDate
) {}
