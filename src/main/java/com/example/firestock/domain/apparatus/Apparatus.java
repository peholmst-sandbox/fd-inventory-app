package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;

import java.time.Year;
import java.util.Optional;

/**
 * Sealed interface representing a fire apparatus (aggregate root).
 *
 * <p>An apparatus is a fire service vehicle that carries equipment and responds to
 * incidents. Each apparatus has compartments containing equipment that must be
 * tracked and verified through inventory checks and formal audits.
 *
 * <p>The sealed interface enforces status-based behaviour through its permitted
 * implementations:
 * <ul>
 *   <li>{@link InServiceApparatus} - Active apparatus that can respond to incidents</li>
 *   <li>{@link OutOfServiceApparatus} - Temporarily unavailable apparatus</li>
 *   <li>{@link ReserveApparatus} - Backup apparatus not in primary rotation</li>
 *   <li>{@link DecommissionedApparatus} - Permanently retired apparatus</li>
 * </ul>
 *
 * <p>State transitions:
 * <pre>
 * InServiceApparatus --putOutOfService()--> OutOfServiceApparatus
 * InServiceApparatus --moveToReserve()----> ReserveApparatus
 * InServiceApparatus --decommission()-----> DecommissionedApparatus
 *
 * OutOfServiceApparatus --returnToService()--> InServiceApparatus
 * OutOfServiceApparatus --decommission()-----> DecommissionedApparatus
 *
 * ReserveApparatus --activate()--------> InServiceApparatus
 * ReserveApparatus --decommission()----> DecommissionedApparatus
 * </pre>
 */
public sealed interface Apparatus
        permits InServiceApparatus, OutOfServiceApparatus, ReserveApparatus, DecommissionedApparatus {

    /**
     * Returns the unique identifier of this apparatus.
     *
     * @return the apparatus ID
     */
    ApparatusId id();

    /**
     * Returns the operational unit number for radio communications.
     *
     * @return the unit number (e.g., "Engine 5", "Ladder 12")
     */
    UnitNumber unitNumber();

    /**
     * Returns the Vehicle Identification Number.
     *
     * @return the VIN, or null if not recorded
     */
    String vin();

    /**
     * Returns the type classification of this apparatus.
     *
     * @return the apparatus type
     */
    ApparatusType type();

    /**
     * Returns the vehicle manufacturer.
     *
     * @return the make, or null if not recorded
     */
    String make();

    /**
     * Returns the vehicle model.
     *
     * @return the model, or null if not recorded
     */
    String model();

    /**
     * Returns the model year of the vehicle.
     *
     * @return the year, or null if not recorded
     */
    Year year();

    /**
     * Returns the station where this apparatus is assigned.
     *
     * @return the station ID
     */
    StationId stationId();

    /**
     * Returns the current operational status.
     *
     * @return the apparatus status
     */
    ApparatusStatus status();

    /**
     * Returns the barcode for quick scanning during inventory.
     *
     * @return the barcode, or null if not assigned
     */
    Barcode barcode();

    /**
     * Returns any notes about this apparatus.
     *
     * @return the notes, or null if none
     */
    String notes();

    /**
     * Returns the VIN as an Optional.
     *
     * @return the VIN, or empty if not recorded
     */
    default Optional<String> vinOpt() {
        return Optional.ofNullable(vin());
    }

    /**
     * Returns the make as an Optional.
     *
     * @return the make, or empty if not recorded
     */
    default Optional<String> makeOpt() {
        return Optional.ofNullable(make());
    }

    /**
     * Returns the model as an Optional.
     *
     * @return the model, or empty if not recorded
     */
    default Optional<String> modelOpt() {
        return Optional.ofNullable(model());
    }

    /**
     * Returns the year as an Optional.
     *
     * @return the year, or empty if not recorded
     */
    default Optional<Year> yearOpt() {
        return Optional.ofNullable(year());
    }

    /**
     * Returns the barcode as an Optional.
     *
     * @return the barcode, or empty if not assigned
     */
    default Optional<Barcode> barcodeOpt() {
        return Optional.ofNullable(barcode());
    }

    /**
     * Returns the notes as an Optional.
     *
     * @return the notes, or empty if none
     */
    default Optional<String> notesOpt() {
        return Optional.ofNullable(notes());
    }

    /**
     * Checks if this apparatus is currently in service.
     *
     * @return true if the apparatus can respond to incidents
     */
    default boolean isInService() {
        return status() == ApparatusStatus.IN_SERVICE;
    }

    /**
     * Checks if this apparatus has been permanently retired.
     *
     * @return true if the apparatus is decommissioned
     */
    default boolean isDecommissioned() {
        return status() == ApparatusStatus.DECOMMISSIONED;
    }

    /**
     * Checks if this apparatus can have inventory checks performed.
     *
     * @return true if inventory checks are applicable
     */
    default boolean canHaveInventoryChecks() {
        return !isDecommissioned();
    }
}
