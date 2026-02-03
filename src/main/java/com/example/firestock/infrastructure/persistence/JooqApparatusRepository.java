package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.apparatus.Apparatus;
import com.example.firestock.domain.apparatus.ApparatusRepository;
import com.example.firestock.domain.apparatus.ApparatusStatus;
import com.example.firestock.domain.apparatus.InServiceApparatus;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.infrastructure.persistence.mapper.ApparatusMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.APPARATUS;

/**
 * jOOQ implementation of the {@link ApparatusRepository} interface.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Repository
public class JooqApparatusRepository implements ApparatusRepository {

    private final DSLContext create;
    private final ApparatusMapper mapper;

    public JooqApparatusRepository(DSLContext create, ApparatusMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    @Override
    public Apparatus save(Apparatus apparatus) {
        var record = create.newRecord(APPARATUS);
        mapper.updateRecord(record, apparatus);

        if (existsById(apparatus.id())) {
            record.setUpdatedAt(LocalDateTime.now());
            record.update();
        } else {
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.insert();
        }

        return apparatus;
    }

    @Override
    public Optional<Apparatus> findById(ApparatusId id) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(ApparatusId id) {
        return create.fetchExists(
                create.selectFrom(APPARATUS)
                        .where(APPARATUS.ID.eq(id))
        );
    }

    @Override
    public void deleteById(ApparatusId id) {
        create.deleteFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<InServiceApparatus> findInServiceById(ApparatusId id) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.ID.eq(id))
                .and(APPARATUS.STATUS.eq(mapper.toJooqStatus(ApparatusStatus.IN_SERVICE)))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast);
    }

    @Override
    public List<Apparatus> findByStationId(StationId stationId) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<InServiceApparatus> findInServiceByStationId(StationId stationId) {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .and(APPARATUS.STATUS.eq(mapper.toJooqStatus(ApparatusStatus.IN_SERVICE)))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast)
                .toList();
    }

    @Override
    public List<InServiceApparatus> findAllInService() {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATUS.eq(mapper.toJooqStatus(ApparatusStatus.IN_SERVICE)))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InServiceApparatus.class::isInstance)
                .map(InServiceApparatus.class::cast)
                .toList();
    }

    @Override
    public List<Apparatus> findAllActive() {
        return create.selectFrom(APPARATUS)
                .where(APPARATUS.STATUS.ne(mapper.toJooqStatus(ApparatusStatus.DECOMMISSIONED)))
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public long countByStationId(StationId stationId) {
        return create.selectCount()
                .from(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countInServiceByStationId(StationId stationId) {
        return create.selectCount()
                .from(APPARATUS)
                .where(APPARATUS.STATION_ID.eq(stationId))
                .and(APPARATUS.STATUS.eq(mapper.toJooqStatus(ApparatusStatus.IN_SERVICE)))
                .fetchOne(0, Long.class);
    }
}
