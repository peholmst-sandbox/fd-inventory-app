package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.manifest.ManifestEntry;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.infrastructure.persistence.mapper.ManifestEntryMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.MANIFEST_ENTRY;

/**
 * Repository for {@link ManifestEntry} persistence.
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
@Repository
public class ManifestEntryRepository {

    private final DSLContext create;
    private final ManifestEntryMapper mapper;

    public ManifestEntryRepository(DSLContext create, ManifestEntryMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a manifest entry (insert or update based on existence).
     *
     * @param entry the manifest entry to save
     * @return the saved manifest entry
     */
    public ManifestEntry save(ManifestEntry entry) {
        var record = create.newRecord(MANIFEST_ENTRY);
        mapper.updateRecord(record, entry);

        if (existsById(entry.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return entry;
    }

    /**
     * Finds a manifest entry by its ID.
     *
     * @param id the manifest entry ID
     * @return the manifest entry, or empty if not found
     */
    public Optional<ManifestEntry> findById(ManifestEntryId id) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if a manifest entry with the given ID exists.
     *
     * @param id the manifest entry ID
     * @return true if the manifest entry exists
     */
    public boolean existsById(ManifestEntryId id) {
        return create.fetchExists(
                create.selectFrom(MANIFEST_ENTRY)
                        .where(MANIFEST_ENTRY.ID.eq(id))
        );
    }

    /**
     * Deletes a manifest entry by its ID.
     *
     * @param id the manifest entry ID to delete
     */
    public void deleteById(ManifestEntryId id) {
        create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.ID.eq(id))
                .execute();
    }

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
    public List<ManifestEntry> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds only critical manifest entries for an apparatus.
     *
     * <p>Useful for generating quick-check lists or priority audits that focus
     * on essential equipment only.
     *
     * @param apparatusId the apparatus ID
     * @return list of critical manifest entries for the apparatus
     */
    public List<ManifestEntry> findCriticalByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .and(MANIFEST_ENTRY.IS_CRITICAL.isTrue())
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Deletes all manifest entries for a specific apparatus.
     *
     * <p>Use with caution - typically only appropriate when reconfiguring
     * an apparatus's manifest from scratch or during decommissioning.
     *
     * @param apparatusId the apparatus ID
     * @return the number of entries deleted
     */
    public int deleteByApparatusId(ApparatusId apparatusId) {
        return create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .execute();
    }

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
    public List<ManifestEntry> findByCompartmentId(CompartmentId compartmentId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Deletes all manifest entries for a specific compartment.
     *
     * <p>Should be called when a compartment is being removed to maintain
     * referential integrity.
     *
     * @param compartmentId the compartment ID
     * @return the number of entries deleted
     */
    public int deleteByCompartmentId(CompartmentId compartmentId) {
        return create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .execute();
    }

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
    public List<ManifestEntry> findByApparatusIdAndEquipmentTypeId(
            ApparatusId apparatusId,
            EquipmentTypeId equipmentTypeId
    ) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .and(MANIFEST_ENTRY.EQUIPMENT_TYPE_ID.eq(equipmentTypeId))
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts manifest entries for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the total count of manifest entries
     */
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts critical manifest entries for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of critical manifest entries
     */
    public long countCriticalByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .and(MANIFEST_ENTRY.IS_CRITICAL.isTrue())
                .fetchOne(0, Long.class);
    }

    /**
     * Counts manifest entries for a compartment.
     *
     * @param compartmentId the compartment ID
     * @return the count of manifest entries in the compartment
     */
    public long countByCompartmentId(CompartmentId compartmentId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .fetchOne(0, Long.class);
    }
}
