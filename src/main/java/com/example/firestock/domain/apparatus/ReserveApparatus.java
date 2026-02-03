package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.util.Objects;

/**
 * An apparatus held in reserve as a backup.
 *
 * <p>Reserve apparatus are operational but not assigned to primary duty:
 * <ul>
 *   <li>Available to substitute for apparatus undergoing maintenance</li>
 *   <li>May be deployed during major incidents requiring additional resources</li>
 *   <li>Should maintain inventory but with reduced check frequency</li>
 * </ul>
 *
 * <p>State transitions available:
 * <ul>
 *   <li>{@link #activate()} - Put apparatus into active service</li>
 *   <li>{@link #decommission(String)} - Permanently retire the apparatus</li>
 * </ul>
 *
 * @param id the unique identifier
 * @param unitNumber the operational identifier for radio communications
 * @param vin the Vehicle Identification Number (nullable)
 * @param type the apparatus type classification
 * @param make the vehicle manufacturer (nullable)
 * @param model the vehicle model (nullable)
 * @param year the model year (nullable)
 * @param stationId the assigned station
 * @param barcode the scanning barcode (nullable)
 * @param notes notes about reserve status (nullable)
 */
public record ReserveApparatus(
        ApparatusId id,
        UnitNumber unitNumber,
        String vin,
        ApparatusType type,
        String make,
        String model,
        Integer year,
        StationId stationId,
        Barcode barcode,
        String notes
) implements Apparatus {

    public ReserveApparatus {
        Objects.requireNonNull(id, "Apparatus ID cannot be null");
        Objects.requireNonNull(unitNumber, "Unit number cannot be null");
        Objects.requireNonNull(type, "Apparatus type cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
    }

    @Override
    public ApparatusStatus status() {
        return ApparatusStatus.RESERVE;
    }

    /**
     * Activates this apparatus for primary duty.
     *
     * @return an in-service apparatus
     */
    public InServiceApparatus activate() {
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
     * Creates a copy assigned to a different station.
     *
     * @param stationId the new station ID
     * @return a new apparatus assigned to the station
     */
    public ReserveApparatus assignToStation(StationId stationId) {
        return new ReserveApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param notes the new notes
     * @return a new apparatus with updated notes
     */
    public ReserveApparatus withNotes(String notes) {
        return new ReserveApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }
}
