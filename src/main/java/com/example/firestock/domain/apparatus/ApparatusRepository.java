package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Apparatus} aggregate persistence.
 *
 * <p>This repository handles storage and retrieval of apparatus across all states
 * (in-service, out-of-service, reserve, decommissioned). The repository works with
 * domain types and abstracts away the persistence mechanism.
 *
 * <h3>Aggregate Boundaries</h3>
 * <p>Apparatus is an aggregate root. The repository manages:
 * <ul>
 *   <li>The apparatus entity itself (all states)</li>
 *   <li>Transactional consistency for state transitions</li>
 * </ul>
 *
 * <p>Compartments are handled by {@link CompartmentRepository} as they are
 * separately managed entities that belong to apparatus.
 *
 * <h3>State Handling</h3>
 * <p>Methods like {@link #save(Apparatus)} accept any apparatus state and persist
 * appropriately. The sealed interface hierarchy ensures type safety.
 */
public interface ApparatusRepository {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves an apparatus (insert or update based on existence).
     *
     * <p>This method handles all apparatus states. The implementation should:
     * <ul>
     *   <li>Insert if the apparatus doesn't exist</li>
     *   <li>Update if the apparatus exists</li>
     *   <li>Persist state-specific fields appropriately</li>
     * </ul>
     *
     * @param apparatus the apparatus to save (any state)
     * @return the saved apparatus
     */
    Apparatus save(Apparatus apparatus);

    /**
     * Finds an apparatus by its ID.
     *
     * <p>Returns the apparatus in its current state (InServiceApparatus,
     * OutOfServiceApparatus, ReserveApparatus, or DecommissionedApparatus).
     *
     * @param id the apparatus ID
     * @return the apparatus, or empty if not found
     */
    Optional<Apparatus> findById(ApparatusId id);

    /**
     * Checks if an apparatus with the given ID exists.
     *
     * @param id the apparatus ID
     * @return true if the apparatus exists
     */
    boolean existsById(ApparatusId id);

    /**
     * Deletes an apparatus by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the apparatus and should only be
     * used for cleanup of test data or administrative purposes. Decommissioned
     * apparatus should generally be retained for historical records.
     *
     * @param id the apparatus ID to delete
     */
    void deleteById(ApparatusId id);

    // ========================================================================
    // State-Specific Queries
    // ========================================================================

    /**
     * Finds an in-service apparatus by its ID.
     *
     * <p>Use this when you specifically need an in-service apparatus for operations
     * that require an active apparatus.
     *
     * @param id the apparatus ID
     * @return the in-service apparatus, or empty if not found or not in-service
     */
    Optional<InServiceApparatus> findInServiceById(ApparatusId id);

    // ========================================================================
    // Station Queries
    // ========================================================================

    /**
     * Finds all apparatus assigned to a specific station.
     *
     * @param stationId the station ID
     * @return list of all apparatus (any state) at the station
     */
    List<Apparatus> findByStationId(StationId stationId);

    /**
     * Finds all in-service apparatus at a specific station.
     *
     * @param stationId the station ID
     * @return list of in-service apparatus at the station
     */
    List<InServiceApparatus> findInServiceByStationId(StationId stationId);

    // ========================================================================
    // List Queries
    // ========================================================================

    /**
     * Finds all in-service apparatus.
     *
     * <p>Useful for listing apparatus available for inventory checks and audits.
     *
     * @return list of all in-service apparatus
     */
    List<InServiceApparatus> findAllInService();

    /**
     * Finds all apparatus that are not decommissioned.
     *
     * <p>This includes in-service, out-of-service, and reserve apparatus.
     *
     * @return list of all active apparatus (excluding decommissioned)
     */
    List<Apparatus> findAllActive();

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all apparatus at a station.
     *
     * @param stationId the station ID
     * @return the count of apparatus
     */
    long countByStationId(StationId stationId);

    /**
     * Counts in-service apparatus at a station.
     *
     * @param stationId the station ID
     * @return the count of in-service apparatus
     */
    long countInServiceByStationId(StationId stationId);
}
