package com.example.firestock.domain.apparatus;

/**
 * The operational status of a fire apparatus.
 *
 * <p>The status determines whether an apparatus can respond to incidents and
 * whether it should be included in regular inventory checks and audits.
 */
public enum ApparatusStatus {

    /**
     * Apparatus is operational and can respond to incidents.
     * In-service apparatus require regular inventory checks and audits.
     */
    IN_SERVICE,

    /**
     * Apparatus is temporarily unavailable due to maintenance, repair, or other issues.
     * Out-of-service apparatus may still have inventory but are not responding to calls.
     */
    OUT_OF_SERVICE,

    /**
     * Apparatus is available as a backup but not in primary rotation.
     * Reserve apparatus should maintain inventory but have reduced check frequency.
     */
    RESERVE,

    /**
     * Apparatus has been permanently retired from service.
     * Decommissioned apparatus are kept for historical records but no longer tracked.
     */
    DECOMMISSIONED
}
