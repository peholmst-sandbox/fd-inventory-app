package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.manifest.ManifestEntry;
import com.example.firestock.domain.manifest.ManifestEntryRepository;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.infrastructure.persistence.mapper.ManifestEntryMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.MANIFEST_ENTRY;

/**
 * jOOQ implementation of the {@link ManifestEntryRepository} interface.
 */
@Repository
public class JooqManifestEntryRepository implements ManifestEntryRepository {

    private final DSLContext create;
    private final ManifestEntryMapper mapper;

    public JooqManifestEntryRepository(DSLContext create, ManifestEntryMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    @Override
    public ManifestEntry save(ManifestEntry entry) {
        var record = create.newRecord(MANIFEST_ENTRY);
        mapper.updateRecord(record, entry);

        if (existsById(entry.id())) {
            record.setUpdatedAt(LocalDateTime.now());
            record.update();
        } else {
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.insert();
        }

        return entry;
    }

    @Override
    public Optional<ManifestEntry> findById(ManifestEntryId id) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(ManifestEntryId id) {
        return create.fetchExists(
                create.selectFrom(MANIFEST_ENTRY)
                        .where(MANIFEST_ENTRY.ID.eq(id))
        );
    }

    @Override
    public void deleteById(ManifestEntryId id) {
        create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.ID.eq(id))
                .execute();
    }

    @Override
    public List<ManifestEntry> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<ManifestEntry> findCriticalByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .and(MANIFEST_ENTRY.IS_CRITICAL.isTrue())
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public int deleteByApparatusId(ApparatusId apparatusId) {
        return create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .execute();
    }

    @Override
    public List<ManifestEntry> findByCompartmentId(CompartmentId compartmentId) {
        return create.selectFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .orderBy(MANIFEST_ENTRY.DISPLAY_ORDER.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public int deleteByCompartmentId(CompartmentId compartmentId) {
        return create.deleteFrom(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .execute();
    }

    @Override
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

    @Override
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countCriticalByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId))
                .and(MANIFEST_ENTRY.IS_CRITICAL.isTrue())
                .fetchOne(0, Long.class);
    }

    @Override
    public long countByCompartmentId(CompartmentId compartmentId) {
        return create.selectCount()
                .from(MANIFEST_ENTRY)
                .where(MANIFEST_ENTRY.COMPARTMENT_ID.eq(compartmentId))
                .fetchOne(0, Long.class);
    }
}
