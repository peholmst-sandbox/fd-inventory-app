package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.apparatus.Apparatus;
import com.example.firestock.domain.apparatus.InServiceApparatus;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.jooq.enums.ApparatusStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.APPARATUS;

/**
 * Repository for {@link Apparatus} aggregate persistence.
 *
 * <p>Handles storage and retrieval of apparatus across all states
 * (in-service, out-of-service, reserve, decommissioned). The repository works with
 * domain types and handles the sealed interface hierarchy by mapping based on the status field.
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
@Repository
public class ApparatusRepository {

    private final DSLContext create;
    private final ApparatusMapper mapper;

    public ApparatusRepository(DSLContext create, ApparatusMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves an apparatus (insert or update based on existence).
     *
     * <p>This method handles all apparatus states. The implementation:
     * <ul>
     *   <li>Inserts if the apparatus doesn't exist</li>
     *   <li>Updates if the apparatus exists</li>
     *   <li>Persists state-specific fields appropriately</li>
     * </ul>
     *
     * @param apparatus the apparatus to save (any state)
     * @return the saved apparatus
     */
    public Apparatus save(Apparatus apparatus) {
        var record = create.newRecord(APPARATUS);
        mapper.updateRecord(record, apparatus);

        if (existsById(apparatus.id())) {
            record.setUpdatedAt(Instant.now());
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.setUpdatedAt(Instant.now());
            record.insert();
        }

        return apparatus;
    }

    /**
     * Finds an apparatus by its ID.
     *
     * <p>Returns the apparatus in its current state (InServiceApparatus,
     * OutOfServiceApparatus, ReserveApparatus, or DecommissionedApparatus).
     *
     * @param id the apparatus ID
     * @return the apparatus, or empty if not found
     */
    public Optional<Apparatus> findById(ApparatusId id) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an apparatus with the given ID exists.
     *
     * @param id the apparatus ID
     * @return true if the apparatus exists
     */
    public boolean existsById(ApparatusId id) {
        return create.fetchExists(
                create.selectFrom(APPARATUS)
                        .where(APPARATUS.ID.eq(id))
        );
    }

    /**
     * Deletes an apparatus by its ID.
     *
     * <p><b>Warning:</b> This permanently removes the apparatus and should only be
     * used for cleanup of test data or administrative purposes. Decommissioned
     * apparatus should generally be retained for historical records.
     *
     * @param id the apparatus ID to delete
     */
    public void deleteById(ApparatusId id) {
        create.deleteFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .execute();
    }

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
    public Optional<InServiceApparatus> findInServiceById(ApparatusId id) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .and(APPARATUS.STATUS.eq(ApparatusStatus.IN_SERVICE))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast);
    }

    // ========================================================================
    // Station Queries
    // ========================================================================

    /**
     * Finds all apparatus assigned to a specific station.
     *
     * @param stationId the station ID
     * @return list of all apparatus (any state) at the station
     */
    public List<Apparatus> findByStationId(StationId stationId) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all in-service apparatus at a specific station.
     *
     * @param stationId the station ID
     * @return list of in-service apparatus at the station
     */
    public List<InServiceApparatus> findInServiceByStationId(StationId stationId) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .and(APPARATUS.STATUS.eq(ApparatusStatus.IN_SERVICE))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast)
                .toList();
    }

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
    public List<InServiceApparatus> findAllInService() {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATUS.eq(ApparatusStatus.IN_SERVICE))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast)
                .toList();
    }

    /**
     * Finds all apparatus that are not decommissioned.
     *
     * <p>This includes in-service, out-of-service, and reserve apparatus.
     *
     * @return list of all active apparatus (excluding decommissioned)
     */
    public List<Apparatus> findAllActive() {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATUS.ne(ApparatusStatus.DECOMMISSIONED))
                .fetch()
                .map(mapper::toDomain);
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all apparatus at a station.
     *
     * @param stationId the station ID
     * @return the count of apparatus
     */
    public long countByStationId(StationId stationId) {
        return create.selectCount()
                .from(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts in-service apparatus at a station.
     *
     * @param stationId the station ID
     * @return the count of in-service apparatus
     */
    public long countInServiceByStationId(StationId stationId) {
        return create.selectCount()
                .from(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .and(APPARATUS.STATUS.eq(ApparatusStatus.IN_SERVICE))
                .fetchOne(0, Long.class);
    }
}
