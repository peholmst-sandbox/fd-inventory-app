package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.jooq.enums.EquipmentStatus;

/**
 * Equipment details for the issue reporting form.
 *
 * @param id the equipment item ID
 * @param name the equipment type name
 * @param serialNumber the serial number, may be null
 * @param barcode the barcode, may be null
 * @param status the current equipment status
 * @param apparatusId the apparatus this equipment is assigned to, may be null
 * @param apparatusUnitNumber the apparatus unit number, may be null
 * @param stationId the station ID where the apparatus is located, may be null
 * @param stationName the station name, may be null
 */
public record EquipmentForReport(
        EquipmentItemId id,
        String name,
        SerialNumber serialNumber,
        Barcode barcode,
        EquipmentStatus status,
        ApparatusId apparatusId,
        String apparatusUnitNumber,
        StationId stationId,
        String stationName
) {
    /**
     * Returns a formatted location string for display.
     */
    public String locationDisplay() {
        if (apparatusUnitNumber != null && stationName != null) {
            return apparatusUnitNumber + " at " + stationName;
        } else if (stationName != null) {
            return stationName;
        } else if (apparatusUnitNumber != null) {
            return apparatusUnitNumber;
        }
        return "Unknown location";
    }

    /**
     * Returns the serial number as a display string.
     */
    public String serialNumberDisplay() {
        return serialNumber != null ? serialNumber.value() : "N/A";
    }

    /**
     * Returns the barcode as a display string.
     */
    public String barcodeDisplay() {
        return barcode != null ? barcode.value() : "N/A";
    }

    /**
     * Returns a display-friendly status label.
     */
    public String statusLabel() {
        return switch (status) {
            case OK -> "Operational";
            case DAMAGED -> "Damaged";
            case IN_REPAIR -> "In Repair";
            case MISSING -> "Missing";
            case FAILED_INSPECTION -> "Failed Inspection";
            case RETIRED -> "Retired";
            case EXPIRED -> "Expired";
        };
    }
}
