package com.example.firestock.domain.manifest;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ManifestEntry} persistence.
 *
 * <p>The manifest defines expected equipment for each apparatus. This repository
 * handles manifest entry CRUD operations and queries for inventory verification.
 *
 * <h3>Usage Patterns</h3>
 * <ul>
 *   <li><b>Inventory checks:</b> Load manifest to compare against actual inventory</li>
 *   <li><b>Audit preparation:</b> Generate audit items from manifest entries</li>
 *   <li><b>Configuration:</b> Add/modify expected equipment for apparatus</li>
 * </ul>
 */
public interface ManifestEntryRepository {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a manifest entry (insert or update based on existence).
     *
     * @param entry the manifest entry to save
     * @return the saved manifest entry
     */
    ManifestEntry save(ManifestEntry entry);

    /**
     * Finds a manifest entry by its ID.
     *
     * @param id the manifest entry ID
     * @return the manifest entry, or empty if not found
     */
    Optional<ManifestEntry> findById(ManifestEntryId id);

    /**
     * Checks if a manifest entry with the given ID exists.
     *
     * @param id the manifest entry ID
     * @return true if the manifest entry exists
     */
    boolean existsById(ManifestEntryId id);

    /**
     * Deletes a manifest entry by its ID.
     *
     * @param id the manifest entry ID to delete
     */
    void deleteById(ManifestEntryId id);

    // ========================================================================
    // Apparatus Queries
    // ========================================================================

    /**
     * Finds all manifest entries for a specific apparatus.
     *
     * <p>This is the primary method for loading an apparatus's complete manifest
     * for inventory checks and audits. Results are ordered by display order.
     *
     * @param apparatusId the apparatus ID
     * @return list of manifest entries for the apparatus, ordered by displayOrder
     */
    List<ManifestEntry> findByApparatusId(ApparatusId apparatusId);

    /**
     * Finds only critical manifest entries for an apparatus.
     *
     * <p>Useful for generating quick-check lists or priority audits that focus
     * on essential equipment only.
     *
     * @param apparatusId the apparatus ID
     * @return list of critical manifest entries for the apparatus
     */
    List<ManifestEntry> findCriticalByApparatusId(ApparatusId apparatusId);

    /**
     * Deletes all manifest entries for a specific apparatus.
     *
     * <p>Use with caution - typically only appropriate when reconfiguring
     * an apparatus's manifest from scratch or during decommissioning.
     *
     * @param apparatusId the apparatus ID
     * @return the number of entries deleted
     */
    int deleteByApparatusId(ApparatusId apparatusId);

    // ========================================================================
    // Compartment Queries
    // ========================================================================

    /**
     * Finds all manifest entries for a specific compartment.
     *
     * <p>Useful for compartment-by-compartment inventory verification.
     *
     * @param compartmentId the compartment ID
     * @return list of manifest entries for the compartment
     */
    List<ManifestEntry> findByCompartmentId(CompartmentId compartmentId);

    /**
     * Deletes all manifest entries for a specific compartment.
     *
     * <p>Should be called when a compartment is being removed to maintain
     * referential integrity.
     *
     * @param compartmentId the compartment ID
     * @return the number of entries deleted
     */
    int deleteByCompartmentId(CompartmentId compartmentId);

    // ========================================================================
    // Equipment Type Queries
    // ========================================================================

    /**
     * Finds manifest entries for a specific equipment type on an apparatus.
     *
     * <p>Useful for checking if an equipment type is expected on a particular
     * apparatus, regardless of compartment.
     *
     * @param apparatusId the apparatus ID
     * @param equipmentTypeId the equipment type ID
     * @return list of manifest entries matching the criteria
     */
    List<ManifestEntry> findByApparatusIdAndEquipmentTypeId(
            ApparatusId apparatusId,
            EquipmentTypeId equipmentTypeId
    );

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts manifest entries for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the total count of manifest entries
     */
    long countByApparatusId(ApparatusId apparatusId);

    /**
     * Counts critical manifest entries for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of critical manifest entries
     */
    long countCriticalByApparatusId(ApparatusId apparatusId);

    /**
     * Counts manifest entries for a compartment.
     *
     * @param compartmentId the compartment ID
     * @return the count of manifest entries in the compartment
     */
    long countByCompartmentId(CompartmentId compartmentId);
}
