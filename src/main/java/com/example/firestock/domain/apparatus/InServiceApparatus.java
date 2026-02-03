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
 * An apparatus that is operational and can respond to incidents.
 *
 * <p>In-service apparatus are the primary focus of FireStock tracking:
 * <ul>
 *   <li>Require regular shift inventory checks</li>
 *   <li>Subject to formal audits by maintenance technicians</li>
 *   <li>Equipment issues must be tracked and resolved</li>
 * </ul>
 *
 * <p>State transitions available:
 * <ul>
 *   <li>{@link #putOutOfService(String)} - Take apparatus out of service for maintenance/repair</li>
 *   <li>{@link #moveToReserve(String)} - Move to reserve status</li>
 *   <li>{@link #decommission(String)} - Permanently retire the apparatus</li>
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
 * @param notes any notes (nullable)
 */
public record InServiceApparatus(
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

    public InServiceApparatus {
        Objects.requireNonNull(id, "Apparatus ID cannot be null");
        Objects.requireNonNull(unitNumber, "Unit number cannot be null");
        Objects.requireNonNull(type, "Apparatus type cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
    }

    @Override
    public ApparatusStatus status() {
        return ApparatusStatus.IN_SERVICE;
    }

    /**
     * Creates a new in-service apparatus with required fields only.
     *
     * @param id the apparatus ID
     * @param unitNumber the unit number
     * @param type the apparatus type
     * @param stationId the station ID
     * @return a new in-service apparatus
     */
    public static InServiceApparatus create(
            ApparatusId id,
            UnitNumber unitNumber,
            ApparatusType type,
            StationId stationId
    ) {
        return new InServiceApparatus(id, unitNumber, null, type, null, null, null, stationId, null, null);
    }

    /**
     * Takes this apparatus out of service.
     *
     * @param reason the reason for taking out of service
     * @return an out-of-service apparatus
     */
    public OutOfServiceApparatus putOutOfService(String reason) {
        return new OutOfServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, reason
        );
    }

    /**
     * Moves this apparatus to reserve status.
     *
     * @param notes notes about the status change
     * @return a reserve apparatus
     */
    public ReserveApparatus moveToReserve(String notes) {
        return new ReserveApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
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
     * Creates a copy with updated vehicle details.
     *
     * @param vin the VIN
     * @param make the make
     * @param model the model
     * @param year the year
     * @return a new apparatus with updated details
     */
    public InServiceApparatus withVehicleDetails(String vin, String make, String model, Year year) {
        return new InServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }

    /**
     * Creates a copy with an updated barcode.
     *
     * @param barcode the new barcode
     * @return a new apparatus with the updated barcode
     */
    public InServiceApparatus withBarcode(Barcode barcode) {
        return new InServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }

    /**
     * Creates a copy assigned to a different station.
     *
     * @param stationId the new station ID
     * @return a new apparatus assigned to the station
     */
    public InServiceApparatus assignToStation(StationId stationId) {
        return new InServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param notes the new notes
     * @return a new apparatus with updated notes
     */
    public InServiceApparatus withNotes(String notes) {
        return new InServiceApparatus(
                id, unitNumber, vin, type, make, model, year, stationId, barcode, notes
        );
    }
}
