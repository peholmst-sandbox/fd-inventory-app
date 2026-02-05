package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.CheckStatus;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.*;

/**
 * Query class for apparatus-related data access operations.
 */
@Component
class ApparatusQuery {

    private final DSLContext create;

    ApparatusQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds all apparatus assigned to a station with their last completed check date.
     *
     * @param stationId the station to find apparatus for
     * @return list of apparatus summaries
     */
    List<ApparatusSummary> findByStationId(StationId stationId) {
        // Subquery to get the latest completed check date for each apparatus
        var latestCheck = DSL.select(
                INVENTORY_CHECK.APPARATUS_ID,
                DSL.max(INVENTORY_CHECK.COMPLETED_AT).as("last_check_date"))
            .from(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
            .groupBy(INVENTORY_CHECK.APPARATUS_ID)
            .asTable("latest_check");

        return create.select(
                APPARATUS.ID,
                APPARATUS.UNIT_NUMBER,
                STATION.NAME,
                latestCheck.field("last_check_date", LocalDateTime.class))
            .from(APPARATUS)
            .join(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .leftJoin(latestCheck).on(APPARATUS.ID.eq(latestCheck.field(INVENTORY_CHECK.APPARATUS_ID)))
            .where(APPARATUS.STATION_ID.eq(stationId))
            .orderBy(APPARATUS.UNIT_NUMBER)
            .fetch(r -> new ApparatusSummary(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4()
            ));
    }

    /**
     * Finds apparatus details including compartments and all items.
     *
     * @param apparatusId the apparatus to retrieve
     * @return the apparatus details or empty if not found
     */
    Optional<ApparatusDetails> findByIdWithCompartmentsAndItems(ApparatusId apparatusId) {
        // First, get the apparatus basic info
        var apparatusInfo = create.select(
                APPARATUS.ID,
                APPARATUS.UNIT_NUMBER,
                APPARATUS.STATION_ID,
                STATION.NAME)
            .from(APPARATUS)
            .join(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .where(APPARATUS.ID.eq(apparatusId))
            .fetchOptional();

        if (apparatusInfo.isEmpty()) {
            return Optional.empty();
        }

        Record4<ApparatusId, com.example.firestock.domain.primitives.strings.UnitNumber, StationId, String> info = apparatusInfo.get();

        // Get compartments
        var compartments = create.select(
                COMPARTMENT.ID,
                COMPARTMENT.CODE,
                COMPARTMENT.NAME,
                COMPARTMENT.DISPLAY_ORDER)
            .from(COMPARTMENT)
            .where(COMPARTMENT.APPARATUS_ID.eq(apparatusId))
            .orderBy(COMPARTMENT.DISPLAY_ORDER, COMPARTMENT.CODE)
            .fetchInto(CompartmentRecord.class);

        // Build compartment list with items
        List<CompartmentWithItems> compartmentList = new ArrayList<>();

        for (var comp : compartments) {
            List<CheckableItem> items = new ArrayList<>();

            // Get equipment items in this compartment
            var equipmentItems = create.select(
                    EQUIPMENT_ITEM.ID,
                    EQUIPMENT_TYPE.NAME,
                    EQUIPMENT_TYPE.NAME.as("type_name"),
                    EQUIPMENT_ITEM.SERIAL_NUMBER,
                    EQUIPMENT_ITEM.BARCODE)
                .from(EQUIPMENT_ITEM)
                .join(EQUIPMENT_TYPE).on(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
                .where(EQUIPMENT_ITEM.COMPARTMENT_ID.eq(comp.id()))
                .and(EQUIPMENT_ITEM.APPARATUS_ID.eq(apparatusId))
                .fetch();

            for (var eq : equipmentItems) {
                items.add(CheckableItem.forEquipment(
                    eq.value1(),
                    eq.value2(),
                    eq.value3(),
                    eq.value4(),
                    eq.value5()
                ));
            }

            // Get consumable stock in this compartment
            var consumables = create.select(
                    CONSUMABLE_STOCK.ID,
                    EQUIPMENT_TYPE.NAME,
                    EQUIPMENT_TYPE.NAME.as("type_name"),
                    CONSUMABLE_STOCK.REQUIRED_QUANTITY,
                    CONSUMABLE_STOCK.QUANTITY,
                    CONSUMABLE_STOCK.EXPIRY_DATE)
                .from(CONSUMABLE_STOCK)
                .join(EQUIPMENT_TYPE).on(CONSUMABLE_STOCK.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
                .where(CONSUMABLE_STOCK.COMPARTMENT_ID.eq(comp.id()))
                .and(CONSUMABLE_STOCK.APPARATUS_ID.eq(apparatusId))
                .fetch();

            for (var cs : consumables) {
                items.add(CheckableItem.forConsumable(
                    cs.value1(),
                    cs.value2(),
                    cs.value3(),
                    cs.value4(),
                    cs.value5(),
                    cs.value6()
                ));
            }

            compartmentList.add(new CompartmentWithItems(
                comp.id(),
                comp.code(),
                comp.name(),
                comp.displayOrder(),
                items
            ));
        }

        return Optional.of(new ApparatusDetails(
            info.value1(),
            info.value2(),
            info.value3(),
            info.value4(),
            compartmentList
        ));
    }

    /**
     * Finds apparatus with active check info for a station.
     * Returns apparatus data with check status (but not user names - those are resolved separately).
     *
     * @param stationId the station to find apparatus for
     * @return list of apparatus with their active check records
     */
    List<ApparatusWithActiveCheckRecord> findByStationIdWithActiveChecks(StationId stationId) {
        // Subquery to get the latest completed check date for each apparatus
        var latestCompletedCheck = DSL.select(
                INVENTORY_CHECK.APPARATUS_ID,
                DSL.max(INVENTORY_CHECK.COMPLETED_AT).as("last_check_date"))
            .from(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
            .groupBy(INVENTORY_CHECK.APPARATUS_ID)
            .asTable("latest_completed_check");

        // Subquery to get active check info for each apparatus
        var activeCheck = DSL.select(
                INVENTORY_CHECK.APPARATUS_ID.as("active_apparatus_id"),
                INVENTORY_CHECK.ID.as("active_check_id"),
                INVENTORY_CHECK.STARTED_AT.as("active_started_at"))
            .from(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
            .asTable("active_check");

        return create.select(
                APPARATUS.ID,
                APPARATUS.UNIT_NUMBER,
                STATION.NAME,
                latestCompletedCheck.field("last_check_date", LocalDateTime.class),
                activeCheck.field("active_check_id", InventoryCheckId.class),
                activeCheck.field("active_started_at", Instant.class))
            .from(APPARATUS)
            .join(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .leftJoin(latestCompletedCheck).on(APPARATUS.ID.eq(latestCompletedCheck.field(INVENTORY_CHECK.APPARATUS_ID)))
            .leftJoin(activeCheck).on(APPARATUS.ID.eq(activeCheck.field("active_apparatus_id", ApparatusId.class)))
            .where(APPARATUS.STATION_ID.eq(stationId))
            .orderBy(APPARATUS.UNIT_NUMBER)
            .fetch(r -> new ApparatusWithActiveCheckRecord(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6()
            ));
    }

    /**
     * Simple record for compartment data.
     */
    private record CompartmentRecord(
        com.example.firestock.domain.primitives.ids.CompartmentId id,
        String code,
        String name,
        int displayOrder
    ) {}

    /**
     * Record for apparatus with active check status.
     * User names are resolved separately by the service layer.
     */
    record ApparatusWithActiveCheckRecord(
        ApparatusId id,
        UnitNumber unitNumber,
        String stationName,
        LocalDateTime lastCheckDate,
        InventoryCheckId activeCheckId,
        Instant activeCheckStartedAt
    ) {
        boolean hasActiveCheck() {
            return activeCheckId != null;
        }
    }
}
