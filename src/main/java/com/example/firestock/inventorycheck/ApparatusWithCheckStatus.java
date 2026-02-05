package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Apparatus information with its current check status.
 * Used in the apparatus selection view to show which apparatus have active checks
 * and who is currently checking them.
 *
 * @param id the unique apparatus identifier
 * @param unitNumber the operational unit number (e.g., "Engine 5")
 * @param stationName the name of the station where the apparatus is assigned
 * @param lastCheckDate the date of the last completed inventory check, or null if never checked
 * @param hasActiveCheck true if there is an IN_PROGRESS check for this apparatus
 * @param currentCheckerNames display names of users currently checking this apparatus,
 *                            looked up from the user table
 */
public record ApparatusWithCheckStatus(
    ApparatusId id,
    UnitNumber unitNumber,
    String stationName,
    LocalDateTime lastCheckDate,
    boolean hasActiveCheck,
    List<String> currentCheckerNames
) {}
