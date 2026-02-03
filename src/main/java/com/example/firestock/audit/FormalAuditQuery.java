package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.AuditStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;
import com.example.firestock.jooq.tables.records.FormalAuditRecord;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.*;

/**
 * Query class for formal audit read operations.
 */
@Component
class FormalAuditQuery {

    private final DSLContext create;

    FormalAuditQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds an active (IN_PROGRESS) formal audit for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the active audit record, or empty if none exists
     */
    Optional<FormalAuditRecord> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS))
                .fetchOptional();
    }

    /**
     * Finds the latest completed audit date for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the completion date, or empty if no completed audits exist
     */
    Optional<LocalDateTime> findLatestCompletedDate(ApparatusId apparatusId) {
        return create.select(FORMAL_AUDIT.COMPLETED_AT)
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(apparatusId))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.COMPLETED))
                .orderBy(FORMAL_AUDIT.COMPLETED_AT.desc())
                .limit(1)
                .fetchOptional(FORMAL_AUDIT.COMPLETED_AT);
    }

    /**
     * Finds a formal audit by ID.
     *
     * @param id the audit ID
     * @return the audit summary, or empty if not found
     */
    Optional<AuditSummary> findById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .fetchOptional()
                .map(r -> new AuditSummary(
                        r.getId(),
                        r.getApparatusId(),
                        r.getStatus(),
                        r.getStartedAt(),
                        r.getCompletedAt(),
                        r.getPausedAt(),
                        r.getTotalItems(),
                        r.getAuditedCount(),
                        r.getIssuesFoundCount(),
                        r.getUnexpectedItemsCount()
                ));
    }

    /**
     * Finds the formal audit record by ID for updates.
     *
     * @param id the audit ID
     * @return the record, or empty if not found
     */
    Optional<FormalAuditRecord> findRecordById(FormalAuditId id) {
        return create.selectFrom(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(id))
                .fetchOptional();
    }

    /**
     * Gets the station ID for a formal audit.
     *
     * @param auditId the audit ID
     * @return the station ID, or null if not found
     */
    StationId getStationIdForAudit(FormalAuditId auditId) {
        return create.select(FORMAL_AUDIT.STATION_ID)
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.ID.eq(auditId))
                .fetchOptional(FORMAL_AUDIT.STATION_ID)
                .orElse(null);
    }

    /**
     * Gets full audit details including compartments and items.
     *
     * @param auditId the audit ID
     * @return the audit details, or empty if not found
     */
    Optional<AuditDetails> findDetailsById(FormalAuditId auditId) {
        // First get the audit header with apparatus info
        var auditWithApparatus = create
                .select(
                        FORMAL_AUDIT.asterisk(),
                        APPARATUS.UNIT_NUMBER
                )
                .from(FORMAL_AUDIT)
                .join(APPARATUS).on(APPARATUS.ID.eq(FORMAL_AUDIT.APPARATUS_ID))
                .where(FORMAL_AUDIT.ID.eq(auditId))
                .fetchOptional();

        if (auditWithApparatus.isEmpty()) {
            return Optional.empty();
        }

        var auditRecord = auditWithApparatus.get();
        var apparatusId = auditRecord.get(FORMAL_AUDIT.APPARATUS_ID);

        // Get compartments with items
        var compartments = getCompartmentsWithItems(apparatusId, auditId);

        return Optional.of(new AuditDetails(
                auditRecord.get(FORMAL_AUDIT.ID),
                apparatusId,
                auditRecord.get(FORMAL_AUDIT.STATION_ID),
                auditRecord.get(APPARATUS.UNIT_NUMBER),
                auditRecord.get(FORMAL_AUDIT.STATUS),
                auditRecord.get(FORMAL_AUDIT.STARTED_AT),
                auditRecord.get(FORMAL_AUDIT.COMPLETED_AT),
                auditRecord.get(FORMAL_AUDIT.PAUSED_AT),
                auditRecord.get(FORMAL_AUDIT.TOTAL_ITEMS),
                auditRecord.get(FORMAL_AUDIT.AUDITED_COUNT),
                auditRecord.get(FORMAL_AUDIT.ISSUES_FOUND_COUNT),
                auditRecord.get(FORMAL_AUDIT.UNEXPECTED_ITEMS_COUNT),
                auditRecord.get(FORMAL_AUDIT.NOTES),
                compartments
        ));
    }

    /**
     * Gets all compartments with their auditable items for an apparatus.
     */
    private List<CompartmentWithAuditableItems> getCompartmentsWithItems(ApparatusId apparatusId, FormalAuditId auditId) {
        // Query all items for the apparatus with their audit status
        var itemsQuery = create
                .select(
                        COMPARTMENT.ID,
                        COMPARTMENT.CODE,
                        COMPARTMENT.NAME,
                        COMPARTMENT.LOCATION,
                        COMPARTMENT.DISPLAY_ORDER,
                        EQUIPMENT_ITEM.ID.as("equipment_item_id"),
                        DSL.val((com.example.firestock.domain.primitives.ids.ConsumableStockId) null).as("consumable_stock_id"),
                        MANIFEST_ENTRY.ID.as("manifest_entry_id"),
                        EQUIPMENT_TYPE.NAME.as("item_name"),
                        EQUIPMENT_TYPE.NAME.as("type_name"),
                        EQUIPMENT_ITEM.SERIAL_NUMBER,
                        DSL.val((Quantity) null).as("current_quantity"),
                        DSL.val((java.math.BigDecimal) null).as("required_quantity"),
                        EQUIPMENT_ITEM.WARRANTY_EXPIRY_DATE.as("expiry_date"),
                        EQUIPMENT_TYPE.REQUIRES_TESTING,
                        EQUIPMENT_ITEM.LAST_TEST_DATE,
                        EQUIPMENT_ITEM.NEXT_TEST_DUE_DATE,
                        DSL.val(false).as("is_consumable"),
                        DSL.coalesce(MANIFEST_ENTRY.IS_CRITICAL, false).as("is_critical"),
                        FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS,
                        FORMAL_AUDIT_ITEM.ITEM_CONDITION,
                        FORMAL_AUDIT_ITEM.TEST_RESULT
                )
                .from(EQUIPMENT_ITEM)
                .join(COMPARTMENT).on(COMPARTMENT.ID.eq(EQUIPMENT_ITEM.COMPARTMENT_ID))
                .join(EQUIPMENT_TYPE).on(EQUIPMENT_TYPE.ID.eq(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID))
                .leftJoin(MANIFEST_ENTRY).on(
                        MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId)
                                .and(MANIFEST_ENTRY.COMPARTMENT_ID.eq(COMPARTMENT.ID))
                                .and(MANIFEST_ENTRY.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
                )
                .leftJoin(FORMAL_AUDIT_ITEM).on(
                        FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId)
                                .and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(EQUIPMENT_ITEM.ID))
                )
                .where(EQUIPMENT_ITEM.APPARATUS_ID.eq(apparatusId))
                .unionAll(
                        // Consumable stock items
                        create.select(
                                        COMPARTMENT.ID,
                                        COMPARTMENT.CODE,
                                        COMPARTMENT.NAME,
                                        COMPARTMENT.LOCATION,
                                        COMPARTMENT.DISPLAY_ORDER,
                                        DSL.val((com.example.firestock.domain.primitives.ids.EquipmentItemId) null).as("equipment_item_id"),
                                        CONSUMABLE_STOCK.ID.as("consumable_stock_id"),
                                        MANIFEST_ENTRY.ID.as("manifest_entry_id"),
                                        EQUIPMENT_TYPE.NAME.as("item_name"),
                                        EQUIPMENT_TYPE.NAME.as("type_name"),
                                        DSL.val((SerialNumber) null).as("serial_number"),
                                        CONSUMABLE_STOCK.QUANTITY.as("current_quantity"),
                                        CONSUMABLE_STOCK.REQUIRED_QUANTITY.as("required_quantity"),
                                        CONSUMABLE_STOCK.EXPIRY_DATE.as("expiry_date"),
                                        EQUIPMENT_TYPE.REQUIRES_TESTING,
                                        DSL.val((java.time.LocalDate) null).as("last_test_date"),
                                        DSL.val((java.time.LocalDate) null).as("next_test_due_date"),
                                        DSL.val(true).as("is_consumable"),
                                        DSL.coalesce(MANIFEST_ENTRY.IS_CRITICAL, false).as("is_critical"),
                                        FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS,
                                        FORMAL_AUDIT_ITEM.ITEM_CONDITION,
                                        FORMAL_AUDIT_ITEM.TEST_RESULT
                                )
                                .from(CONSUMABLE_STOCK)
                                .join(COMPARTMENT).on(COMPARTMENT.ID.eq(CONSUMABLE_STOCK.COMPARTMENT_ID))
                                .join(EQUIPMENT_TYPE).on(EQUIPMENT_TYPE.ID.eq(CONSUMABLE_STOCK.EQUIPMENT_TYPE_ID))
                                .leftJoin(MANIFEST_ENTRY).on(
                                        MANIFEST_ENTRY.APPARATUS_ID.eq(apparatusId)
                                                .and(MANIFEST_ENTRY.COMPARTMENT_ID.eq(COMPARTMENT.ID))
                                                .and(MANIFEST_ENTRY.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
                                )
                                .leftJoin(FORMAL_AUDIT_ITEM).on(
                                        FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId)
                                                .and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(CONSUMABLE_STOCK.ID))
                                )
                                .where(CONSUMABLE_STOCK.APPARATUS_ID.eq(apparatusId))
                )
                .orderBy(COMPARTMENT.DISPLAY_ORDER, COMPARTMENT.ID)
                .fetch();

        // Group items by compartment
        var compartmentMap = new LinkedHashMap<com.example.firestock.domain.primitives.ids.CompartmentId, CompartmentBuilder>();

        for (Record record : itemsQuery) {
            var compartmentId = record.get(COMPARTMENT.ID);
            var builder = compartmentMap.computeIfAbsent(compartmentId, id -> new CompartmentBuilder(
                    id,
                    record.get(COMPARTMENT.CODE),
                    record.get(COMPARTMENT.NAME),
                    record.get(COMPARTMENT.LOCATION),
                    record.get(COMPARTMENT.DISPLAY_ORDER)
            ));

            var item = new AuditableItem(
                    record.get("equipment_item_id", com.example.firestock.domain.primitives.ids.EquipmentItemId.class),
                    record.get("consumable_stock_id", com.example.firestock.domain.primitives.ids.ConsumableStockId.class),
                    record.get("manifest_entry_id", com.example.firestock.domain.primitives.ids.ManifestEntryId.class),
                    record.get("item_name", String.class),
                    record.get("type_name", String.class),
                    record.get(EQUIPMENT_ITEM.SERIAL_NUMBER),
                    record.get("current_quantity", Quantity.class),
                    record.get("required_quantity", java.math.BigDecimal.class),
                    record.get("expiry_date", java.time.LocalDate.class),
                    record.get(EQUIPMENT_TYPE.REQUIRES_TESTING),
                    record.get("last_test_date", java.time.LocalDate.class),
                    record.get("next_test_due_date", java.time.LocalDate.class),
                    record.get("is_consumable", Boolean.class),
                    record.get("is_critical", Boolean.class),
                    record.get(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS),
                    record.get(FORMAL_AUDIT_ITEM.ITEM_CONDITION),
                    record.get(FORMAL_AUDIT_ITEM.TEST_RESULT)
            );

            builder.addItem(item);
        }

        // Also add empty compartments that have no items
        var emptyCompartments = create
                .selectFrom(COMPARTMENT)
                .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
                .and(COMPARTMENT.ID.notIn(compartmentMap.keySet().isEmpty()
                        ? List.of(com.example.firestock.domain.primitives.ids.CompartmentId.generate())
                        : compartmentMap.keySet()))
                .orderBy(COMPARTMENT.DISPLAY_ORDER)
                .fetch();

        for (var comp : emptyCompartments) {
            compartmentMap.put(comp.getId(), new CompartmentBuilder(
                    comp.getId(),
                    comp.getCode(),
                    comp.getName(),
                    comp.getLocation(),
                    comp.getDisplayOrder()
            ));
        }

        return compartmentMap.values().stream()
                .map(CompartmentBuilder::build)
                .sorted((a, b) -> Integer.compare(a.displayOrder(), b.displayOrder()))
                .toList();
    }

    /**
     * Gets apparatus list with last audit dates for a station.
     */
    List<ApparatusAuditInfo> findApparatusWithAuditInfoByStation(StationId stationId) {
        // Subquery for last completed audit date
        var lastAuditSubquery = DSL.select(DSL.max(FORMAL_AUDIT.COMPLETED_AT))
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(APPARATUS.ID))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.COMPLETED));

        // Check for active audit
        var activeAuditSubquery = DSL.select(FORMAL_AUDIT.ID)
                .from(FORMAL_AUDIT)
                .where(FORMAL_AUDIT.APPARATUS_ID.eq(APPARATUS.ID))
                .and(FORMAL_AUDIT.STATUS.eq(AuditStatus.IN_PROGRESS));

        return create
                .select(
                        APPARATUS.ID,
                        APPARATUS.UNIT_NUMBER,
                        APPARATUS.TYPE,
                        STATION.NAME.as("station_name"),
                        lastAuditSubquery.asField("last_audit_date"),
                        DSL.exists(activeAuditSubquery).as("has_active_audit")
                )
                .from(APPARATUS)
                .join(STATION).on(STATION.ID.eq(APPARATUS.STATION_ID))
                .where(APPARATUS.STATION_ID.eq(stationId))
                .and(APPARATUS.STATUS.eq(com.example.firestock.jooq.enums.ApparatusStatus.IN_SERVICE))
                .orderBy(APPARATUS.UNIT_NUMBER)
                .fetch()
                .map(r -> new ApparatusAuditInfo(
                        r.get(APPARATUS.ID),
                        r.get(APPARATUS.UNIT_NUMBER),
                        r.get(APPARATUS.TYPE),
                        r.get("station_name", String.class),
                        r.get("last_audit_date", LocalDateTime.class),
                        r.get("has_active_audit", Boolean.class)
                ));
    }

    /**
     * Helper class for building compartments with items.
     */
    private static class CompartmentBuilder {
        private final com.example.firestock.domain.primitives.ids.CompartmentId id;
        private final String code;
        private final String name;
        private final com.example.firestock.jooq.enums.CompartmentLocation location;
        private final int displayOrder;
        private final List<AuditableItem> items = new ArrayList<>();

        CompartmentBuilder(com.example.firestock.domain.primitives.ids.CompartmentId id,
                           String code, String name,
                           com.example.firestock.jooq.enums.CompartmentLocation location,
                           int displayOrder) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.location = location;
            this.displayOrder = displayOrder;
        }

        void addItem(AuditableItem item) {
            items.add(item);
        }

        CompartmentWithAuditableItems build() {
            return new CompartmentWithAuditableItems(id, code, name, location, displayOrder, List.copyOf(items));
        }
    }
}
