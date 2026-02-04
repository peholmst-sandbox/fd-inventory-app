package com.example.firestock.domain.apparatus;

/**
 * The type classification of a fire apparatus.
 *
 * <p>Apparatus types determine the general purpose and capabilities of a fire
 * service vehicle. This classification affects what equipment manifests are
 * expected and how the apparatus is deployed.
 */
public enum ApparatusType {

    /**
     * Fire engine/pumper - primary fire suppression vehicle with pump and hose.
     */
    ENGINE,

    /**
     * Ladder truck/aerial - provides elevated access and ventilation.
     */
    LADDER,

    /**
     * Rescue unit - specialised for technical rescue operations.
     */
    RESCUE,

    /**
     * Tanker/tender - provides additional water supply.
     */
    TANKER,

    /**
     * Brush/wildland unit - designed for vegetation fires and off-road access.
     */
    BRUSH,

    /**
     * Ambulance/medic unit - provides emergency medical services.
     */
    AMBULANCE,

    /**
     * Command vehicle - used for incident command and coordination.
     */
    COMMAND,

    /**
     * Support vehicle - provides logistical support and equipment transport.
     */
    SUPPORT,

    /**
     * Hazmat unit - specialised for hazardous materials response.
     */
    HAZMAT,

    /**
     * Boat/marine unit - for water rescue operations.
     */
    BOAT
}
