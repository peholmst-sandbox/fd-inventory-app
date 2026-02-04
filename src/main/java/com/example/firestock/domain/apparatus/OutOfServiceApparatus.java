package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;

import java.time.Year;
import java.util.Objects;

/**
 * An apparatus that is temporarily out of service.
 *
 * <p>Out-of-service apparatus are unavailable for incident response due to:
 * <ul>
 *   <li>Scheduled or unscheduled maintenance</li>
 *   <li>Repair work</li>
 *   <li>Equipment issues that prevent safe operation</li>
 * </ul>
 *
 * <p>Inventory tracking continues for out-of-service apparatus to maintain
 * accurate records and prepare for return to service.
 *
 * <p>State transitions available:
 * <ul>
 *   <li>{@link #returnToService()} - Return apparatus to active duty</li>
 *   <li>{@link #decommission(String)} - Permanently retire if repair is not feasible</li>
 * </ul>
 *
 * @param id the unique identifier
 * @param unitNumber the operational identifier for radio communications
 * @param vin the Vehicle Identification Number (nullable)
 * @param type the apparatus type classification
 * @param make the vehicle manufacturer (nullable)
 * @param model the vehicle model (nullable)
 * @param year the model year as java.time.Year (nullable)
 * @param stationId the assigned station
 * @param barcode the scanning barcode (nullable)
 * @param notes the reason for being out of service or other notes (nullable)
 */
public record OutOfServiceApparatus(
        ApparatusId id,
        UnitNumber unitNumber,
        String vin,
        ApparatusType type,
        String make,
        String model,
        Year year,
        StationId stationId,
        Barcode barcode,
        String notes
) implements Apparatus {

    public OutOfServiceApparatus {
        Objects.requireNonNull(id, "Apparatus ID cannot be null");
        Objects.requireNonNull(unitNumber, "Unit number cannot be null");
        Objects.requireNonNull(type, "Apparatus type cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
    }

    @Override
    public ApparatusStatus status() {
        return ApparatusStatus.OUT_OF_SERVICE;
    }

    /**
     * Returns this apparatus to active service.
     *
     * @return an in-service apparatus
     */
    public InServiceApparatus returnToService() {
        return new InServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, null
        );
    }

    /**
     * Decommissions this apparatus permanently.
     *
     * @param reason the reason for decommissioning
     * @return a decommissioned apparatus
     */
    public DecommissionedApparatus decommission(String reason) {
        return new DecommissionedApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, reason
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param notes the new notes
     * @return a new apparatus with updated notes
     */
    public OutOfServiceApparatus withNotes(String notes) {
        return new OutOfServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }
}
