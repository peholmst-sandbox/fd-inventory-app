package com.example.firestock.inventorycheck.query;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.primitives.ids.*;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApparatusQueryTest {

    @Autowired
    private ApparatusQuery apparatusQuery;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private ApparatusId testApparatusId;
    private CompartmentId testCompartmentId;
    private EquipmentTypeId testEquipmentTypeId;
    private UserId testUserId;

    @BeforeEach
    void setUp() {
        // Clean up test data
        create.deleteFrom(INVENTORY_CHECK_ITEM).execute();
        create.deleteFrom(ISSUE).execute();
        create.deleteFrom(INVENTORY_CHECK).execute();
        create.deleteFrom(EQUIPMENT_ITEM).execute();
        create.deleteFrom(CONSUMABLE_STOCK).execute();
        create.deleteFrom(MANIFEST_ENTRY).execute();
        create.deleteFrom(COMPARTMENT).execute();
        create.deleteFrom(APPARATUS).execute();
        create.deleteFrom(USER_STATION_ASSIGNMENT).execute();
        create.deleteFrom(APP_USER).execute();
        create.deleteFrom(STATION).execute();
        create.deleteFrom(EQUIPMENT_TYPE).execute();

        // Create test station
        testStationId = StationId.generate();
        create.insertInto(STATION)
            .set(STATION.ID, testStationId)
            .set(STATION.CODE, new com.example.firestock.domain.primitives.strings.StationCode("ST01"))
            .set(STATION.NAME, "Test Station Alpha")
            .execute();

        // Create test user
        testUserId = UserId.generate();
        create.insertInto(APP_USER)
            .set(APP_USER.ID, testUserId)
            .set(APP_USER.BADGE_NUMBER, new com.example.firestock.domain.primitives.strings.BadgeNumber("B001"))
            .set(APP_USER.FIRST_NAME, "Test")
            .set(APP_USER.LAST_NAME, "User")
            .set(APP_USER.EMAIL, new com.example.firestock.domain.primitives.strings.EmailAddress("test@example.com"))
            .set(APP_USER.ROLE, UserRole.FIREFIGHTER)
            .execute();

        // Create test apparatus
        testApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, testApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 5"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, testStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        // Create test compartment
        testCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, testCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
            .set(COMPARTMENT.CODE, "L1")
            .set(COMPARTMENT.NAME, "Left Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.LEFT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        // Create test equipment type
        testEquipmentTypeId = EquipmentTypeId.generate();
        create.insertInto(EQUIPMENT_TYPE)
            .set(EQUIPMENT_TYPE.ID, testEquipmentTypeId)
            .set(EQUIPMENT_TYPE.CODE, "HALLIGAN")
            .set(EQUIPMENT_TYPE.NAME, "Halligan Bar")
            .set(EQUIPMENT_TYPE.CATEGORY, EquipmentCategory.TOOLS_HAND)
            .set(EQUIPMENT_TYPE.TRACKING_METHOD, TrackingMethod.SERIALIZED)
            .execute();
    }

    @Test
    void findByStationId_returnsApparatusWithLastCheckDate() {
        // Insert a completed inventory check
        var checkId = InventoryCheckId.generate();
        var completedAt = LocalDateTime.now().minusDays(1);
        create.insertInto(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.ID, checkId)
            .set(INVENTORY_CHECK.APPARATUS_ID, testApparatusId)
            .set(INVENTORY_CHECK.STATION_ID, testStationId)
            .set(INVENTORY_CHECK.PERFORMED_BY_ID, testUserId)
            .set(INVENTORY_CHECK.STATUS, CheckStatus.COMPLETED)
            .set(INVENTORY_CHECK.COMPLETED_AT, completedAt)
            .set(INVENTORY_CHECK.TOTAL_ITEMS, 0)
            .execute();

        var result = apparatusQuery.findByStationId(testStationId);

        assertEquals(1, result.size());
        var summary = result.getFirst();
        assertEquals(testApparatusId, summary.id());
        assertEquals("Engine 5", summary.unitNumber().value());
        assertEquals("Test Station Alpha", summary.stationName());
        assertNotNull(summary.lastCheckDate());
    }

    @Test
    void findByStationId_returnsNullLastCheckDate_whenNoCompletedChecks() {
        var result = apparatusQuery.findByStationId(testStationId);

        assertEquals(1, result.size());
        assertNull(result.getFirst().lastCheckDate());
    }

    @Test
    void findByStationId_returnsEmpty_forUnknownStation() {
        var result = apparatusQuery.findByStationId(StationId.generate());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdWithCompartmentsAndItems_returnsFullHierarchy() {
        // Add equipment item
        var equipmentId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, equipmentId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, testApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, testCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        var result = apparatusQuery.findByIdWithCompartmentsAndItems(testApparatusId);

        assertTrue(result.isPresent());
        var details = result.get();
        assertEquals(testApparatusId, details.id());
        assertEquals("Engine 5", details.unitNumber().value());
        assertEquals(testStationId, details.stationId());
        assertEquals("Test Station Alpha", details.stationName());

        assertEquals(1, details.compartments().size());
        var compartment = details.compartments().getFirst();
        assertEquals(testCompartmentId, compartment.id());
        assertEquals("L1", compartment.code());
        assertEquals("Left Compartment 1", compartment.name());

        assertEquals(1, compartment.items().size());
        var item = compartment.items().getFirst();
        assertEquals(equipmentId, item.equipmentItemId());
        assertFalse(item.isConsumable());
        assertEquals("Halligan Bar", item.name());
    }

    @Test
    void findByIdWithCompartmentsAndItems_includesConsumables() {
        // Add consumable stock
        var consumableId = ConsumableStockId.generate();
        create.insertInto(CONSUMABLE_STOCK)
            .set(CONSUMABLE_STOCK.ID, consumableId)
            .set(CONSUMABLE_STOCK.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(CONSUMABLE_STOCK.APPARATUS_ID, testApparatusId)
            .set(CONSUMABLE_STOCK.COMPARTMENT_ID, testCompartmentId)
            .set(CONSUMABLE_STOCK.QUANTITY, Quantity.of(5))
            .set(CONSUMABLE_STOCK.REQUIRED_QUANTITY, new BigDecimal("10"))
            .execute();

        var result = apparatusQuery.findByIdWithCompartmentsAndItems(testApparatusId);

        assertTrue(result.isPresent());
        var compartment = result.get().compartments().getFirst();
        assertEquals(1, compartment.items().size());

        var item = compartment.items().getFirst();
        assertEquals(consumableId, item.consumableStockId());
        assertTrue(item.isConsumable());
    }

    @Test
    void findByIdWithCompartmentsAndItems_returnsEmpty_forUnknownApparatus() {
        var result = apparatusQuery.findByIdWithCompartmentsAndItems(ApparatusId.generate());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdWithCompartmentsAndItems_ordersCompartmentsByDisplayOrder() {
        // Add second compartment with earlier display order
        var secondCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, secondCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
            .set(COMPARTMENT.CODE, "R1")
            .set(COMPARTMENT.NAME, "Right Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.RIGHT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 0) // Earlier than L1
            .execute();

        var result = apparatusQuery.findByIdWithCompartmentsAndItems(testApparatusId);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().compartments().size());
        assertEquals("R1", result.get().compartments().get(0).code()); // R1 first (display_order=0)
        assertEquals("L1", result.get().compartments().get(1).code()); // L1 second (display_order=1)
    }

    @Test
    void totalItemCount_sumsItemsAcrossCompartments() {
        // Add items to first compartment
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, EquipmentItemId.generate())
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, testApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, testCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, EquipmentItemId.generate())
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, testApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, testCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        // Add second compartment with items
        var secondCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, secondCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
            .set(COMPARTMENT.CODE, "R1")
            .set(COMPARTMENT.NAME, "Right Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.RIGHT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 2)
            .execute();

        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, EquipmentItemId.generate())
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, testApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, secondCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        var result = apparatusQuery.findByIdWithCompartmentsAndItems(testApparatusId);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().totalItemCount());
    }
}
