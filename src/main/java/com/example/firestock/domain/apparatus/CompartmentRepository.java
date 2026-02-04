package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Compartment} persistence.
 *
 * <p>Compartments are storage areas on apparatus where equipment is kept.
 * This repository handles compartment CRUD operations independent of the
 * apparatus aggregate.
 *
 * <h3>Relationship to Apparatus</h3>
 * <p>Compartments belong to apparatus but are managed separately to allow:
 * <ul>
 *   <li>Adding/removing compartments without loading the full apparatus</li>
 *   <li>Querying compartments for manifest configuration</li>
 *   <li>Efficient access during inventory checks</li>
 * </ul>
 */
public interface CompartmentRepository {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a compartment (insert or update based on existence).
     *
     * @param compartment the compartment to save
     * @return the saved compartment
     */
    Compartment save(Compartment compartment);

    /**
     * Finds a compartment by its ID.
     *
     * @param id the compartment ID
     * @return the compartment, or empty if not found
     */
    Optional<Compartment> findById(CompartmentId id);

    /**
     * Checks if a compartment with the given ID exists.
     *
     * @param id the compartment ID
     * @return true if the compartment exists
     */
    boolean existsById(CompartmentId id);

    /**
     * Deletes a compartment by its ID.
     *
     * <p><b>Warning:</b> Deleting a compartment may affect manifest entries
     * that reference it. Consider updating manifests first.
     *
     * @param id the compartment ID to delete
     */
    void deleteById(CompartmentId id);

    // ========================================================================
    // Apparatus Queries
    // ========================================================================

    /**
     * Finds all compartments for a specific apparatus.
     *
     * <p>Results are ordered by display order ascending.
     *
     * @param apparatusId the apparatus ID
     * @return list of compartments for the apparatus, ordered by displayOrder
     */
    List<Compartment> findByApparatusId(ApparatusId apparatusId);

    /**
     * Finds compartments by apparatus ID and location.
     *
     * @param apparatusId the apparatus ID
     * @param location the compartment location
     * @return list of compartments at the specified location
     */
    List<Compartment> findByApparatusIdAndLocation(ApparatusId apparatusId, CompartmentLocation location);

    /**
     * Deletes all compartments for a specific apparatus.
     *
     * <p>Use with caution - this is typically only appropriate when
     * decommissioning an apparatus or for data cleanup.
     *
     * @param apparatusId the apparatus ID
     * @return the number of compartments deleted
     */
    int deleteByApparatusId(ApparatusId apparatusId);

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts compartments for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of compartments
     */
    long countByApparatusId(ApparatusId apparatusId);
}
