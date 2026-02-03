package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.audit.AbandonedAudit;
import com.example.firestock.domain.audit.AuditStatus;
import com.example.firestock.domain.audit.CompletedAudit;
import com.example.firestock.domain.audit.FormalAudit;
import com.example.firestock.domain.audit.FormalAuditRepository;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.infrastructure.persistence.mapper.FormalAuditMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.APPARATUS;
import static com.example.firestock.jooq.Tables.FORMAL_AUDIT;

/**
 * jOOQ implementation of the {@link FormalAuditRepository} interface.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Repository
public class JooqFormalAuditRepository implements FormalAuditRepository {

    private final DSLContext create;
    private final FormalAuditMapper mapper;

    public JooqFormalAuditRepository(DSLContext create, FormalAuditMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    @Override
    public FormalAudit save(FormalAudit audit) {
        var record = create.newRecord(FORMAL_AUDIT);
        mapper.updateRecord(record, audit);

        if (existsById(audit.id())) {
            record.setUpdatedAt(LocalDateTime.now());
            record.update();
        } else {
            // Look up station_id from apparatus (denormalized for query performance)
            var stationId = create.select(APPARATUS.STATION_ID)
                    .from(APPARATUS)
                    .where(APPARATUS.ID.eq(audit.apparatusId()))
                    .fetchOne(APPARATUS.STATION_ID);
            record.setStationId(stationId);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.insert();
        }

        return audit;
    }

    @Override
    public Optional<FormalAudit> findById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(FormalAuditId id) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT)
                        .where(FORMAL_AUDIT.ID.eq(id))
        );
    }

    @Override
    public void deleteById(FormalAuditId id) {
        create.deleteFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<InProgressAudit> findInProgressById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.IN_PROGRESS)))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast);
    }

    @Override
    public Optional<CompletedAudit> findCompletedById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.COMPLETED)))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(CompletedAudit.class::isInstance)
                .map(CompletedAudit.class::cast);
    }

    @Override
    public Optional<AbandonedAudit> findAbandonedById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.ABANDONED)))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(AbandonedAudit.class::isInstance)
                .map(AbandonedAudit.class::cast);
    }

    @Override
    public Optional<InProgressAudit> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.IN_PROGRESS)))
                .fetchOptional()
                .map(mapper::toDomain)
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast);
    }

    @Override
    public List<FormalAudit> findByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .orderBy(FORMAL_AUDIT.STARTED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<InProgressAudit> findAllInProgress() {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.IN_PROGRESS)))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast)
                .toList();
    }

    @Override
    public List<InProgressAudit> findInProgressByAuditorId(UserId auditorId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.PERFORMED_BY_ID.eq(auditorId))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.IN_PROGRESS)))
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(InProgressAudit.class::isInstance)
                .map(InProgressAudit.class::cast)
                .toList();
    }

    @Override
    public List<CompletedAudit> findCompletedByApparatusIdAndDateRange(
            ApparatusId apparatusId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.COMPLETED)))
                .and(FORMAL_AUDIT.COMPLETED_AT.ge(mapper.toLocalDateTime(fromInclusive)))
                .and(FORMAL_AUDIT.COMPLETED_AT.lt(mapper.toLocalDateTime(toExclusive)))
                .orderBy(FORMAL_AUDIT.COMPLETED_AT.desc())
                .fetch()
                .map(mapper::toDomain)
                .stream()
                .filter(CompletedAudit.class::isInstance)
                .map(CompletedAudit.class::cast)
                .toList();
    }

    @Override
    public long countByApparatusId(ApparatusId apparatusId) {
        return create.selectCount()
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countInProgress() {
        return create.selectCount()
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.STATUS.eq(mapper.toJooqStatus(AuditStatus.IN_PROGRESS)))
                .fetchOne(0, Long.class);
    }
}
