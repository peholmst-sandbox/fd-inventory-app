package com.example.firestock.domain.apparatus;

/**
 * The physical location of a compartment on an apparatus.
 *
 * <p>Compartment locations help firefighters quickly locate equipment during
 * emergencies and ensure consistent organisation across apparatus of the same type.
 */
public enum CompartmentLocation {

    /**
     * Compartment on the driver's side of the apparatus.
     */
    DRIVER_SIDE,

    /**
     * Compartment on the passenger's side of the apparatus.
     */
    PASSENGER_SIDE,

    /**
     * Compartment at the rear of the apparatus.
     */
    REAR,

    /**
     * Compartment at the front of the apparatus.
     */
    FRONT,

    /**
     * Compartment on the top/roof of the apparatus.
     */
    TOP,

    /**
     * Compartment inside the cab or crew area.
     */
    INTERIOR,

    /**
     * Crosslay compartment for pre-connected hose lines (typically on engines).
     */
    CROSSLAY
}
