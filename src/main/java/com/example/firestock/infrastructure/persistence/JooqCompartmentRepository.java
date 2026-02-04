package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.apparatus.Compartment;
import com.example.firestock.domain.apparatus.CompartmentLocation;
import com.example.firestock.domain.apparatus.CompartmentRepository;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.infrastructure.persistence.mapper.CompartmentMapper;
import com.example.firestock.jooq.tables.records.CompartmentRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.COMPARTMENT;

/**
 * jOOQ implementation of the {@link CompartmentRepository} interface.
 */
@Repository
public class JooqCompartmentRepository implements CompartmentRepository {

    private final DSLContext create;
    private final CompartmentMapper mapper;

    public JooqCompartmentRepository(DSLContext create, CompartmentMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    @Override
    public Compartment save(Compartment compartment) {
        var record = create.newRecord(COMPARTMENT);
        mapper.updateRecord(record, compartment);

        if (existsById(compartment.id())) {
            record.setUpdatedAt(LocalDateTime.now());
            record.update();
        } else {
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.insert();
        }

        return compartment;
    }

    @Override
    public Optional<Compartment> findById(CompartmentId id) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(CompartmentId id) {
        return create.fetchExists(
                create.selectFrom(COMPARTMENT)
                        .where(COMPARTMENT.ID.eq(id))
        );
    }

    @Override
    public void deleteById(CompartmentId id) {
        create.deleteFrom(COMPARTMENT)
                .where(COMPARTMENT.ID.eq(id))
                .execute();
    }

    @Override
    public List<Compartment> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .orderBy(COMPARTMENT.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<Compartment> findByApparatusIdAndLocation(ApparatusId apparatusId, CompartmentLocation location) {
        return create.selectFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .and(COMPARTMENT.LOCATION.eq(mapper.toJooqLocation(location)))
                .orderBy(COMPARTMENT.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public int deleteByApparatusId(ApparatusId apparatusId) {
        return create.deleteFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .execute();
    }

    @Override
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }
}
