package com.example.firestock.domain.equipment;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.jooq.enums.EquipmentStatus;
import com.example.firestock.jooq.enums.OwnershipType;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain model representing an individual equipment item.
 *
 * <p>An equipment item is an individual, trackable piece of equipment (e.g., a specific
 * halligan bar with serial number). Equipment items are instances of equipment types
 * and can be assigned to apparatus compartments, stored at stations, or in transit.
 *
 * <h3>Location States</h3>
 * <ul>
 *   <li><b>On apparatus</b>: apparatusId and compartmentId are set, stationId is null</li>
 *   <li><b>In storage</b>: stationId is set, apparatusId and compartmentId are null</li>
 *   <li><b>In transit</b>: all location fields are null</li>
 * </ul>
 *
 * @param id the unique identifier
 * @param equipmentTypeId the equipment type this is an instance of
 * @param serialNumber the serial number (nullable, unique if present)
 * @param barcode the barcode (nullable, unique if present)
 * @param ownershipType whether department-owned or crew-owned
 * @param homeStationId the home station for crew-owned items (required if crew-owned)
 * @param manufacturer the manufacturer name (nullable)
 * @param model the model name (nullable)
 * @param acquisitionDate when the item was acquired (nullable)
 * @param warrantyExpiryDate when the warranty expires (nullable)
 * @param lastTestDate when the item was last tested (nullable)
 * @param nextTestDueDate when the next test is due (nullable)
 * @param status the current equipment status
 * @param stationId the storage station (for items in storage)
 * @param apparatusId the assigned apparatus (for items on apparatus)
 * @param compartmentId the assigned compartment (for items on apparatus)
 * @param notes free-form notes (nullable)
 */
public record EquipmentItem(
        EquipmentItemId id,
        EquipmentTypeId equipmentTypeId,
        SerialNumber serialNumber,
        Barcode barcode,
        OwnershipType ownershipType,
        StationId homeStationId,
        String manufacturer,
        String model,
        LocalDate acquisitionDate,
        LocalDate warrantyExpiryDate,
        LocalDate lastTestDate,
        LocalDate nextTestDueDate,
        EquipmentStatus status,
        StationId stationId,
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        String notes
) {

    public EquipmentItem {
        Objects.requireNonNull(id, "Equipment item ID cannot be null");
        Objects.requireNonNull(equipmentTypeId, "Equipment type ID cannot be null");
        Objects.requireNonNull(ownershipType, "Ownership type cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        // BR-08: Crew-owned must have home station
        if (ownershipType == OwnershipType.CREW_OWNED && homeStationId == null) {
            throw new IllegalArgumentException("Crew-owned equipment must have a home station");
        }
        if (ownershipType == OwnershipType.DEPARTMENT && homeStationId != null) {
            throw new IllegalArgumentException("Department-owned equipment cannot have a home station");
        }

        // Location consistency: either on apparatus, in storage, or in transit
        boolean onApparatus = apparatusId != null && compartmentId != null && stationId == null;
        boolean inStorage = apparatusId == null && compartmentId == null && stationId != null;
        boolean inTransit = apparatusId == null && compartmentId == null && stationId == null;

        if (!onApparatus && !inStorage && !inTransit) {
            throw new IllegalArgumentException(
                    "Invalid location state: must be on apparatus (apparatus + compartment), " +
                    "in storage (station only), or in transit (no location)");
        }
    }

    /**
     * Creates a copy with a new status.
     *
     * @param newStatus the new equipment status
     * @return a new equipment item with the updated status
     */
    public EquipmentItem withStatus(EquipmentStatus newStatus) {
        return new EquipmentItem(
                id, equipmentTypeId, serialNumber, barcode, ownershipType, homeStationId,
                manufacturer, model, acquisitionDate, warrantyExpiryDate, lastTestDate,
                nextTestDueDate, newStatus, stationId, apparatusId, compartmentId, notes
        );
    }

    /**
     * Checks if this equipment is currently assigned to an apparatus.
     *
     * @return true if on apparatus
     */
    public boolean isOnApparatus() {
        return apparatusId != null && compartmentId != null;
    }

    /**
     * Checks if this equipment is currently in storage at a station.
     *
     * @return true if in storage
     */
    public boolean isInStorage() {
        return stationId != null && apparatusId == null;
    }

    /**
     * Checks if this equipment is currently in transit.
     *
     * @return true if in transit
     */
    public boolean isInTransit() {
        return stationId == null && apparatusId == null;
    }

    /**
     * Returns the serial number as an Optional.
     *
     * @return the serial number, or empty if not set
     */
    public Optional<SerialNumber> serialNumberOpt() {
        return Optional.ofNullable(serialNumber);
    }

    /**
     * Returns the barcode as an Optional.
     *
     * @return the barcode, or empty if not set
     */
    public Optional<Barcode> barcodeOpt() {
        return Optional.ofNullable(barcode);
    }

    /**
     * Returns the notes as an Optional.
     *
     * @return the notes, or empty if not set
     */
    public Optional<String> notesOpt() {
        return Optional.ofNullable(notes);
    }
}
