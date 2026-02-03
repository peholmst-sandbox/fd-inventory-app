package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.audit.AuditItemStatus;
import com.example.firestock.domain.audit.FormalAuditItem;
import com.example.firestock.domain.audit.FormalAuditItemRepository;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import com.example.firestock.infrastructure.persistence.mapper.FormalAuditItemMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.FORMAL_AUDIT_ITEM;

/**
 * jOOQ implementation of the {@link FormalAuditItemRepository} interface.
 *
 * <p>Handles the polymorphic target (equipment vs. consumable) via XOR on ID fields.
 */
@Repository
public class JooqFormalAuditItemRepository implements FormalAuditItemRepository {

    private final DSLContext create;
    private final FormalAuditItemMapper mapper;

    public JooqFormalAuditItemRepository(DSLContext create, FormalAuditItemMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    @Override
    public FormalAuditItem save(FormalAuditItem item) {
        var record = create.newRecord(FORMAL_AUDIT_ITEM);
        mapper.updateRecord(record, item);

        if (existsById(item.id())) {
            record.update();
        } else {
            record.setCreatedAt(LocalDateTime.now());
            record.insert();
        }

        return item;
    }

    @Override
    public List<FormalAuditItem> saveAll(List<FormalAuditItem> items) {
        return items.stream()
                .map(this::save)
                .toList();
    }

    @Override
    public Optional<FormalAuditItem> findById(FormalAuditItemId id) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(FormalAuditItemId id) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.ID.eq(id))
        );
    }

    @Override
    public void deleteById(FormalAuditItemId id) {
        create.deleteFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.ID.eq(id))
                .execute();
    }

    @Override
    public List<FormalAuditItem> findByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<FormalAuditItem> findAuditedByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.ne(mapper.toJooqStatus(AuditItemStatus.NOT_AUDITED)))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<FormalAuditItem> findUnauditedByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(mapper.toJooqStatus(AuditItemStatus.NOT_AUDITED)))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<FormalAuditItem> findWithIssuesByAuditId(FormalAuditId auditId) {
        // Statuses that require issue creation per BR-05
        var issueStatuses = List.of(
                mapper.toJooqStatus(AuditItemStatus.MISSING),
                mapper.toJooqStatus(AuditItemStatus.DAMAGED),
                mapper.toJooqStatus(AuditItemStatus.FAILED_INSPECTION),
                mapper.toJooqStatus(AuditItemStatus.EXPIRED)
        );

        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.in(issueStatuses))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<FormalAuditItem> findByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(mapper.toJooqStatus(status)))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public int deleteByAuditId(FormalAuditId auditId) {
        return create.deleteFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .execute();
    }

    @Override
    public Optional<FormalAuditItem> findByAuditIdAndEquipmentItemId(
            FormalAuditId auditId,
            EquipmentItemId equipmentItemId
    ) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FormalAuditItem> findByAuditIdAndConsumableStockId(
            FormalAuditId auditId,
            ConsumableStockId consumableStockId
    ) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByAuditIdAndEquipmentItemId(FormalAuditId auditId, EquipmentItemId equipmentItemId) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                        .and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
        );
    }

    @Override
    public boolean existsByAuditIdAndConsumableStockId(FormalAuditId auditId, ConsumableStockId consumableStockId) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                        .and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
        );
    }

    @Override
    public List<FormalAuditItem> findByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public List<FormalAuditItem> findByConsumableStockId(ConsumableStockId consumableStockId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    @Override
    public long countByAuditId(FormalAuditId auditId) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countAuditedByAuditId(FormalAuditId auditId) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.ne(mapper.toJooqStatus(AuditItemStatus.NOT_AUDITED)))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countWithIssuesByAuditId(FormalAuditId auditId) {
        var issueStatuses = List.of(
                mapper.toJooqStatus(AuditItemStatus.MISSING),
                mapper.toJooqStatus(AuditItemStatus.DAMAGED),
                mapper.toJooqStatus(AuditItemStatus.FAILED_INSPECTION),
                mapper.toJooqStatus(AuditItemStatus.EXPIRED)
        );

        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.in(issueStatuses))
                .fetchOne(0, Long.class);
    }

    @Override
    public long countByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(mapper.toJooqStatus(status)))
                .fetchOne(0, Long.class);
    }
}
