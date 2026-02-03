package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.apparatus.Compartment;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.infrastructure.persistence.mapper.CompartmentMapper;
import com.example.firestock.jooq.enums.CompartmentLocation;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.COMPARTMENT;

/**
 * Repository for {@link Compartment} persistence.
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
@Repository
public class CompartmentRepository {

    private final DSLContext create;
    private final CompartmentMapper mapper;

    public CompartmentRepository(DSLContext create, CompartmentMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a compartment (insert or update based on existence).
     *
     * @param compartment the compartment to save
     * @return the saved compartment
     */
    public Compartment save(Compartment compartment) {
        var record = create.newRecord(COMPARTMENT);
        mapper.updateRecord(record, compartment);

        if (existsById(compartment.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return compartment;
    }

    /**
     * Finds a compartment by its ID.
     *
     * @param id the compartment ID
     * @return the compartment, or empty if not found
     */
    public Optional<Compartment> findById(CompartmentId id) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if a compartment with the given ID exists.
     *
     * @param id the compartment ID
     * @return true if the compartment exists
     */
    public boolean existsById(CompartmentId id) {
        return create.fetchExists(
                create.selectFrom(COMPARTMENT)
                        .where(COMPARTMENT.ID.eq(id))
        );
    }

    /**
     * Deletes a compartment by its ID.
     *
     * <p><b>Warning:</b> Deleting a compartment may affect manifest entries
     * that reference it. Consider updating manifests first.
     *
     * @param id the compartment ID to delete
     */
    public void deleteById(CompartmentId id) {
        create.deleteFrom(COMPARTMENT)
                .where(COMPARTMENT.ID.eq(id))
                .execute();
    }

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
    public List<Compartment> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .orderBy(COMPARTMENT.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds compartments by apparatus ID and location.
     *
     * @param apparatusId the apparatus ID
     * @param location the compartment location
     * @return list of compartments at the specified location
     */
    public List<Compartment> findByApparatusIdAndLocation(ApparatusId apparatusId, CompartmentLocation location) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .and(COMPARTMENT.LOCATION.eq(location))
                .orderBy(COMPARTMENT.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Deletes all compartments for a specific apparatus.
     *
     * <p>Use with caution - this is typically only appropriate when
     * decommissioning an apparatus or for data cleanup.
     *
     * @param apparatusId the apparatus ID
     * @return the number of compartments deleted
     */
    public int deleteByApparatusId(ApparatusId apparatusId) {
        return create.deleteFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .execute();
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts compartments for an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the count of compartments
     */
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }
}
