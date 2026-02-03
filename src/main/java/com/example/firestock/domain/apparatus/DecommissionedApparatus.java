package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.util.Objects;

/**
 * An apparatus that has been permanently retired from service.
 *
 * <p>Decommissioned apparatus are kept in the system for historical records:
 * <ul>
 *   <li>Past inventory checks and audits remain accessible</li>
 *   <li>Equipment transfer records are preserved</li>
 *   <li>No further inventory tracking is performed</li>
 * </ul>
 *
 * <p>This is a terminal state with no available state transitions.
 * Decommissioned apparatus cannot be returned to service.
 *
 * @param id the unique identifier
 * @param unitNumber the operational identifier (historical)
 * @param vin the Vehicle Identification Number (nullable)
 * @param type the apparatus type classification
 * @param make the vehicle manufacturer (nullable)
 * @param model the vehicle model (nullable)
 * @param year the model year (nullable)
 * @param stationId the last assigned station
 * @param barcode the scanning barcode (nullable)
 * @param notes the reason for decommissioning or other notes (nullable)
 */
public record DecommissionedApparatus(
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

    public DecommissionedApparatus {
        Objects.requireNonNull(id, "Apparatus ID cannot be null");
        Objects.requireNonNull(unitNumber, "Unit number cannot be null");
        Objects.requireNonNull(type, "Apparatus type cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
    }

    @Override
    public ApparatusStatus status() {
        return ApparatusStatus.DECOMMISSIONED;
    }
}
